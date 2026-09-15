package com.aicomp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val PREF_NAME = "app_preferences"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREF_NAME)

object PreferencesManager {

    private const val KEY_DARK_MODE = "dark_mode"
    private const val DEFAULT_DARK_MODE = 1 // 1 = dark, 0 = light

    private val DARK_MODE_KEY = intPreferencesKey(KEY_DARK_MODE)

    /** One-shot read — call from a coroutine. */
    suspend fun getDarkMode(context: Context): Boolean {
        val prefs = context.dataStore.data.first()
        return (prefs[DARK_MODE_KEY] ?: DEFAULT_DARK_MODE) == 1
    }

    /** Live stream — collect this in a ViewModel/Composable. */
    fun observeDarkMode(context: Context): Flow<Boolean> = context.dataStore.data
        .map { prefs -> (prefs[DARK_MODE_KEY] ?: DEFAULT_DARK_MODE) == 1 }

    suspend fun setDarkMode(context: Context, isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = if (isDark) 1 else 0
        }
    }
}