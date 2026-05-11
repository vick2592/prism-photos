package dev.prism.gallery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.prism.gallery.data.local.entity.FavoriteEntity
import dev.prism.gallery.data.local.entity.TrashEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId)")
    suspend fun isFavorite(mediaId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaId = :mediaId")
    suspend fun removeFavorite(mediaId: Long)
}

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<TrashEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToTrash(entity: TrashEntity)

    @Query("DELETE FROM trash WHERE mediaId = :mediaId")
    suspend fun removeFromTrash(mediaId: Long)

    /** Purge items that have been in trash longer than [olderThanMs] milliseconds. */
    @Query("DELETE FROM trash WHERE deletedAt < :olderThanMs")
    suspend fun purgeExpired(olderThanMs: Long)
}
