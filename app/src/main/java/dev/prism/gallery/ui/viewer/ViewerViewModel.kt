package dev.prism.gallery.ui.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.prism.gallery.data.local.MediaStoreRepository
import dev.prism.gallery.data.local.dao.FavoriteDao
import dev.prism.gallery.data.local.entity.FavoriteEntity
import dev.prism.gallery.data.model.MediaItem
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
) : ViewModel() {

    private val _state = MutableStateFlow(ViewerState())
    val state: StateFlow<ViewerState> = _state.asStateFlow()

    fun load(mediaId: Long) {
        viewModelScope.launch {
            combine(
                repository.observeMedia(),
                favoriteDao.observeFavorites(),
            ) { items, favorites ->
                val index = items.indexOfFirst { it.id == mediaId }.coerceAtLeast(0)
                ViewerState(
                    items = items,
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
}
