package dev.prism.gallery.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.prism.gallery.domain.usecase.GetGalleryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val getGalleryUseCase: GetGalleryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    fun loadMedia() {
        viewModelScope.launch {
            getGalleryUseCase()
                .catch { e -> _uiState.value = GalleryUiState.Error(e.message ?: "Unknown error") }
                .collect { items ->
                    val grouped = LinkedHashMap<String, MutableList<dev.prism.gallery.data.model.MediaItem>>()
                    items.forEach { item ->
                        val label = if (item.dateTaken > 0) {
                            Instant.ofEpochMilli(item.dateTaken)
                                .atZone(ZoneId.systemDefault())
                                .format(monthYearFormatter)
                        } else {
                            "Unknown Date"
                        }
                        grouped.getOrPut(label) { mutableListOf() }.add(item)
                    }
                    _uiState.value = GalleryUiState.Success(grouped)
                }
        }
    }
}
