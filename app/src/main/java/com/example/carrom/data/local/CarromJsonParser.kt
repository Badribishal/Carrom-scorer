package com.example.carrom.data.local

import com.example.carrom.engine.*
import org.json.JSONArray
import org.json.JSONObject

object CarromJsonParser {

    fun serializeMatchConfig(config: MatchConfig): String {
        val json = JSONObject()
        json.put("team1Name", config.team1Name)
        json.put("team2Name", config.team2Name)
        json.put("firstBreakerPlayerId", config.firstBreakerPlayerId)
        json.put("proMode", config.proMode)
        json.put("targetPoints", config.targetPoints)
        json.put("nillBoardThreshold", config.nillBoardThreshold)
        json.put("queenPoints", config.queenPoints)
        json.put("queenStopThreshold", config.queenStopThreshold)
        json.put("enableQueenStopRule", config.enableQueenStopRule)

        val t1Array = JSONArray()
        config.team1Players.forEach { p ->
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("name", p.name)
            pObj.put("avatarColorIndex", p.avatarColorIndex)
            t1Array.put(pObj)
        }
        json.put("team1Players", t1Array)

        val t2Array = JSONArray()
        config.team2Players.forEach { p ->
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("name", p.name)
            pObj.put("avatarColorIndex", p.avatarColorIndex)
            t2Array.put(pObj)
        }
        json.put("team2Players", t2Array)

        return json.toString()
    }

    fun deserializeMatchConfig(jsonStr: String): MatchConfig {
        val json = JSONObject(jsonStr)
        val team1Name = json.optString("team1Name", "Team 1")
        val team2Name = json.optString("team2Name", "Team 2")
        val firstBreakerPlayerId = json.optLong("firstBreakerPlayerId", 0L)
        val proMode = json.optBoolean("proMode", true)
        val targetPoints = json.optInt("targetPoints", 29)
        val nillBoardThreshold = json.optInt("nillBoardThreshold", 7)
        val queenPoints = json.optInt("queenPoints", 5)
        val queenStopThreshold = json.optInt("queenStopThreshold", 19)
        val enableQueenStopRule = if (json.has("enableQueenStopRule")) json.optBoolean("enableQueenStopRule", true) else json.optBoolean("enable24PlusQueenRule", true)

        val team1Players = mutableListOf<Player>()
        val t1Arr = json.optJSONArray("team1Players") ?: JSONArray()
        for (i in 0 until t1Arr.length()) {
            val pObj = t1Arr.getJSONObject(i)
            team1Players.add(
                Player(
                    id = pObj.getLong("id"),
                    name = pObj.getString("name"),
                    avatarColorIndex = pObj.optInt("avatarColorIndex", 0)
                )
            )
        }

        val team2Players = mutableListOf<Player>()
        val t2Arr = json.optJSONArray("team2Players") ?: JSONArray()
        for (i in 0 until t2Arr.length()) {
            val pObj = t2Arr.getJSONObject(i)
            team2Players.add(
                Player(
                    id = pObj.getLong("id"),
                    name = pObj.getString("name"),
                    avatarColorIndex = pObj.optInt("avatarColorIndex", 0)
                )
            )
        }

        return MatchConfig(
            team1Name = team1Name,
            team2Name = team2Name,
            team1Players = team1Players,
            team2Players = team2Players,
            firstBreakerPlayerId = firstBreakerPlayerId,
            proMode = proMode,
            targetPoints = targetPoints,
            nillBoardThreshold = nillBoardThreshold,
            queenPoints = queenPoints,
            queenStopThreshold = queenStopThreshold,
            enableQueenStopRule = enableQueenStopRule
        )
    }

