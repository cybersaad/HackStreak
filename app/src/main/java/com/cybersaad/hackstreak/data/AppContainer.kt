package com.cybersaad.hackstreak.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hackstreak_prefs")

class AppContainer(private val context: Context) {

    companion object {
        // No-op migration from 1 -> 2 to preserve existing schema
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // No schema changes; preserve data
            }
        }
    }

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "hackstreak_db"
        ).addMigrations(MIGRATION_1_2).build()
    }

    val prefsRepository: PrefsRepository by lazy {
        PrefsRepository(context.dataStore)
    }

    val scraper: ThmProfileScraper by lazy {
        ThmProfileScraper(context)
    }

    val repository: ThmRepository by lazy {
        ThmRepository(database.userProfileDao(), scraper, prefsRepository)
    }
}
