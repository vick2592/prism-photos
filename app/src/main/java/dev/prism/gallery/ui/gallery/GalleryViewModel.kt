package dev.prism.gallery.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.prism.gallery.data.local.MediaStoreRepository
import dev.prism.gallery.data.local.dao.TrashDao
import dev.prism.gallery.data.model.MediaItem
import dev.prism.gallery.data.preferences.PreferencesRepository
import dev.prism.gallery.domain.usecase.GetGalleryUseCase
import dev.prism.gallery.domain.usecase.TrashMediaUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/** Returns true if [relativePath] is under the device's DCIM tree (camera roll). */
internal fun isInDcim(relativePath: String): Boolean =
    relativePath.startsWith("DCIM/", ignoreCase = true)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val getGalleryUseCase: GetGalleryUseCase,
    private val trashDao: TrashDao,
    private val prefs: PreferencesRepository,
    private val trashMediaUseCase: TrashMediaUseCase,
    private val repository: MediaStoreRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GalleryUiState>(GalleryUiState.Loading)
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    // ---------- Selection ----------
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    fun toggleSelection(id: Long) {
        _selectedIds.value = if (id in _selectedIds.value) _selectedIds.value - id else _selectedIds.value + id
    }

    fun clearSelection() { _selectedIds.value = emptySet() }

    fun trashSelected(allItems: List<MediaItem>) {
        val ids = _selectedIds.value
        viewModelScope.launch {
            allItems.filter { it.id in ids }.forEach { trashMediaUseCase(it) }
            clearSelection()
        }
    }

    fun deleteSelectedPermanently(allItems: List<MediaItem>) {
        val ids = _selectedIds.value
        viewModelScope.launch {
            allItems.filter { it.id in ids }.forEach { repository.deleteItemPermanently(it.uri) }
            clearSelection()
        }
    }
    // ----------------------------------

    val gridColumns: StateFlow<Int> = prefs.gridColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferencesRepository.DEFAULT_GRID_COLUMNS)

    private val zone = ZoneId.systemDefault()
    private val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    private var mediaJob: Job? = null

    /** Groups a timestamp into a Google-Photos-style date label. */
    private fun formatDateLabel(ms: Long): String {
        val date = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        return when {
            date == today -> "Today"
            date == today.minusDays(1) -> "Yesterday"
            date.year == today.year -> date.format(dayOfWeekFormatter)   // "Tue, 6 May"
            else -> date.format(monthYearFormatter)                        // "March 2024"
        }
    }

    fun loadMedia() {
        mediaJob?.cancel()
        mediaJob = viewModelScope.launch {
            combine(
                getGalleryUseCase(),
                trashDao.observeTrash(),
                prefs.extraGalleryBucketIds,
            ) { items, trashed, extraBuckets ->
                val trashedIds = trashed.map { it.mediaId }.toSet()
                items
                    .filter { it.id !in trashedIds }
                    .filter { isInDcim(it.relativePath) || it.bucketId in extraBuckets }
            }
                .catch { e -> _uiState.value = GalleryUiState.Error(e.message ?: "Unknown error") }
                .collect { items ->
                    val sorted = items.sortedByDescending {
                        if (it.dateTaken > 0) it.dateTaken else it.dateModified * 1000L
                    }
                    val grouped = LinkedHashMap<String, MutableList<dev.prism.gallery.data.model.MediaItem>>()
                    sorted.forEach { item ->
                        val effectiveMs = if (item.dateTaken > 0) item.dateTaken
                                          else item.dateModified * 1000L
                        val label = if (effectiveMs > 0) formatDateLabel(effectiveMs) else "Unknown Date"
                        grouped.getOrPut(label) { mutableListOf() }.add(item)
                    }
                    _uiState.value = GalleryUiState.Success(grouped)
                }
        }
    }
}
