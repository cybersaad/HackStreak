package com.cybersaad.hackstreak.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE username = :username LIMIT 1")
    fun getProfile(username: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE username = :username LIMIT 1")
    suspend fun getProfileSync(username: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profiles WHERE username = :username")
    suspend fun deleteProfile(username: String)
}
