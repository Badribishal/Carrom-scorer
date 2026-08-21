package com.example.carrom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey
    val id: Long,
    val team1Name: String,
    val team2Name: String,
    val team1PlayerNames: String,
    val team2PlayerNames: String,
    val firstBreakerPlayerId: Long,
    val firstBreakerPlayerName: String,
    val proMode: Boolean,
    val team1FinalScore: Int,
    val team2FinalScore: Int,
    val winnerTeamId: Int?,
    val winnerTeamName: String?,
    val boardsCount: Int,
    val handsCount: Int,
    val targetPoints: Int,
    val nillBoardOccurred: Boolean,
    val boardDetailsJson: String,
    val turnLogsJson: String,
    val isCompleted: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
