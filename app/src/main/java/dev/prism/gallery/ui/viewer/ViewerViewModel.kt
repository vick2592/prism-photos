package dev.prism.gallery.ui.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.prism.gallery.data.local.MediaStoreRepository
import dev.prism.gallery.data.local.dao.FavoriteDao
import dev.prism.gallery.data.local.dao.TrashDao
import dev.prism.gallery.data.local.entity.FavoriteEntity
import dev.prism.gallery.data.model.MediaItem
import dev.prism.gallery.data.preferences.PreferencesRepository
import dev.prism.gallery.domain.usecase.TrashMediaUseCase
import dev.prism.gallery.ui.gallery.isInDcim
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ViewerState(
    val items: List<MediaItem> = emptyList(),
    val startIndex: Int = 0,
    val favoriteIds: Set<Long> = emptySet(),
)

@HiltViewModel
class ViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MediaStoreRepository,
    private val favoriteDao: FavoriteDao,
    private val trashDao: TrashDao,
    private val trashMediaUseCase: TrashMediaUseCase,
    private val prefs: PreferencesRepository,
) : ViewModel() {

    /**
     * Filter context passed from the screen that launched the viewer:
     *  - "gallery"      -> only DCIM/ + user-selected extra buckets (matches gallery grid)
     *  - "bucket:{id}"  -> only that specific album bucket
     *  - "all"          -> no filter beyond trash exclusion (search, favorites)
     */
    private val filter: String = savedStateHandle.get<String>("filter") ?: "all"

    private val _state = MutableStateFlow(ViewerState())
    val state: StateFlow<ViewerState> = _state.asStateFlow()

    fun load(mediaId: Long) {
        viewModelScope.launch {
            combine(
                repository.observeMedia(),
                favoriteDao.observeFavorites(),
                trashDao.observeTrash(),
                prefs.extraGalleryBucketIds,
            ) { items, favorites, trashed, extraBuckets ->
                val trashedIds = trashed.map { it.mediaId }.toSet()
                val withoutTrash = items.filter { it.id !in trashedIds }

                // Apply the same filter that was active when the user tapped the photo,
                // so swiping in the viewer stays within the same media context.
                val contextItems = when {
                    filter == "gallery" -> withoutTrash.filter {
                        isInDcim(it.relativePath) || it.bucketId in extraBuckets
                    }
                    filter.startsWith("bucket:") -> {
                        val bucketId = filter.removePrefix("bucket:")
                        withoutTrash.filter { it.bucketId == bucketId }
                    }
                    else -> withoutTrash
                }

                val index = contextItems.indexOfFirst { it.id == mediaId }.coerceAtLeast(0)
                ViewerState(
                    items = contextItems,
                    startIndex = index,
                    favoriteIds = favorites.map { it.mediaId }.toSet(),
                )
            }.collect { _state.value = it }
        }
    }

    fun toggleFavorite(mediaId: Long) {
        viewModelScope.launch {
            if (favoriteDao.isFavorite(mediaId)) {
                favoriteDao.removeFavorite(mediaId)
            } else {
                favoriteDao.addFavorite(FavoriteEntity(mediaId = mediaId))
            }
        }
    }

    fun trashItem(item: MediaItem) {
        viewModelScope.launch { trashMediaUseCase(item) }
    }

    fun refreshMedia() {
        repository.refresh()
    }
}
