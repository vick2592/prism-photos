package dev.prism.gallery.domain.usecase

import dev.prism.gallery.data.local.MediaStoreRepository
import dev.prism.gallery.data.model.MediaItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGalleryUseCase @Inject constructor(
    private val repository: MediaStoreRepository,
) {
    operator fun invoke(): Flow<List<MediaItem>> = repository.observeMedia()
}
