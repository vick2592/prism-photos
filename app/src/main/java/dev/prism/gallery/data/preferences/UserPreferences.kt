package dev.prism.gallery.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "prism_preferences")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val GRID_COLUMNS_KEY = intPreferencesKey("grid_columns")
    private val SORT_ORDER_KEY = stringPreferencesKey("sort_order")
    private val TRASH_DURATION_DAYS_KEY = intPreferencesKey("trash_duration_days")
    private val SLIDESHOW_INTERVAL_KEY = intPreferencesKey("slideshow_interval_seconds")

    val gridColumns: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[GRID_COLUMNS_KEY] ?: 3 }

    val sortOrder: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[SORT_ORDER_KEY] ?: "date_desc" }

    val trashDurationDays: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[TRASH_DURATION_DAYS_KEY] ?: 30 }

    val slideshowIntervalSeconds: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[SLIDESHOW_INTERVAL_KEY] ?: 3 }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { prefs -> prefs[GRID_COLUMNS_KEY] = columns }
    }

    suspend fun setSortOrder(order: String) {
        context.dataStore.edit { prefs -> prefs[SORT_ORDER_KEY] = order }
    }

    suspend fun setTrashDurationDays(days: Int) {
        context.dataStore.edit { prefs -> prefs[TRASH_DURATION_DAYS_KEY] = days }
    }

    suspend fun setSlideshowIntervalSeconds(seconds: Int) {
        context.dataStore.edit { prefs -> prefs[SLIDESHOW_INTERVAL_KEY] = seconds }
    }
}
