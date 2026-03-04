package com.example.clientproductivity.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferenceManager @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.dataStore

    object PreferencesKeys {
        val TASK_REMINDERS_ENABLED = booleanPreferencesKey("task_reminders_enabled")
        val THEME_ID = intPreferencesKey("theme_id")
    }

    val taskRemindersEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TASK_REMINDERS_ENABLED] ?: true
    }

    val themeId: Flow<Int> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_ID] ?: 0
    }

    suspend fun setTaskRemindersEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TASK_REMINDERS_ENABLED] = enabled
        }
    }

    suspend fun setThemeId(id: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_ID] = id
        }
    }
}
