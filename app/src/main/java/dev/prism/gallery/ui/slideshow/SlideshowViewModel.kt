package dev.prism.gallery.ui.slideshow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.prism.gallery.data.local.dao.TrashDao
import dev.prism.gallery.data.local.MediaStoreRepository
import dev.prism.gallery.data.model.MediaItem
import dev.prism.gallery.data.preferences.PreferencesRepository
import dev.prism.gallery.ui.gallery.isInDcim
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SlideshowViewModel @Inject constructor(
    repository: MediaStoreRepository,
    trashDao: TrashDao,
    prefs: PreferencesRepository,
) : ViewModel() {

    // Images only (no videos), filtered by trash, same source filter as gallery grid
    val images: StateFlow<List<MediaItem>> = combine(
        repository.observeMedia(),
        trashDao.observeTrash(),
        prefs.extraGalleryBucketIds,
    ) { all, trashed, extraBuckets ->
        val trashedIds = trashed.map { it.mediaId }.toSet()
        all.filter { item ->
            !item.isVideo &&
                item.id !in trashedIds &&
                (isInDcim(item.relativePath) || item.bucketId in extraBuckets)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val intervalSecs: StateFlow<Int> = prefs.slideshowInterval
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferencesRepository.DEFAULT_SLIDESHOW_INTERVAL)
}