    fun serializeBoardRecords(boards: List<BoardRecord>): String {
        val array = JSONArray()
        boards.forEach { b ->
            val obj = JSONObject()
            obj.put("boardNumber", b.boardNumber)
            obj.put("breakerPlayerId", b.breakerPlayerId)
            obj.put("breakerPlayerName", b.breakerPlayerName)
            obj.put("winningTeamId", b.winningTeamId)
            obj.put("winningTeamName", b.winningTeamName)
            obj.put("whiteRemaining", b.whiteRemaining)
            obj.put("blackRemaining", b.blackRemaining)
            obj.put("opponentRemainingCoins", b.opponentRemainingCoins)
            if (b.queenCoveredByPlayerId != null) obj.put("queenCoveredByPlayerId", b.queenCoveredByPlayerId)
            if (b.queenCoveredByPlayerName != null) obj.put("queenCoveredByPlayerName", b.queenCoveredByPlayerName)
            if (b.queenCoveredByTeamId != null) obj.put("queenCoveredByTeamId", b.queenCoveredByTeamId)
            obj.put("queenPointsAwarded", b.queenPointsAwarded)
            obj.put("boardScore", b.boardScore)
            obj.put("isNillBoard", b.isNillBoard)
            obj.put("isNillMatchWin", b.isNillMatchWin)
            obj.put("team1ScoreAfterBoard", b.team1ScoreAfterBoard)
            obj.put("team2ScoreAfterBoard", b.team2ScoreAfterBoard)
            obj.put("handsPlayed", b.handsPlayed)
            obj.put("turnsPlayed", b.turnsPlayed)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeBoardRecords(jsonStr: String): List<BoardRecord> {
        val list = mutableListOf<BoardRecord>()
        if (jsonStr.isBlank()) return list
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                BoardRecord(
                    boardNumber = obj.getInt("boardNumber"),
                    breakerPlayerId = obj.optLong("breakerPlayerId", 0L),
                    breakerPlayerName = obj.optString("breakerPlayerName", ""),
                    winningTeamId = obj.getInt("winningTeamId"),
                    winningTeamName = obj.optString("winningTeamName", ""),
                    whiteRemaining = obj.getInt("whiteRemaining"),
                    blackRemaining = obj.getInt("blackRemaining"),
                    opponentRemainingCoins = obj.getInt("opponentRemainingCoins"),
                    queenCoveredByPlayerId = if (obj.has("queenCoveredByPlayerId")) obj.getLong("queenCoveredByPlayerId") else null,
                    queenCoveredByPlayerName = if (obj.has("queenCoveredByPlayerName")) obj.getString("queenCoveredByPlayerName") else null,
                    queenCoveredByTeamId = if (obj.has("queenCoveredByTeamId")) obj.getInt("queenCoveredByTeamId") else null,
                    queenPointsAwarded = obj.getInt("queenPointsAwarded"),
                    boardScore = obj.getInt("boardScore"),
                    isNillBoard = obj.optBoolean("isNillBoard", false),
                    isNillMatchWin = obj.optBoolean("isNillMatchWin", false),
                    team1ScoreAfterBoard = obj.getInt("team1ScoreAfterBoard"),
                    team2ScoreAfterBoard = obj.getInt("team2ScoreAfterBoard"),
                    handsPlayed = obj.optInt("handsPlayed", 1),
                    turnsPlayed = obj.optInt("turnsPlayed", 1)
                )
            )
        }
        return list
    }

