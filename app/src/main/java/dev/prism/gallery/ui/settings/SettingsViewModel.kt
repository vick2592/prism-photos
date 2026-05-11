package dev.prism.gallery.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.prism.gallery.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesRepository,
) : ViewModel() {

    val gridColumns: StateFlow<Int> = prefs.gridColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferencesRepository.DEFAULT_GRID_COLUMNS)

    val slideshowInterval: StateFlow<Int> = prefs.slideshowInterval
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferencesRepository.DEFAULT_SLIDESHOW_INTERVAL)

    fun setGridColumns(columns: Int) {
        viewModelScope.launch { prefs.setGridColumns(columns) }
    }

    fun setSlideshowInterval(secs: Int) {
        viewModelScope.launch { prefs.setSlideshowInterval(secs) }
    }
}
