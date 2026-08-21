package com.example.carrom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val avatarColorIndex: Int = 0,
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    val matchesLost: Int = 0,
    val boardsPlayed: Int = 0,
    val boardsWon: Int = 0,
    val handsPlayed: Int = 0,
    val turnsPlayed: Int = 0,
    val whitePocketed: Int = 0,
    val blackPocketed: Int = 0,
    val queenAttempts: Int = 0,
    val queensCovered: Int = 0,
    val queenPointsScored: Int = 0,
    val penalties: Int = 0,
    val nillBoardWins: Int = 0,
    val nillBoardLosses: Int = 0,
    val totalPointsContributed: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val winRate: Float
        get() = if (matchesPlayed > 0) (matchesWon.toFloat() / matchesPlayed) * 100f else 0f

    val totalCoinsPocketed: Int
        get() = whitePocketed + blackPocketed

    val queenSuccessRate: Float
        get() = if (queenAttempts > 0) (queensCovered.toFloat() / queenAttempts) * 100f else 0f
}
