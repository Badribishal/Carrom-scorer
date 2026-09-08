package com.example.carrom.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val colorIndex: Int = 0,
    val memberPlayerIds: String = "",
    val team1Name: String = "Team 1",
    val team2Name: String = "Team 2",
    val team1Player1: String = "",
    val team1Player2: String = "",
    val team2Player1: String = "",
    val team2Player2: String = "",
    val isDoubles: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getMemberIds(): Set<Long> {
        if (memberPlayerIds.isBlank()) return emptySet()
        return memberPlayerIds.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .toSet()
    }

    fun hasMember(playerId: Long): Boolean {
        return getMemberIds().contains(playerId)
    }

    fun withPlayerAdded(playerId: Long): GroupEntity {
        val current = getMemberIds().toMutableSet()
        current.add(playerId)
        return copy(memberPlayerIds = current.joinToString(","))
    }

    fun withPlayerRemoved(playerId: Long): GroupEntity {
        val current = getMemberIds().toMutableSet()
        current.remove(playerId)
        return copy(memberPlayerIds = current.joinToString(","))
    }

    fun getAllPlayerNames(): List<String> {
        return if (isDoubles) {
            listOf(team1Player1, team1Player2, team2Player1, team2Player2).filter { it.isNotBlank() }
        } else {
            listOf(team1Player1, team2Player1).filter { it.isNotBlank() }
        }
    }

    fun getSummaryText(): String {
        return if (isDoubles) {
            "T1: $team1Player1, $team1Player2  vs  T2: $team2Player1, $team2Player2"
        } else {
            "T1: $team1Player1  vs  T2: $team2Player1"
        }
    }
}
