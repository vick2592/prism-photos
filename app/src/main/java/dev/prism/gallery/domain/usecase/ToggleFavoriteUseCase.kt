package dev.prism.gallery.domain.usecase

import dev.prism.gallery.data.local.dao.FavoriteDao
import dev.prism.gallery.data.local.entity.FavoriteEntity
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val favoriteDao: FavoriteDao,
) {
    suspend operator fun invoke(mediaId: Long) {
        if (favoriteDao.isFavorite(mediaId)) {
            favoriteDao.removeFavorite(mediaId)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(mediaId = mediaId))
        }
    }
}
