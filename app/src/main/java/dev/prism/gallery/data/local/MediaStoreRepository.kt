package dev.prism.gallery.data.local

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.prism.gallery.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // Allows any component to force an immediate re-query independent of the ContentObserver.
    private val _manualRefresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun refresh() {
        _manualRefresh.tryEmit(Unit)
    }

    /**
     * Returns a [Flow] that emits the full media list immediately, then re-emits
     * whenever the Camera (or any app) saves a new photo or video to MediaStore,
     * or when [refresh] is called explicitly.
     */
    fun observeMedia(): Flow<List<MediaItem>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                launch { send(queryAllMedia()) }
            }
        }
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer
        )
        context.contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer
        )
        // Manual refresh trigger (e.g. after UCrop save)
        launch {
            _manualRefresh.collect { send(queryAllMedia()) }
        }
        // Emit initial value immediately
        send(queryAllMedia())
        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    private fun queryAllMedia(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        items += queryUri(
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            isVideo = false,
        )
        items += queryUri(
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            isVideo = true,
        )
        return items.sortedByDescending { it.dateTaken }
    }

    private fun queryUri(uri: Uri, isVideo: Boolean): List<MediaItem> {
        val items = mutableListOf<MediaItem>()

        val baseProjection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            // RELATIVE_PATH (API 29+) gives "DCIM/Camera/" reliably.
            // DATA (API 26-28) gives the absolute path as fallback.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.MediaColumns.RELATIVE_PATH
            else
                MediaStore.MediaColumns.DATA,
        )
        val projection = if (isVideo) {
            baseProjection + arrayOf(MediaStore.Video.VideoColumns.DURATION)
        } else {
            baseProjection
        }

        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_TAKEN} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val durationCol = if (isVideo) cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION) else -1
            val relativePathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            else -1
            val dataCol = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            else -1
            val extRoot = Environment.getExternalStorageDirectory().absolutePath

            while (cursor.moveToNext()) {
                val mimeType = cursor.getString(mimeCol) ?: continue
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(uri, id)
                val duration = if (isVideo && durationCol != -1) cursor.getLong(durationCol) else 0L

                // Derive relativePath: "DCIM/Camera/" style, always ends with "/"
                val relativePath = when {
                    relativePathCol >= 0 -> cursor.getString(relativePathCol) ?: ""
                    dataCol >= 0 -> {
                        val absPath = cursor.getString(dataCol) ?: ""
                        val rel = absPath.removePrefix(extRoot).trimStart('/')
                        val dir = rel.substringBeforeLast('/')
                        if (dir.isEmpty()) "" else "$dir/"
                    }
                    else -> ""
                }

                items += MediaItem(
                    id = id,
                    uri = contentUri,
                    displayName = cursor.getString(nameCol) ?: "",
                    dateTaken = cursor.getLong(dateTakenCol),
                    dateModified = cursor.getLong(dateModifiedCol),
                    mimeType = mimeType,
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    size = cursor.getLong(sizeCol),
                    bucketId = cursor.getString(bucketIdCol) ?: "",
                    bucketName = cursor.getString(bucketNameCol) ?: "Unknown",
                    relativePath = relativePath,
                    duration = duration,
                )
            }
        }
        return items
    }

    /**
     * Permanently removes [uri] from MediaStore (and the underlying file).
     * With MANAGE_MEDIA permission on API 31+, no user consent dialog is shown.
     * Returns true if at least one row was deleted.
     */
    suspend fun deleteItemPermanently(uri: Uri): Boolean =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.delete(uri, null, null) > 0 }
                .getOrDefault(false)
        }
}
