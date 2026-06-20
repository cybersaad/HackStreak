package com.cybersaad.hackstreak.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey
    val username: String,
    val streak: Int = 0,
    val rank: String = "N/A",
    val points: Int = 0,
    val roomsCompleted: Int = 0,
    val badges: Int = 0,
    val avatarUrl: String = "",
    val level: String = "",
    val lastSyncedTimestamp: Long = System.currentTimeMillis(),
    // Store weekly activity as comma-separated booleans (Mon-Sun)
    val weeklyActivity: String = "false,false,false,false,false,false,false"
)
