package dev.prism.gallery.domain.usecase

import dev.prism.gallery.data.local.dao.TrashDao
import dev.prism.gallery.data.local.entity.TrashEntity
import dev.prism.gallery.data.model.MediaItem
import javax.inject.Inject

class TrashMediaUseCase @Inject constructor(
    private val trashDao: TrashDao,
) {
    suspend operator fun invoke(item: MediaItem) {
        trashDao.addToTrash(
            TrashEntity(
                mediaId = item.id,
                originalUri = item.uri.toString(),
                displayName = item.displayName,
            )
        )
    }

    suspend fun restore(mediaId: Long) {
        trashDao.removeFromTrash(mediaId)
    }
}
