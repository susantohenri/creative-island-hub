package com.henrisusanto.creativeislandhub.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesDataStore(private val context: Context) {

    companion object {
        val UNLOCKED_ISLANDS = stringSetPreferencesKey("unlocked_islands")
        val LIKED_ISLANDS = stringSetPreferencesKey("liked_islands")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "SYSTEM", "LIGHT", "DARK"
        val LANGUAGE = stringPreferencesKey("language") // "AUTO", "EN", "ID"
    }

    val unlockedIslandsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[UNLOCKED_ISLANDS] ?: emptySet()
    }

    val likedIslandsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[LIKED_ISLANDS] ?: emptySet()
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "SYSTEM"
    }
    
    val languageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE] ?: "AUTO"
    }

    suspend fun unlockIsland(code: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[UNLOCKED_ISLANDS] ?: emptySet()
            preferences[UNLOCKED_ISLANDS] = current + code
        }
    }

    suspend fun toggleLikeIsland(code: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[LIKED_ISLANDS] ?: emptySet()
            if (current.contains(code)) {
                preferences[LIKED_ISLANDS] = current - code
            } else {
                preferences[LIKED_ISLANDS] = current + code
            }
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE] = lang
        }
    }
}