    fun serializeTurnRecords(turns: List<TurnRecord>): String {
        val array = JSONArray()
        turns.forEach { t ->
            val obj = JSONObject()
            obj.put("turnNumber", t.turnNumber)
            obj.put("handNumber", t.handNumber)
            obj.put("playerId", t.playerId)
            obj.put("playerName", t.playerName)
            obj.put("teamId", t.teamId)
            obj.put("teamColor", t.teamColor.name)
            obj.put("whitePocketed", t.whitePocketed)
            obj.put("blackPocketed", t.blackPocketed)
            obj.put("queenPocketed", t.queenPocketed)
            obj.put("queenCovered", t.queenCovered)
            obj.put("penalties", t.penalties)
            obj.put("timestamp", t.timestamp)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeTurnRecords(jsonStr: String): List<TurnRecord> {
        val list = mutableListOf<TurnRecord>()
        if (jsonStr.isBlank()) return list
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                TurnRecord(
                    turnNumber = obj.getInt("turnNumber"),
                    handNumber = obj.getInt("handNumber"),
                    playerId = obj.getLong("playerId"),
                    playerName = obj.getString("playerName"),
                    teamId = obj.getInt("teamId"),
                    teamColor = TeamColor.valueOf(obj.getString("teamColor")),
                    whitePocketed = obj.getInt("whitePocketed"),
                    blackPocketed = obj.getInt("blackPocketed"),
                    queenPocketed = obj.getBoolean("queenPocketed"),
                    queenCovered = obj.getBoolean("queenCovered"),
                    penalties = obj.getInt("penalties"),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }
        return list
    }

    fun serializeGameState(state: GameState): String {
        val json = JSONObject()
        json.put("matchId", state.matchId)
        json.put("config", serializeMatchConfig(state.config))
        json.put("team1Score", state.team1Score)
        json.put("team2Score", state.team2Score)
        json.put("currentBoardNumber", state.currentBoardNumber)
        json.put("isMatchOver", state.isMatchOver)
        if (state.matchWinnerTeamId != null) json.put("matchWinnerTeamId", state.matchWinnerTeamId)
        json.put("isWonByNillRule", state.isWonByNillRule)
        json.put("startTime", state.startTime)
        if (state.endTime != null) json.put("endTime", state.endTime)

        // Board State
        val bObj = JSONObject()
        bObj.put("boardNumber", state.boardState.boardNumber)
        bObj.put("whiteRemaining", state.boardState.whiteRemaining)
        bObj.put("blackRemaining", state.boardState.blackRemaining)
        bObj.put("queenStatus", state.boardState.queenStatus.name)
        if (state.boardState.queenPocketedByPlayerId != null) bObj.put("queenPocketedByPlayerId", state.boardState.queenPocketedByPlayerId)
        if (state.boardState.queenPocketedByTeamId != null) bObj.put("queenPocketedByTeamId", state.boardState.queenPocketedByTeamId)
        if (state.boardState.queenCoveredByPlayerId != null) bObj.put("queenCoveredByPlayerId", state.boardState.queenCoveredByPlayerId)
        if (state.boardState.queenCoveredByTeamId != null) bObj.put("queenCoveredByTeamId", state.boardState.queenCoveredByTeamId)
        bObj.put("isCompleted", state.boardState.isCompleted)
        if (state.boardState.winnerTeamId != null) bObj.put("winnerTeamId", state.boardState.winnerTeamId)
        bObj.put("boardScore", state.boardState.boardScore)
        bObj.put("isNillBoard", state.boardState.isNillBoard)
        json.put("boardState", bObj)

        // Turn State
        val tObj = JSONObject()
        tObj.put("currentHand", state.turnState.currentHand)
        tObj.put("currentTurnIndexInRotation", state.turnState.currentTurnIndexInRotation)
        tObj.put("currentOverallTurnNumber", state.turnState.currentOverallTurnNumber)
        tObj.put("currentTurnWhite", state.turnState.currentTurnWhite)
        tObj.put("currentTurnBlack", state.turnState.currentTurnBlack)
        tObj.put("currentTurnQueenPocketed", state.turnState.currentTurnQueenPocketed)
        tObj.put("currentTurnQueenCovered", state.turnState.currentTurnQueenCovered)
        tObj.put("currentTurnPenalties", state.turnState.currentTurnPenalties)
        json.put("turnState", tObj)

        json.put("completedBoards", serializeBoardRecords(state.completedBoards))
        json.put("allTurnLogs", serializeTurnRecords(state.allTurnLogs))

        return json.toString()
    }

    fun deserializeGameState(jsonStr: String): GameState {
        val json = JSONObject(jsonStr)
        val matchId = json.getLong("matchId")
        val config = deserializeMatchConfig(json.getString("config"))
        val team1Score = json.getInt("team1Score")
        val team2Score = json.getInt("team2Score")
        val currentBoardNumber = json.getInt("currentBoardNumber")
        val isMatchOver = json.optBoolean("isMatchOver", false)
        val matchWinnerTeamId = if (json.has("matchWinnerTeamId")) json.getInt("matchWinnerTeamId") else null
        val isWonByNillRule = json.optBoolean("isWonByNillRule", false)
        val startTime = json.optLong("startTime", System.currentTimeMillis())
        val endTime = if (json.has("endTime")) json.getLong("endTime") else null

        val bObj = json.getJSONObject("boardState")
        val boardState = BoardLiveState(
            boardNumber = bObj.getInt("boardNumber"),
            whiteRemaining = bObj.getInt("whiteRemaining"),
            blackRemaining = bObj.getInt("blackRemaining"),
            queenStatus = QueenStatus.valueOf(bObj.getString("queenStatus")),
            queenPocketedByPlayerId = if (bObj.has("queenPocketedByPlayerId")) bObj.getLong("queenPocketedByPlayerId") else null,
            queenPocketedByTeamId = if (bObj.has("queenPocketedByTeamId")) bObj.getInt("queenPocketedByTeamId") else null,
            queenCoveredByPlayerId = if (bObj.has("queenCoveredByPlayerId")) bObj.getLong("queenCoveredByPlayerId") else null,
            queenCoveredByTeamId = if (bObj.has("queenCoveredByTeamId")) bObj.getInt("queenCoveredByTeamId") else null,
            isCompleted = bObj.optBoolean("isCompleted", false),
            winnerTeamId = if (bObj.has("winnerTeamId")) bObj.getInt("winnerTeamId") else null,
            boardScore = bObj.optInt("boardScore", 0),
            isNillBoard = bObj.optBoolean("isNillBoard", false)
        )

        val tObj = json.getJSONObject("turnState")
        val turnState = TurnLiveState(
            currentHand = tObj.getInt("currentHand"),
            currentTurnIndexInRotation = tObj.getInt("currentTurnIndexInRotation"),
            currentOverallTurnNumber = tObj.getInt("currentOverallTurnNumber"),
            currentTurnWhite = tObj.getInt("currentTurnWhite"),
            currentTurnBlack = tObj.getInt("currentTurnBlack"),
            currentTurnQueenPocketed = tObj.getBoolean("currentTurnQueenPocketed"),
            currentTurnQueenCovered = tObj.getBoolean("currentTurnQueenCovered"),
            currentTurnPenalties = tObj.getInt("currentTurnPenalties"),
            undoStack = emptyList()
        )

        val completedBoards = deserializeBoardRecords(json.optString("completedBoards", "[]"))
        val allTurnLogs = deserializeTurnRecords(json.optString("allTurnLogs", "[]"))

        return GameState(
            matchId = matchId,
            config = config,
            team1Score = team1Score,
            team2Score = team2Score,
            currentBoardNumber = currentBoardNumber,
            boardState = boardState,
            turnState = turnState,
            completedBoards = completedBoards,
            allTurnLogs = allTurnLogs,
            isMatchOver = isMatchOver,
            matchWinnerTeamId = matchWinnerTeamId,
            isWonByNillRule = isWonByNillRule,
            startTime = startTime,
            endTime = endTime
        )
    }
}
