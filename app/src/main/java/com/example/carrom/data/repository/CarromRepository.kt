package com.example.carrom.data.repository

import com.example.carrom.data.local.CarromDatabase
import com.example.carrom.data.local.CarromJsonParser
import com.example.carrom.data.local.entity.ActiveMatchEntity
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

    private val finalizedMatchIds = Collections.synchronizedSet(mutableSetOf<Long>())

    val allPlayers: Flow<List<PlayerEntity>> = playerDao.getAllPlayers()
    val allMatches: Flow<List<MatchEntity>> = matchDao.getAllMatches()

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

    suspend fun getOrCreatePlayer(name: String, avatarColorIndex: Int = 0): PlayerEntity {
        val trimmed = name.trim()
        val existing = playerDao.getPlayerByName(trimmed)
        if (existing != null) {
            return existing
        }
        val newPlayer = PlayerEntity(
            name = trimmed,
            avatarColorIndex = avatarColorIndex
        )
        val id = playerDao.insertPlayer(newPlayer)
        return newPlayer.copy(id = id)
    }

    suspend fun insertPlayer(name: String, avatarColorIndex: Int = 0): Long {
        val trimmed = name.trim()
        val existing = playerDao.getPlayerByName(trimmed)
        if (existing != null) {
            return existing.id
        }
        return playerDao.insertPlayer(PlayerEntity(name = trimmed, avatarColorIndex = avatarColorIndex))
    }

    suspend fun getPlayerById(id: Long): PlayerEntity? {
        return playerDao.getPlayerById(id)
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

    suspend fun resetAllData() {
        activeMatchDao.clearActiveMatch()
        matchDao.deleteAllMatches()
        playerDao.deleteAllPlayers()
    }
}
