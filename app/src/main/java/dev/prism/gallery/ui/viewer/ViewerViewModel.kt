package dev.prism.gallery.ui.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.prism.gallery.data.local.MediaStoreRepository
import dev.prism.gallery.data.local.dao.FavoriteDao
import dev.prism.gallery.data.local.dao.TrashDao
import dev.prism.gallery.data.local.entity.FavoriteEntity
import dev.prism.gallery.data.model.MediaItem
import dev.prism.gallery.domain.usecase.TrashMediaUseCase
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
    private val repository: MediaStoreRepository,
    private val favoriteDao: FavoriteDao,
    private val trashDao: TrashDao,
    private val trashMediaUseCase: TrashMediaUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ViewerState())
    val state: StateFlow<ViewerState> = _state.asStateFlow()

    fun load(mediaId: Long) {
        viewModelScope.launch {
            combine(
                repository.observeMedia(),
                favoriteDao.observeFavorites(),
                trashDao.observeTrash(),
            ) { items, favorites, trashed ->
                val trashedIds = trashed.map { it.mediaId }.toSet()
                val filtered = items.filter { it.id !in trashedIds }
                val index = filtered.indexOfFirst { it.id == mediaId }.coerceAtLeast(0)
                ViewerState(
                    items = filtered,
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
}
