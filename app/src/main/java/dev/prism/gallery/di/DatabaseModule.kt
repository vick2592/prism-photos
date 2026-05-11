package dev.prism.gallery.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.prism.gallery.data.local.PrismDatabase
import dev.prism.gallery.data.local.dao.FavoriteDao
import dev.prism.gallery.data.local.dao.TrashDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PrismDatabase =
        Room.databaseBuilder(
            context,
            PrismDatabase::class.java,
            "prism_database",
        ).build()

    @Provides
    fun provideFavoriteDao(database: PrismDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun provideTrashDao(database: PrismDatabase): TrashDao = database.trashDao()
}
