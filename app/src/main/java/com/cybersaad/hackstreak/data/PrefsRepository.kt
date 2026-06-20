package com.cybersaad.hackstreak.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PrefsRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val KEY_USERNAME = stringPreferencesKey("saved_username")
        private val KEY_LAST_SYNC = longPreferencesKey("last_sync_timestamp")
    }

    val savedUsername: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_USERNAME] ?: ""
    }

    val lastSyncTimestamp: Flow<Long> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_SYNC] ?: 0L
    }

    suspend fun saveUsername(username: String) {
        dataStore.edit { prefs ->
            prefs[KEY_USERNAME] = username
        }
    }

    suspend fun saveLastSyncTimestamp(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC] = timestamp
        }
    }
}
