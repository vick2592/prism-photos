package dev.prism.gallery.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.prism.gallery.data.preferences.PreferencesRepository
import dev.prism.gallery.domain.model.Album
import dev.prism.gallery.domain.usecase.GetAlbumsUseCase
import dev.prism.gallery.ui.gallery.isInDcim
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesRepository,
    getAlbumsUseCase: GetAlbumsUseCase,
) : ViewModel() {

    val gridColumns: StateFlow<Int> = prefs.gridColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferencesRepository.DEFAULT_GRID_COLUMNS)

    val slideshowInterval: StateFlow<Int> = prefs.slideshowInterval
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferencesRepository.DEFAULT_SLIDESHOW_INTERVAL)

    /** Albums NOT under DCIM — these can be opted into the main gallery view. */
    val nonCameraAlbums: StateFlow<List<Album>> = getAlbumsUseCase()
        .map { albums -> albums.filter { !isInDcim(it.relativePath) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val extraGalleryBucketIds: StateFlow<Set<String>> = prefs.extraGalleryBucketIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun setGridColumns(columns: Int) {
        viewModelScope.launch { prefs.setGridColumns(columns) }
    }

    fun setSlideshowInterval(secs: Int) {
        viewModelScope.launch { prefs.setSlideshowInterval(secs) }
    }

    fun toggleGalleryBucket(bucketId: String) {
        viewModelScope.launch {
            val current = extraGalleryBucketIds.value.toMutableSet()
            if (bucketId in current) current.remove(bucketId) else current.add(bucketId)
            prefs.setExtraGalleryBucketIds(current)
        }
    }
}
