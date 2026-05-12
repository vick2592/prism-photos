package dev.prism.gallery.ui.viewer

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.yalantis.ucrop.UCrop
import java.io.File

object EditHelper {

    /**
     * Builds a UCrop Intent from a MediaStore source URI.
     * The destination is a temporary file in the app's cache directory.
     */
    fun buildCropIntent(context: Context, sourceUri: Uri, displayName: String): Intent {
        val ext = displayName.substringAfterLast('.', "jpg").lowercase()
        val destFile = File(context.cacheDir, "prism_edit_${System.currentTimeMillis()}.$ext")

        val options = UCrop.Options().apply {
            setCompressionQuality(95)
            setFreeStyleCropEnabled(true)
            setShowCropGrid(true)
            setShowCropFrame(true)
            // Dark toolbar to match the black viewer background
            setToolbarColor(0xFF000000.toInt())
            setStatusBarColor(0xFF000000.toInt())
            setToolbarWidgetColor(0xFFFFFFFF.toInt())
            setActiveControlsWidgetColor(0xFF9C27B0.toInt())
            setToolbarTitle("Edit")
        }

        return UCrop.of(sourceUri, Uri.fromFile(destFile))
            .withOptions(options)
            .getIntent(context)
    }

    /**
     * Queries MediaStore for the RELATIVE_PATH of [originalUri].
     * Falls back to "DCIM/" if the column is unavailable.
     */
    private fun queryRelativePath(context: Context, originalUri: Uri): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val projection = arrayOf(MediaStore.Images.Media.RELATIVE_PATH)
            context.contentResolver.query(originalUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val col = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                    if (col >= 0) return cursor.getString(col)
                }
            }
        }
        // API 26-28: derive from DATA path
        val dataProjection = arrayOf(MediaStore.Images.Media.DATA)
        context.contentResolver.query(originalUri, dataProjection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val col = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                if (col >= 0) {
                    val absolutePath = cursor.getString(col) ?: return@use
                    // Convert absolute path to relative: strip the external storage root
                    val extStorage = android.os.Environment.getExternalStorageDirectory().absolutePath
                    val relative = absolutePath.removePrefix(extStorage).trimStart('/')
                    return relative.substringBeforeLast('/') + "/"
                }
            }
        }
        return "DCIM/"
    }

    /**
     * Checks if a file with [displayName] already exists in [relativePath] in MediaStore.
     */
    private fun nameExistsInMediaStore(
        context: Context,
        relativePath: String,
        displayName: String,
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND " +
                "${MediaStore.Images.Media.RELATIVE_PATH} = ?"
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
                selection,
                arrayOf(displayName, relativePath),
                null,
            )?.use { return it.count > 0 }
        }
        return false
    }

    /**
     * Returns a unique display name in [relativePath]:
     * "photo.jpg" → "photo_2.jpg" → "photo_3.jpg" → …
     */
    private fun uniqueName(
        context: Context,
        relativePath: String,
        baseName: String,
        ext: String,
    ): String {
        var counter = 2
        var candidate = "${baseName}_2.$ext"
        while (nameExistsInMediaStore(context, relativePath, candidate)) {
            counter++
            candidate = "${baseName}_$counter.$ext"
        }
        return candidate
    }

    /**
     * Copies the UCrop output file into MediaStore, in the same directory as the
     * original, named "<originalBaseName>_2.jpg" (incrementing until unique).
     * Returns the new MediaStore URI on success, null on failure.
     */
    suspend fun saveToMediaStore(
        context: Context,
        editedUri: Uri,
        originalDisplayName: String,
        originalUri: Uri,
    ): Uri? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val ext = originalDisplayName.substringAfterLast('.', "jpg").lowercase()
            val baseName = originalDisplayName.substringBeforeLast('.')
            val mimeType = when (ext) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }

            val relativePath = queryRelativePath(context, originalUri)
            val newName = uniqueName(context, relativePath, baseName, ext)

            val now = System.currentTimeMillis()
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, newName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.DATE_TAKEN, now)
                put(MediaStore.Images.Media.DATE_ADDED, now / 1000)
                put(MediaStore.Images.Media.DATE_MODIFIED, now / 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val outputUri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return@withContext null

            context.contentResolver.openInputStream(editedUri)?.use { input ->
                context.contentResolver.openOutputStream(outputUri)?.use { output ->
                    input.copyTo(output)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                values.put(MediaStore.Images.Media.DATE_TAKEN, now)
                context.contentResolver.update(outputUri, values, null, null)
            }

            outputUri
        } catch (e: Exception) {
            null
        }
    }

    /** Deletes the temporary cache file produced by UCrop. */
    fun cleanupCacheFile(uri: Uri) {
        uri.path?.let { File(it).delete() }
    }
}
