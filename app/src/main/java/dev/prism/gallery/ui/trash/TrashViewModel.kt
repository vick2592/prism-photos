package dev.prism.gallery.ui.trash

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.prism.gallery.data.local.MediaStoreRepository
import dev.prism.gallery.data.local.dao.TrashDao
import dev.prism.gallery.data.local.entity.TrashEntity
import dev.prism.gallery.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class TrashedItem(
    val entity: TrashEntity,
    val mediaItem: MediaItem?,
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trashDao: TrashDao,
    private val repository: MediaStoreRepository,
) : ViewModel() {

    val trashedItems: StateFlow<List<TrashedItem>> = combine(
        trashDao.observeTrash(),
        repository.observeMedia(),
    ) { trashed, media ->
        val mediaMap = media.associateBy { it.id }
        trashed.map { entity -> TrashedItem(entity, mediaMap[entity.mediaId]) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restore(mediaId: Long) {
        viewModelScope.launch { trashDao.removeFromTrash(mediaId) }
    }

    /** Returns an IntentSenderRequest for system delete dialog on API 30+,
     *  or null if deletion was handled inline (API <30). */
    suspend fun buildDeleteRequest(item: TrashedItem): IntentSenderRequest? =
        withContext(Dispatchers.IO) {
            val uri = item.mediaItem?.uri ?: Uri.parse(item.entity.originalUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pi = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                IntentSenderRequest.Builder(pi.intentSender).build()
            } else {
                try { context.contentResolver.delete(uri, null, null) } catch (_: Exception) { }
                trashDao.removeFromTrash(item.entity.mediaId)
                null
            }
        }

    fun removeFromTrashDb(mediaId: Long) {
        viewModelScope.launch { trashDao.removeFromTrash(mediaId) }
    }
}
