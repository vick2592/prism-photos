package dev.prism.gallery.domain.usecase

import dev.prism.gallery.data.local.MediaStoreRepository
import dev.prism.gallery.domain.model.Album
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetAlbumsUseCase @Inject constructor(
    private val repository: MediaStoreRepository,
) {
    operator fun invoke(): Flow<List<Album>> = repository.observeMedia().map { items ->
        items
            .groupBy { it.bucketId }
            .map { (bucketId, mediaItems) ->
                val sorted = mediaItems.sortedByDescending { it.dateTaken }
                Album(
                    id = bucketId,
                    name = sorted.first().bucketName,
                    coverUri = sorted.first().uri,
                    count = mediaItems.size,
                    dateModified = sorted.first().dateTaken,
                )
            }
            .sortedByDescending { it.dateModified }
    }
}
