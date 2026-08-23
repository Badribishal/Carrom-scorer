package com.example.carrom.export

import com.example.carrom.data.local.CarromJsonParser
import com.example.carrom.data.local.entity.MatchEntity
import com.example.carrom.data.local.entity.PlayerEntity
import com.example.carrom.engine.BoardRecord
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object CarromCsvExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * Exports all matches as CSV text
     */
    fun exportMatchesToCsv(matches: List<MatchEntity>): String {
        val sb = StringBuilder()
        // Header
        sb.append("Match_ID,Date_Time,Team_1_Name,Team_2_Name,Team_1_Players,Team_2_Players,Team_1_Score,Team_2_Score,Winner_Team,Target_Points,Boards_Count,Hands_Count,Pro_Mode,First_Breaker,Nill_Board_Occurred,Is_Completed\n")

        for (m in matches) {
            val dateStr = dateFormat.format(Date(m.timestamp))
            sb.append(escapeCsv(m.id.toString())).append(",")
            sb.append(escapeCsv(dateStr)).append(",")
            sb.append(escapeCsv(m.team1Name)).append(",")
            sb.append(escapeCsv(m.team2Name)).append(",")
            sb.append(escapeCsv(m.team1PlayerNames)).append(",")
            sb.append(escapeCsv(m.team2PlayerNames)).append(",")
            sb.append(m.team1FinalScore).append(",")
            sb.append(m.team2FinalScore).append(",")
            sb.append(escapeCsv(m.winnerTeamName ?: "Draw")).append(",")
            sb.append(m.targetPoints).append(",")
            sb.append(m.boardsCount).append(",")
            sb.append(m.handsCount).append(",")
            sb.append(if (m.proMode) "Pro" else "Standard").append(",")
            sb.append(escapeCsv(m.firstBreakerPlayerName)).append(",")
            sb.append(if (m.nillBoardOccurred) "Yes" else "No").append(",")
            sb.append(if (m.isCompleted) "Completed" else "Incomplete").append("\n")
        }

        return sb.toString()
    }

    /**
     * Exports detailed board-by-board breakdown of matches as CSV
     */
    fun exportBoardsBreakdownToCsv(matches: List<MatchEntity>): String {
        val sb = StringBuilder()
        sb.append("Match_ID,Date_Time,Match_Teams,Board_Number,Breaker_Player,Winning_Team,Board_Score,Opponent_Coins_Left,Queen_Winner,Queen_Points,Team_1_Score,Team_2_Score,Is_Nill_Board,Hands_Played,Turns_Played\n")

        for (m in matches) {
            val dateStr = dateFormat.format(Date(m.timestamp))
            val matchTeams = "${m.team1Name} vs ${m.team2Name}"
            val boards = CarromJsonParser.deserializeBoardRecords(m.boardDetailsJson)

            for (b in boards) {
                sb.append(escapeCsv(m.id.toString())).append(",")
                sb.append(escapeCsv(dateStr)).append(",")
                sb.append(escapeCsv(matchTeams)).append(",")
                sb.append(b.boardNumber).append(",")
                sb.append(escapeCsv(b.breakerPlayerName)).append(",")
                sb.append(escapeCsv(b.winningTeamName)).append(",")
                sb.append(b.boardScore).append(",")
                sb.append(b.opponentRemainingCoins).append(",")
                sb.append(escapeCsv(b.queenCoveredByPlayerName ?: "None")).append(",")
                sb.append(b.queenPointsAwarded).append(",")
                sb.append(b.team1ScoreAfterBoard).append(",")
                sb.append(b.team2ScoreAfterBoard).append(",")
                sb.append(if (b.isNillBoard) "Yes" else "No").append(",")
                sb.append(b.handsPlayed).append(",")
                sb.append(b.turnsPlayed).append("\n")
            }
        }

        return sb.toString()
    }

    /**
     * Exports player career statistics as CSV
     */
    fun exportPlayersToCsv(players: List<PlayerEntity>): String {
        val sb = StringBuilder()
        sb.append("Player_ID,Name,Nickname,Skill_Level,Matches_Played,Matches_Won,Matches_Lost,Win_Rate_Percent,Boards_Played,Boards_Won,Total_Coins_Pocketed,White_Coins,Black_Coins,Queen_Attempts,Queens_Covered,Queen_Success_Rate_Percent,Queen_Points_Scored,Penalties,Nill_Board_Wins,Nill_Board_Losses,Total_Points_Contributed,Notes,Created_Date\n")

        for (p in players) {
            val dateStr = dateFormat.format(Date(p.createdAt))
            sb.append(p.id).append(",")
            sb.append(escapeCsv(p.name)).append(",")
            sb.append(escapeCsv(p.nickname)).append(",")
            sb.append(escapeCsv(p.skillLevel)).append(",")
            sb.append(p.matchesPlayed).append(",")
            sb.append(p.matchesWon).append(",")
            sb.append(p.matchesLost).append(",")
            sb.append(String.format(Locale.US, "%.1f", p.winRate)).append(",")
            sb.append(p.boardsPlayed).append(",")
            sb.append(p.boardsWon).append(",")
            sb.append(p.totalCoinsPocketed).append(",")
            sb.append(p.whitePocketed).append(",")
            sb.append(p.blackPocketed).append(",")
            sb.append(p.queenAttempts).append(",")
            sb.append(p.queensCovered).append(",")
            sb.append(String.format(Locale.US, "%.1f", p.queenSuccessRate)).append(",")
            sb.append(p.queenPointsScored).append(",")
            sb.append(p.penalties).append(",")
            sb.append(p.nillBoardWins).append(",")
            sb.append(p.nillBoardLosses).append(",")
            sb.append(p.totalPointsContributed).append(",")
            sb.append(escapeCsv(p.notes)).append(",")
            sb.append(escapeCsv(dateStr)).append("\n")
        }

        return sb.toString()
    }

    /**
     * Exports full portable backup JSON including players and matches
     */
    fun exportFullBackupJson(players: List<PlayerEntity>, matches: List<MatchEntity>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "CarromScoreKeeper")
        root.put("exportedAt", System.currentTimeMillis())

        val playersArray = JSONArray()
        for (p in players) {
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("name", p.name)
            pObj.put("nickname", p.nickname)
            pObj.put("avatarColorIndex", p.avatarColorIndex)
            pObj.put("notes", p.notes)
            pObj.put("skillLevel", p.skillLevel)
            pObj.put("matchesPlayed", p.matchesPlayed)
            pObj.put("matchesWon", p.matchesWon)
            pObj.put("matchesLost", p.matchesLost)
            pObj.put("boardsPlayed", p.boardsPlayed)
            pObj.put("boardsWon", p.boardsWon)
            pObj.put("handsPlayed", p.handsPlayed)
            pObj.put("turnsPlayed", p.turnsPlayed)
            pObj.put("whitePocketed", p.whitePocketed)
            pObj.put("blackPocketed", p.blackPocketed)
            pObj.put("queenAttempts", p.queenAttempts)
            pObj.put("queensCovered", p.queensCovered)
            pObj.put("queenPointsScored", p.queenPointsScored)
            pObj.put("penalties", p.penalties)
            pObj.put("nillBoardWins", p.nillBoardWins)
            pObj.put("nillBoardLosses", p.nillBoardLosses)
            pObj.put("totalPointsContributed", p.totalPointsContributed)
            pObj.put("createdAt", p.createdAt)
            playersArray.put(pObj)
        }
        root.put("players", playersArray)

        val matchesArray = JSONArray()
        for (m in matches) {
            val mObj = JSONObject()
            mObj.put("id", m.id)
            mObj.put("team1Name", m.team1Name)
            mObj.put("team2Name", m.team2Name)
            mObj.put("team1PlayerNames", m.team1PlayerNames)
            mObj.put("team2PlayerNames", m.team2PlayerNames)
            mObj.put("firstBreakerPlayerId", m.firstBreakerPlayerId)
            mObj.put("firstBreakerPlayerName", m.firstBreakerPlayerName)
            mObj.put("proMode", m.proMode)
            mObj.put("team1FinalScore", m.team1FinalScore)
            mObj.put("team2FinalScore", m.team2FinalScore)
            if (m.winnerTeamId != null) mObj.put("winnerTeamId", m.winnerTeamId)
            if (m.winnerTeamName != null) mObj.put("winnerTeamName", m.winnerTeamName)
            mObj.put("boardsCount", m.boardsCount)
            mObj.put("handsCount", m.handsCount)
            mObj.put("targetPoints", m.targetPoints)
            mObj.put("nillBoardOccurred", m.nillBoardOccurred)
            mObj.put("boardDetailsJson", m.boardDetailsJson)
            mObj.put("turnLogsJson", m.turnLogsJson)
            mObj.put("isCompleted", m.isCompleted)
            mObj.put("timestamp", m.timestamp)
            matchesArray.put(mObj)
        }
        root.put("matches", matchesArray)

        return root.toString(2)
    }

    /**
     * Parses an imported CSV file or JSON string, extracting parsed players and matches
     */
    fun parseImportData(rawContent: String): ParsedImportResult {
        val trimmed = rawContent.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return parseJsonBackup(trimmed)
        }

        // CSV parsing
        val lines = splitCsvLines(trimmed)
        if (lines.isEmpty()) {
            return ParsedImportResult(emptyList(), emptyList(), "Empty file", ImportType.UNKNOWN)
        }

        val headerLine = lines.first()
        val headers = parseCsvRow(headerLine).map { it.trim().lowercase(Locale.ROOT) }

        if (headers.any { it.contains("match_id") || it.contains("team_1_name") || it.contains("team1name") }) {
            return parseMatchesCsv(lines)
        } else if (headers.any { it.contains("skill_level") || it.contains("matches_played") || it.contains("matchesplayed") || it.contains("white_coins") }) {
            return parsePlayersCsv(lines)
        }

        return ParsedImportResult(emptyList(), emptyList(), "Unrecognized CSV format", ImportType.UNKNOWN)
    }

    private fun parseJsonBackup(jsonStr: String): ParsedImportResult {
        try {
            val root = JSONObject(jsonStr)
            val parsedPlayers = mutableListOf<PlayerEntity>()
            val parsedMatches = mutableListOf<MatchEntity>()

            val pArr = root.optJSONArray("players") ?: JSONArray()
            for (i in 0 until pArr.length()) {
                val p = pArr.getJSONObject(i)
                parsedPlayers.add(
                    PlayerEntity(
                        id = p.optLong("id", 0L),
                        name = p.getString("name"),
                        nickname = p.optString("nickname", ""),
                        avatarColorIndex = p.optInt("avatarColorIndex", 0),
                        notes = p.optString("notes", ""),
                        skillLevel = p.optString("skillLevel", "Intermediate"),
                        matchesPlayed = p.optInt("matchesPlayed", 0),
                        matchesWon = p.optInt("matchesWon", 0),
                        matchesLost = p.optInt("matchesLost", 0),
                        boardsPlayed = p.optInt("boardsPlayed", 0),
                        boardsWon = p.optInt("boardsWon", 0),
                        handsPlayed = p.optInt("handsPlayed", 0),
                        turnsPlayed = p.optInt("turnsPlayed", 0),
                        whitePocketed = p.optInt("whitePocketed", 0),
                        blackPocketed = p.optInt("blackPocketed", 0),
                        queenAttempts = p.optInt("queenAttempts", 0),
                        queensCovered = p.optInt("queensCovered", 0),
                        queenPointsScored = p.optInt("queenPointsScored", 0),
                        penalties = p.optInt("penalties", 0),
                        nillBoardWins = p.optInt("nillBoardWins", 0),
                        nillBoardLosses = p.optInt("nillBoardLosses", 0),
                        totalPointsContributed = p.optInt("totalPointsContributed", 0),
                        createdAt = p.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val mArr = root.optJSONArray("matches") ?: JSONArray()
            for (i in 0 until mArr.length()) {
                val m = mArr.getJSONObject(i)
                parsedMatches.add(
                    MatchEntity(
                        id = m.optLong("id", System.currentTimeMillis() + i),
                        team1Name = m.optString("team1Name", "Team 1"),
                        team2Name = m.optString("team2Name", "Team 2"),
                        team1PlayerNames = m.optString("team1PlayerNames", ""),
                        team2PlayerNames = m.optString("team2PlayerNames", ""),
                        firstBreakerPlayerId = m.optLong("firstBreakerPlayerId", 0L),
                        firstBreakerPlayerName = m.optString("firstBreakerPlayerName", ""),
                        proMode = m.optBoolean("proMode", true),
                        team1FinalScore = m.optInt("team1FinalScore", 0),
                        team2FinalScore = m.optInt("team2FinalScore", 0),
                        winnerTeamId = if (m.has("winnerTeamId")) m.getInt("winnerTeamId") else null,
                        winnerTeamName = if (m.has("winnerTeamName")) m.getString("winnerTeamName") else null,
                        boardsCount = m.optInt("boardsCount", 0),
                        handsCount = m.optInt("handsCount", 0),
                        targetPoints = m.optInt("targetPoints", 29),
                        nillBoardOccurred = m.optBoolean("nillBoardOccurred", false),
                        boardDetailsJson = m.optString("boardDetailsJson", "[]"),
                        turnLogsJson = m.optString("turnLogsJson", "[]"),
                        isCompleted = m.optBoolean("isCompleted", true),
                        timestamp = m.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }

            return ParsedImportResult(
                players = parsedPlayers,
                matches = parsedMatches,
                summary = "Found ${parsedPlayers.size} players and ${parsedMatches.size} matches in JSON backup",
                importType = ImportType.FULL_JSON_BACKUP
            )
        } catch (e: Exception) {
            return ParsedImportResult(emptyList(), emptyList(), "Error parsing JSON backup: ${e.message}", ImportType.UNKNOWN)
        }
    }

    private fun parseMatchesCsv(lines: List<String>): ParsedImportResult {
        try {
            val matches = mutableListOf<MatchEntity>()
            val playersMap = mutableMapOf<String, PlayerEntity>()

            val headerRow = parseCsvRow(lines[0]).map { it.trim().lowercase(Locale.ROOT) }
            val colMap = headerRow.mapIndexed { idx, name -> name to idx }.toMap()

            for (i in 1 until lines.size) {
                val row = parseCsvRow(lines[i])
                if (row.isEmpty() || row.all { it.isBlank() }) continue

                val id = getCol(row, colMap, "match_id")?.toLongOrNull() ?: (System.currentTimeMillis() + i)
                val t1Name = getCol(row, colMap, "team_1_name") ?: "Team 1"
                val t2Name = getCol(row, colMap, "team_2_name") ?: "Team 2"
                val t1Players = getCol(row, colMap, "team_1_players") ?: ""
                val t2Players = getCol(row, colMap, "team_2_players") ?: ""
                val t1Score = getCol(row, colMap, "team_1_score")?.toIntOrNull() ?: 0
                val t2Score = getCol(row, colMap, "team_2_score")?.toIntOrNull() ?: 0
                val winnerName = getCol(row, colMap, "winner_team")
                val targetPts = getCol(row, colMap, "target_points")?.toIntOrNull() ?: 29
                val boardsCount = getCol(row, colMap, "boards_count")?.toIntOrNull() ?: 1
                val handsCount = getCol(row, colMap, "hands_count")?.toIntOrNull() ?: boardsCount
                val proMode = getCol(row, colMap, "pro_mode")?.equals("pro", ignoreCase = true) ?: true
                val breaker = getCol(row, colMap, "first_breaker") ?: ""
                val nillOccurred = getCol(row, colMap, "nill_board_occurred")?.equals("yes", ignoreCase = true) ?: false
                val isCompleted = getCol(row, colMap, "is_completed")?.equals("completed", ignoreCase = true) ?: true

                val dateStr = getCol(row, colMap, "date_time")
                val timestamp = if (dateStr != null) {
                    try { dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis() } catch (_: Exception) { System.currentTimeMillis() }
                } else System.currentTimeMillis()

                val winnerTeamId = when (winnerName) {
                    t1Name -> 1
                    t2Name -> 2
                    else -> null
                }

                matches.add(
                    MatchEntity(
                        id = id,
                        team1Name = t1Name,
                        team2Name = t2Name,
                        team1PlayerNames = t1Players,
                        team2PlayerNames = t2Players,
                        firstBreakerPlayerId = 0L,
                        firstBreakerPlayerName = breaker,
                        proMode = proMode,
                        team1FinalScore = t1Score,
                        team2FinalScore = t2Score,
                        winnerTeamId = winnerTeamId,
                        winnerTeamName = winnerName,
                        boardsCount = boardsCount,
                        handsCount = handsCount,
                        targetPoints = targetPts,
                        nillBoardOccurred = nillOccurred,
                        boardDetailsJson = "[]",
                        turnLogsJson = "[]",
                        isCompleted = isCompleted,
                        timestamp = timestamp
                    )
                )

                // Auto extract player names from match roster
                val allRoster = (t1Players.split(",") + t2Players.split(",")).map { it.trim() }.filter { it.isNotBlank() }
                for (pName in allRoster) {
                    if (!playersMap.containsKey(pName)) {
                        playersMap[pName] = PlayerEntity(
                            name = pName,
                            matchesPlayed = 1,
                            matchesWon = if (winnerName != null && ((t1Players.contains(pName) && winnerName == t1Name) || (t2Players.contains(pName) && winnerName == t2Name))) 1 else 0
                        )
                    } else {
                        val curr = playersMap[pName]!!
                        val won = if (winnerName != null && ((t1Players.contains(pName) && winnerName == t1Name) || (t2Players.contains(pName) && winnerName == t2Name))) 1 else 0
                        playersMap[pName] = curr.copy(
                            matchesPlayed = curr.matchesPlayed + 1,
                            matchesWon = curr.matchesWon + won
                        )
                    }
                }
            }

            return ParsedImportResult(
                players = playersMap.values.toList(),
                matches = matches,
                summary = "Found ${matches.size} matches and ${playersMap.size} unique players from CSV",
                importType = ImportType.MATCHES_CSV
            )
        } catch (e: Exception) {
            return ParsedImportResult(emptyList(), emptyList(), "Error parsing matches CSV: ${e.message}", ImportType.UNKNOWN)
        }
    }

    private fun parsePlayersCsv(lines: List<String>): ParsedImportResult {
        try {
            val players = mutableListOf<PlayerEntity>()
            val headerRow = parseCsvRow(lines[0]).map { it.trim().lowercase(Locale.ROOT) }
            val colMap = headerRow.mapIndexed { idx, name -> name to idx }.toMap()

            for (i in 1 until lines.size) {
                val row = parseCsvRow(lines[i])
                if (row.isEmpty() || row.all { it.isBlank() }) continue

                val name = getCol(row, colMap, "name") ?: continue
                if (name.isBlank()) continue

                val nickname = getCol(row, colMap, "nickname") ?: ""
                val skill = getCol(row, colMap, "skill_level") ?: "Intermediate"
                val matchesPlayed = getCol(row, colMap, "matches_played")?.toIntOrNull() ?: 0
                val matchesWon = getCol(row, colMap, "matches_won")?.toIntOrNull() ?: 0
                val matchesLost = getCol(row, colMap, "matches_lost")?.toIntOrNull() ?: 0
                val boardsPlayed = getCol(row, colMap, "boards_played")?.toIntOrNull() ?: 0
                val boardsWon = getCol(row, colMap, "boards_won")?.toIntOrNull() ?: 0
                val whiteCoins = getCol(row, colMap, "white_coins")?.toIntOrNull() ?: 0
                val blackCoins = getCol(row, colMap, "black_coins")?.toIntOrNull() ?: 0
                val queenAttempts = getCol(row, colMap, "queen_attempts")?.toIntOrNull() ?: 0
                val queensCovered = getCol(row, colMap, "queens_covered")?.toIntOrNull() ?: 0
                val queenPoints = getCol(row, colMap, "queen_points_scored")?.toIntOrNull() ?: 0
                val penalties = getCol(row, colMap, "penalties")?.toIntOrNull() ?: 0
                val nillWins = getCol(row, colMap, "nill_board_wins")?.toIntOrNull() ?: 0
                val nillLosses = getCol(row, colMap, "nill_board_losses")?.toIntOrNull() ?: 0
                val totalPoints = getCol(row, colMap, "total_points_contributed")?.toIntOrNull() ?: 0
                val notes = getCol(row, colMap, "notes") ?: ""

                players.add(
                    PlayerEntity(
                        name = name,
                        nickname = nickname,
                        skillLevel = skill,
                        matchesPlayed = matchesPlayed,
                        matchesWon = matchesWon,
                        matchesLost = matchesLost,
                        boardsPlayed = boardsPlayed,
                        boardsWon = boardsWon,
                        whitePocketed = whiteCoins,
                        blackPocketed = blackCoins,
                        queenAttempts = queenAttempts,
                        queensCovered = queensCovered,
                        queenPointsScored = queenPoints,
                        penalties = penalties,
                        nillBoardWins = nillWins,
                        nillBoardLosses = nillLosses,
                        totalPointsContributed = totalPoints,
                        notes = notes
                    )
                )
            }

            return ParsedImportResult(
                players = players,
                matches = emptyList(),
                summary = "Found ${players.size} player performance profiles from CSV",
                importType = ImportType.PLAYERS_CSV
            )
        } catch (e: Exception) {
            return ParsedImportResult(emptyList(), emptyList(), "Error parsing players CSV: ${e.message}", ImportType.UNKNOWN)
        }
    }

    private fun getCol(row: List<String>, colMap: Map<String, Int>, key: String): String? {
        val idx = colMap[key] ?: return null
        return if (idx < row.size) row[idx].trim() else null
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun splitCsvLines(content: String): List<String> {
        val lines = mutableListOf<String>()
        val currentLine = StringBuilder()
        var insideQuotes = false

        for (ch in content) {
            if (ch == '\"') {
                insideQuotes = !insideQuotes
                currentLine.append(ch)
            } else if (ch == '\n' && !insideQuotes) {
                lines.add(currentLine.toString())
                currentLine.clear()
            } else if (ch != '\r') {
                currentLine.append(ch)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    private fun parseCsvRow(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++ // skip escaped quote
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString())
        return tokens
    }
}

enum class ImportType {
    FULL_JSON_BACKUP,
    MATCHES_CSV,
    PLAYERS_CSV,
    UNKNOWN
}

data class ParsedImportResult(
    val players: List<PlayerEntity>,
    val matches: List<MatchEntity>,
    val summary: String,
    val importType: ImportType
)
