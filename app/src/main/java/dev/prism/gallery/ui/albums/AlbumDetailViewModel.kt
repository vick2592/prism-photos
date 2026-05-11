package dev.prism.gallery.ui.albums

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.prism.gallery.data.local.MediaStoreRepository
import dev.prism.gallery.data.model.MediaItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: MediaStoreRepository,
) : ViewModel() {

    private val bucketId: String = checkNotNull(savedStateHandle["bucketId"])

    val items: StateFlow<List<MediaItem>> = repository.observeMedia()
        .map { all -> all.filter { it.bucketId == bucketId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
