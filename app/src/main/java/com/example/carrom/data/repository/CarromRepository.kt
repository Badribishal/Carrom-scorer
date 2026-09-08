package com.example.carrom.data.repository

import com.example.carrom.data.local.CarromDatabase
import com.example.carrom.data.local.CarromJsonParser
import com.example.carrom.data.local.entity.ActiveMatchEntity
import com.example.carrom.data.local.entity.GroupEntity
import com.example.carrom.data.local.entity.MatchEntity
import com.example.carrom.data.local.entity.PlayerEntity
import com.example.carrom.engine.GameState
import com.example.carrom.engine.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import java.util.Collections

class CarromRepository(private val database: CarromDatabase) {

    private val playerDao = database.playerDao()
    private val matchDao = database.matchDao()
    private val activeMatchDao = database.activeMatchDao()
    private val groupDao = database.groupDao()

    private val finalizedMatchIds = Collections.synchronizedSet(mutableSetOf<Long>())

    val allPlayers: Flow<List<PlayerEntity>> = playerDao.getAllPlayers()
    val allMatches: Flow<List<MatchEntity>> = matchDao.getAllMatches()
    val allGroups: Flow<List<GroupEntity>> = groupDao.getAllGroups()

    val activeMatchFlow: Flow<GameState?> = activeMatchDao.getActiveMatchFlow().map { activeEntity ->
        if (activeEntity != null && activeEntity.gameStateJson.isNotBlank()) {
            try {
                CarromJsonParser.deserializeGameState(activeEntity.gameStateJson)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    suspend fun getOrCreatePlayer(
        name: String,
        avatarColorIndex: Int = 0,
        nickname: String = "",
        notes: String = "",
        skillLevel: String = "Intermediate",
        groupName: String = "General"
    ): PlayerEntity {
        val trimmed = name.trim()
        val existing = playerDao.getPlayerByName(trimmed)
        if (existing != null) {
            return existing
        }
        val newPlayer = PlayerEntity(
            name = trimmed,
            nickname = nickname.trim(),
            avatarColorIndex = avatarColorIndex,
            notes = notes.trim(),
            skillLevel = skillLevel,
            groupName = groupName.trim().ifBlank { "General" }
        )
        val id = playerDao.insertPlayer(newPlayer)
        return newPlayer.copy(id = id)
    }

    suspend fun insertPlayer(
        name: String,
        avatarColorIndex: Int = 0,
        nickname: String = "",
        notes: String = "",
        skillLevel: String = "Intermediate",
        groupName: String = "General"
    ): Long {
        val trimmed = name.trim()
        val existing = playerDao.getPlayerByName(trimmed)
        if (existing != null) {
            val updated = existing.copy(
                avatarColorIndex = avatarColorIndex,
                nickname = if (nickname.isNotBlank()) nickname.trim() else existing.nickname,
                notes = if (notes.isNotBlank()) notes.trim() else existing.notes,
                skillLevel = if (skillLevel.isNotBlank()) skillLevel else existing.skillLevel,
                groupName = if (groupName.isNotBlank()) groupName.trim() else existing.groupName
            )
            playerDao.updatePlayer(updated)
            return existing.id
        }
        return playerDao.insertPlayer(
            PlayerEntity(
                name = trimmed,
                nickname = nickname.trim(),
                avatarColorIndex = avatarColorIndex,
                notes = notes.trim(),
                skillLevel = skillLevel,
                groupName = groupName.trim().ifBlank { "General" }
            )
        )
    }

    suspend fun updatePlayer(player: PlayerEntity) {
        playerDao.updatePlayer(player)
    }

    suspend fun deletePlayerById(id: Long) {
        playerDao.deletePlayerById(id)
    }

    suspend fun getPlayerById(id: Long): PlayerEntity? {
        return playerDao.getPlayerById(id)
    }

    // Group Management Operations
    suspend fun getOrCreateGroup(
        name: String,
        description: String = "",
        colorIndex: Int = 0
    ): GroupEntity {
        val trimmed = name.trim()
        val existing = groupDao.getGroupByName(trimmed)
        if (existing != null) return existing
        val newGroup = GroupEntity(
            name = trimmed,
            description = description.trim(),
            colorIndex = colorIndex
        )
        val id = groupDao.insertGroup(newGroup)
        return newGroup.copy(id = id)
    }

    suspend fun insertGroup(
        name: String,
        description: String = "",
        colorIndex: Int = 0
    ): Long {
        val trimmed = name.trim()
        val existing = groupDao.getGroupByName(trimmed)
        if (existing != null) {
            val updated = existing.copy(
                description = if (description.isNotBlank()) description.trim() else existing.description,
                colorIndex = colorIndex
            )
            groupDao.updateGroup(updated)
            return existing.id
        }
        return groupDao.insertGroup(
            GroupEntity(
                name = trimmed,
                description = description.trim(),
                colorIndex = colorIndex
            )
        )
    }

    suspend fun updateGroup(group: GroupEntity) {
        groupDao.updateGroup(group)
    }

    suspend fun saveGroup(group: GroupEntity): Long {
        val existing = if (group.id > 0L) groupDao.getGroupById(group.id) else groupDao.getGroupByName(group.name.trim())
        return if (existing != null) {
            val updated = group.copy(id = existing.id)
            groupDao.updateGroup(updated)
            existing.id
        } else {
            groupDao.insertGroup(group)
        }
    }

    suspend fun deleteGroupById(id: Long) {
        groupDao.deleteGroupById(id)
    }

    suspend fun getAllGroupsList(): List<GroupEntity> {
        return groupDao.getAllGroupsList()
    }

    suspend fun addPlayerToGroup(groupId: Long, playerId: Long) {
        val group = groupDao.getGroupById(groupId) ?: return
        val updated = group.withPlayerAdded(playerId)
        groupDao.updateGroup(updated)
    }

    suspend fun removePlayerFromGroup(groupId: Long, playerId: Long) {
        val group = groupDao.getGroupById(groupId) ?: return
        val updated = group.withPlayerRemoved(playerId)
        groupDao.updateGroup(updated)
    }

    suspend fun setGroupMembers(groupId: Long, playerIds: Set<Long>) {
        val group = groupDao.getGroupById(groupId) ?: return
        val updated = group.copy(memberPlayerIds = playerIds.joinToString(","))
        groupDao.updateGroup(updated)
    }

    suspend fun getGroupById(id: Long): GroupEntity? {
        return groupDao.getGroupById(id)
    }

    suspend fun seedDefaultGroupsIfEmpty() {
        if (groupDao.getAllGroupsList().isEmpty()) {
            groupDao.insertGroups(
                listOf(
                    GroupEntity(
                        name = "Friday Team",
                        description = "Friday evening regulars",
                        colorIndex = 1,
                        team1Name = "Team 1",
                        team2Name = "Team 2",
                        team1Player1 = "Rahul",
                        team1Player2 = "Amit",
                        team2Player1 = "Suman",
                        team2Player2 = "Raj",
                        isDoubles = true
                    ),
                    GroupEntity(
                        name = "College Group",
                        description = "College friends carrom squad",
                        colorIndex = 2,
                        team1Name = "Team 1",
                        team2Name = "Team 2",
                        team1Player1 = "Player A",
                        team1Player2 = "Player B",
                        team2Player1 = "Player C",
                        team2Player2 = "Player D",
                        isDoubles = true
                    ),
                    GroupEntity(
                        name = "Regular Players",
                        description = "Local club weekend tournament",
                        colorIndex = 0,
                        team1Name = "Team 1",
                        team2Name = "Team 2",
                        team1Player1 = "Anand",
                        team1Player2 = "Vikram",
                        team2Player1 = "Rohan",
                        team2Player2 = "Suresh",
                        isDoubles = true
                    ),
                    GroupEntity(
                        name = "Family Match",
                        description = "Home & Family game night",
                        colorIndex = 3,
                        team1Name = "Team 1",
                        team2Name = "Team 2",
                        team1Player1 = "Dad",
                        team1Player2 = "Mom",
                        team2Player1 = "Brother",
                        team2Player2 = "Sister",
                        isDoubles = true
                    )
                )
            )
        }
    }

    suspend fun saveActiveMatch(state: GameState) {
        val json = CarromJsonParser.serializeGameState(state)
        activeMatchDao.saveActiveMatch(
            ActiveMatchEntity(
                id = 1,
                gameStateJson = json,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    suspend fun getActiveMatch(): GameState? {
        val active = activeMatchDao.getActiveMatch() ?: return null
        return try {
            CarromJsonParser.deserializeGameState(active.gameStateJson)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun clearActiveMatch() {
        activeMatchDao.clearActiveMatch()
    }

    suspend fun finalizeAndSaveMatch(state: GameState) {
        val config = state.config
        val t1Players = config.team1Players.joinToString(", ") { it.name }
        val t2Players = config.team2Players.joinToString(", ") { it.name }
        val breaker = config.team1Players.find { it.id == config.firstBreakerPlayerId }
            ?: config.team2Players.find { it.id == config.firstBreakerPlayerId }
        val breakerName = breaker?.name ?: "Unknown"

        val winnerName = when (state.matchWinnerTeamId) {
            1 -> config.team1Name
            2 -> config.team2Name
            else -> null
        }

        val hasNillBoard = state.completedBoards.any { it.isNillBoard }
        val totalHands = state.completedBoards.sumOf { it.handsPlayed }

        val matchEntity = MatchEntity(
            id = state.matchId,
            team1Name = config.team1Name,
            team2Name = config.team2Name,
            team1PlayerNames = t1Players,
            team2PlayerNames = t2Players,
            firstBreakerPlayerId = config.firstBreakerPlayerId,
            firstBreakerPlayerName = breakerName,
            proMode = config.proMode,
            team1FinalScore = state.team1Score,
            team2FinalScore = state.team2Score,
            winnerTeamId = state.matchWinnerTeamId,
            winnerTeamName = winnerName,
            boardsCount = state.completedBoards.size,
            handsCount = totalHands,
            targetPoints = config.targetPoints,
            nillBoardOccurred = hasNillBoard,
            boardDetailsJson = CarromJsonParser.serializeBoardRecords(state.completedBoards),
            turnLogsJson = CarromJsonParser.serializeTurnRecords(state.allTurnLogs),
            isCompleted = state.isMatchOver,
            timestamp = state.endTime ?: System.currentTimeMillis()
        )

        matchDao.insertMatch(matchEntity)

        // Only update player stats once per unique matchId to prevent double counting
        val isFirstFinalization = synchronized(finalizedMatchIds) {
            finalizedMatchIds.add(state.matchId)
        }
        if (isFirstFinalization) {
            updatePlayerStats(state)
        }
        clearActiveMatch()
    }

    private suspend fun updatePlayerStats(state: GameState) {
        val allMatchPlayers = state.config.team1Players + state.config.team2Players
        val winnerTeamId = state.matchWinnerTeamId

        for (p in allMatchPlayers) {
            val existing = playerDao.getPlayerById(p.id) ?: continue
            val teamId = state.config.getPlayerTeamId(p.id)

            val wonMatch = if (winnerTeamId != null && winnerTeamId == teamId) 1 else 0
            val lostMatch = if (winnerTeamId != null && winnerTeamId != teamId) 1 else 0

            val boardsWonCount = state.completedBoards.count { it.winningTeamId == teamId }
            val nillBoardWinsCount = state.completedBoards.count { it.winningTeamId == teamId && it.isNillBoard }
            val nillBoardLossesCount = state.completedBoards.count { it.winningTeamId != teamId && it.isNillBoard }

            val playerTurns = state.allTurnLogs.filter { it.playerId == p.id }
            val totalWhite = playerTurns.sumOf { it.whitePocketed }
            val totalBlack = playerTurns.sumOf { it.blackPocketed }
            val queenAttempts = playerTurns.count { it.queenPocketed }
            val queenCovers = playerTurns.count { it.queenCovered }
            val penalties = playerTurns.sumOf { it.penalties }

            val queenPointsFromBoards = state.completedBoards.filter { it.queenCoveredByPlayerId == p.id && it.winningTeamId == teamId }
                .sumOf { it.queenPointsAwarded }

            val totalCoins = totalWhite + totalBlack
            val pointsContributed = totalCoins + queenPointsFromBoards

            val updatedPlayer = existing.copy(
                matchesPlayed = existing.matchesPlayed + 1,
                matchesWon = existing.matchesWon + wonMatch,
                matchesLost = existing.matchesLost + lostMatch,
                boardsPlayed = existing.boardsPlayed + state.completedBoards.size,
                boardsWon = existing.boardsWon + boardsWonCount,
                handsPlayed = existing.handsPlayed + state.completedBoards.sumOf { it.handsPlayed },
                turnsPlayed = existing.turnsPlayed + playerTurns.size,
                whitePocketed = existing.whitePocketed + totalWhite,
                blackPocketed = existing.blackPocketed + totalBlack,
                queenAttempts = existing.queenAttempts + queenAttempts,
                queensCovered = existing.queensCovered + queenCovers,
                queenPointsScored = existing.queenPointsScored + queenPointsFromBoards,
                penalties = existing.penalties + penalties,
                nillBoardWins = existing.nillBoardWins + nillBoardWinsCount,
                nillBoardLosses = existing.nillBoardLosses + nillBoardLossesCount,
                totalPointsContributed = existing.totalPointsContributed + pointsContributed
            )

            playerDao.updatePlayer(updatedPlayer)
        }
    }

    suspend fun deleteMatchById(id: Long) {
        matchDao.deleteMatchById(id)
    }

    suspend fun getAllMatchesList(): List<MatchEntity> {
        return matchDao.getAllMatchesList()
    }

    suspend fun getAllPlayersList(): List<PlayerEntity> {
        return playerDao.getAllPlayersList()
    }

    suspend fun importFullBackup(
        players: List<PlayerEntity>,
        matches: List<MatchEntity>,
        groups: List<GroupEntity> = emptyList(),
        replaceAll: Boolean
    ): Triple<Int, Int, Int> {
        if (replaceAll) {
            activeMatchDao.clearActiveMatch()
            matchDao.deleteAllMatches()
            playerDao.deleteAllPlayers()
            groupDao.deleteAllGroups()
        }

        var playersImported = 0
        var matchesImported = 0
        var groupsImported = 0

        // Import / merge groups
        for (group in groups) {
            if (group.name.isBlank()) continue
            val existing = groupDao.getGroupByName(group.name.trim())
            if (existing == null) {
                groupDao.insertGroup(group.copy(id = 0L))
                groupsImported++
            } else if (replaceAll) {
                groupDao.insertGroup(group)
                groupsImported++
            } else {
                // Merge group details
                val merged = existing.copy(
                    description = if (group.description.isNotBlank()) group.description else existing.description,
                    colorIndex = if (group.colorIndex != 0) group.colorIndex else existing.colorIndex,
                    team1Name = if (group.team1Name.isNotBlank() && group.team1Name != "Team 1") group.team1Name else existing.team1Name,
                    team2Name = if (group.team2Name.isNotBlank() && group.team2Name != "Team 2") group.team2Name else existing.team2Name,
                    team1Player1 = if (group.team1Player1.isNotBlank()) group.team1Player1 else existing.team1Player1,
                    team1Player2 = if (group.team1Player2.isNotBlank()) group.team1Player2 else existing.team1Player2,
                    team2Player1 = if (group.team2Player1.isNotBlank()) group.team2Player1 else existing.team2Player1,
                    team2Player2 = if (group.team2Player2.isNotBlank()) group.team2Player2 else existing.team2Player2,
                    isDoubles = group.isDoubles,
                    memberPlayerIds = mergeMemberIds(existing.memberPlayerIds, group.memberPlayerIds)
                )
                groupDao.updateGroup(merged)
                groupsImported++
            }
        }

        // Import / merge players
        for (player in players) {
            val existing = playerDao.getPlayerByName(player.name)
            if (player.groupName.isNotBlank() && !player.groupName.equals("General", ignoreCase = true)) {
                getOrCreateGroup(player.groupName)
            }
            if (existing == null) {
                playerDao.insertPlayer(player.copy(id = 0L))
                playersImported++
            } else if (replaceAll) {
                playerDao.insertPlayer(player)
                playersImported++
            } else {
                // Merge player stats if existing
                val merged = existing.copy(
                    nickname = if (player.nickname.isNotBlank()) player.nickname else existing.nickname,
                    avatarColorIndex = player.avatarColorIndex,
                    notes = if (player.notes.isNotBlank()) player.notes else existing.notes,
                    skillLevel = player.skillLevel,
                    groupName = if (player.groupName.isNotBlank() && !player.groupName.equals("General", ignoreCase = true)) player.groupName else existing.groupName,
                    matchesPlayed = maxOf(existing.matchesPlayed, player.matchesPlayed),
                    matchesWon = maxOf(existing.matchesWon, player.matchesWon),
                    matchesLost = maxOf(existing.matchesLost, player.matchesLost),
                    boardsPlayed = maxOf(existing.boardsPlayed, player.boardsPlayed),
                    boardsWon = maxOf(existing.boardsWon, player.boardsWon),
                    whitePocketed = maxOf(existing.whitePocketed, player.whitePocketed),
                    blackPocketed = maxOf(existing.blackPocketed, player.blackPocketed),
                    queenAttempts = maxOf(existing.queenAttempts, player.queenAttempts),
                    queensCovered = maxOf(existing.queensCovered, player.queensCovered),
                    queenPointsScored = maxOf(existing.queenPointsScored, player.queenPointsScored),
                    penalties = maxOf(existing.penalties, player.penalties),
                    totalPointsContributed = maxOf(existing.totalPointsContributed, player.totalPointsContributed)
                )
                playerDao.updatePlayer(merged)
                playersImported++
            }
        }

        // Import matches
        for (match in matches) {
            val existing = matchDao.getMatchById(match.id)
            if (existing == null || replaceAll) {
                matchDao.insertMatch(match)
                matchesImported++
            }
        }

        return Triple(playersImported, matchesImported, groupsImported)
    }

    private fun mergeMemberIds(ids1: String, ids2: String): String {
        val set1 = ids1.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
        val set2 = ids2.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
        val combined = (set1 + set2).filter { it > 0 }
        return combined.joinToString(",")
    }

    suspend fun resetAllData() {
        activeMatchDao.clearActiveMatch()
        matchDao.deleteAllMatches()
        playerDao.deleteAllPlayers()
        groupDao.deleteAllGroups()
    }
}
