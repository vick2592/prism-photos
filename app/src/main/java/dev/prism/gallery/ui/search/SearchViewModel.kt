package dev.prism.gallery.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.prism.gallery.data.model.MediaItem
import dev.prism.gallery.domain.usecase.SearchMediaUseCase
import dev.prism.gallery.domain.usecase.SearchQuery
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMediaUseCase: SearchMediaUseCase,
) : ViewModel() {

    private val _results = MutableStateFlow<List<MediaItem>>(emptyList())
    val results: StateFlow<List<MediaItem>> = _results.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: SearchQuery) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            searchMediaUseCase(query)
                .catch { }
                .collect { _results.value = it }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _results.value = emptyList()
    }
}
