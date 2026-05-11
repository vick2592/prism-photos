package dev.prism.gallery.domain.usecase

import dev.prism.gallery.data.local.MediaStoreRepository
import dev.prism.gallery.data.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class SearchQuery(
    val startDate: Long? = null,
    val endDate: Long? = null,
    val locationQuery: String? = null,
)

class SearchMediaUseCase @Inject constructor(
    private val repository: MediaStoreRepository,
) {
    operator fun invoke(query: SearchQuery): Flow<List<MediaItem>> =
        repository.observeMedia().map { items ->
            items.filter { item ->
                val afterStart = query.startDate == null || item.dateTaken >= query.startDate
                val beforeEnd = query.endDate == null || item.dateTaken <= query.endDate
                afterStart && beforeEnd
            }
        }
}
