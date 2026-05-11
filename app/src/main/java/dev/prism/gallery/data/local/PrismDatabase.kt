package dev.prism.gallery.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.prism.gallery.data.local.dao.FavoriteDao
import dev.prism.gallery.data.local.dao.TrashDao
import dev.prism.gallery.data.local.entity.FavoriteEntity
import dev.prism.gallery.data.local.entity.TrashEntity

@Database(
    entities = [FavoriteEntity::class, TrashEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PrismDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun trashDao(): TrashDao
}
