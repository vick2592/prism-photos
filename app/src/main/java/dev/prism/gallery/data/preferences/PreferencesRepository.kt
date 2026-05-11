package dev.prism.gallery.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "prism_settings")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_GRID_COLUMNS = intPreferencesKey("grid_columns")
        private val KEY_SLIDESHOW_INTERVAL = intPreferencesKey("slideshow_interval_secs")

        const val DEFAULT_GRID_COLUMNS = 3
        const val DEFAULT_SLIDESHOW_INTERVAL = 3
    }

    val gridColumns: Flow<Int> = context.dataStore.data
        .map { it[KEY_GRID_COLUMNS] ?: DEFAULT_GRID_COLUMNS }

    val slideshowInterval: Flow<Int> = context.dataStore.data
        .map { it[KEY_SLIDESHOW_INTERVAL] ?: DEFAULT_SLIDESHOW_INTERVAL }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { it[KEY_GRID_COLUMNS] = columns }
    }

    suspend fun setSlideshowInterval(secs: Int) {
        context.dataStore.edit { it[KEY_SLIDESHOW_INTERVAL] = secs }
    }
}
