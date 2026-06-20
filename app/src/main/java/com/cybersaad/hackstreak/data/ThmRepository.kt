package com.cybersaad.hackstreak.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Result of a sync operation that carries more context than a simple Result.
 */
sealed class SyncResult {
    data class Success(val profile: UserProfileEntity) : SyncResult()
    data class StaleCache(val profile: UserProfileEntity, val errorMessage: String) : SyncResult()
    data class Failure(val errorMessage: String) : SyncResult()
}

class ThmRepository(
    private val dao: UserProfileDao,
    private val scraper: ThmProfileScraper,
    private val prefs: PrefsRepository
) {

    fun getProfile(username: String): Flow<UserProfileEntity?> = dao.getProfile(username)

    suspend fun getSavedUsername(): String = prefs.savedUsername.first()

    suspend fun saveUsername(username: String) = prefs.saveUsername(username)

    fun observeUsername(): Flow<String> = prefs.savedUsername

    fun observeLastSync(): Flow<Long> = prefs.lastSyncTimestamp

    /**
     * Syncs the user profile by scraping TryHackMe.
     * Returns a [SyncResult] that distinguishes between:
     * - Fresh success
     * - Stale cache (scraping failed but we have old data)
     * - Total failure (scraping failed and no cache)
     */
    suspend fun syncProfile(username: String): SyncResult {
        val result = scraper.scrapeProfile(username)
        return result.fold(
            onSuccess = { scraped ->
                val now = System.currentTimeMillis()
                val weeklyStr = scraped.weeklyActivity.joinToString(",")
                val entity = UserProfileEntity(
                    username = scraped.username,
                    streak = scraped.streak,
                    rank = scraped.rank,
                    points = scraped.points,
                    roomsCompleted = scraped.roomsCompleted,
                    badges = scraped.badges,
                    avatarUrl = scraped.avatarUrl,
                    level = scraped.level,
                    lastSyncedTimestamp = now,
                    weeklyActivity = weeklyStr
                )
                dao.insertProfile(entity)
                prefs.saveUsername(username)
                prefs.saveLastSyncTimestamp(now)
                SyncResult.Success(entity)
            },
            onFailure = { error ->
                // If scraping failed, try to return cached data but mark it as stale
                val cached = dao.getProfileSync(username)
                if (cached != null) {
                    SyncResult.StaleCache(
                        profile = cached,
                        errorMessage = error.message ?: "Sync failed"
                    )
                } else {
                    SyncResult.Failure(
                        errorMessage = error.message ?: "Sync failed. Please try again."
                    )
                }
            }
        )
    }
}
