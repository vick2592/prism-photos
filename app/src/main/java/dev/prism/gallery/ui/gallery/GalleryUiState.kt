package dev.prism.gallery.ui.gallery

import dev.prism.gallery.data.model.MediaItem

sealed interface GalleryUiState {
    data object Loading : GalleryUiState
    data object PermissionRequired : GalleryUiState
    data class Success(val mediaGroups: Map<String, List<MediaItem>>) : GalleryUiState
    data class Error(val message: String) : GalleryUiState
}
