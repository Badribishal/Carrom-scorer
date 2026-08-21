package com.example.carrom.engine

enum class TeamColor {
    WHITE,
    BLACK;

    val displayName: String
        get() = when (this) {
            WHITE -> "White"
            BLACK -> "Black"
        }
}

enum class QueenStatus {
    AVAILABLE,
    PENDING_COVER,
    COVERED;

    val displayName: String
        get() = when (this) {
            AVAILABLE -> "Available"
            PENDING_COVER -> "Pending Cover"
            COVERED -> "Covered"
        }
}

data class Player(
    val id: Long = 0L,
    val name: String,
    val avatarColorIndex: Int = 0
)

data class MatchConfig(
    val team1Name: String,
    val team2Name: String,
    val team1Players: List<Player>,
    val team2Players: List<Player>,
    val firstBreakerPlayerId: Long,
    val proMode: Boolean = true,
    val targetPoints: Int = 29,
    val nillBoardThreshold: Int = 7,
    val queenPoints: Int = 5,
    val enable24PlusQueenRule: Boolean = true
) {
    val isDoubles: Boolean
        get() = team1Players.size >= 2 && team2Players.size >= 2

    val breakingTeamId: Int
        get() {
            if (team1Players.any { it.id == firstBreakerPlayerId || it.name.equals(team1Players.firstOrNull()?.name, ignoreCase = true) }) return 1
            if (team2Players.any { it.id == firstBreakerPlayerId || it.name.equals(team2Players.firstOrNull()?.name, ignoreCase = true) }) return 2
            return 1
        }

    fun getTeamColor(teamId: Int): TeamColor {
        return if (teamId == breakingTeamId) TeamColor.WHITE else TeamColor.BLACK
    }

    fun getPlayerTeamId(playerId: Long): Int {
        if (team1Players.any { it.id == playerId }) return 1
        if (team2Players.any { it.id == playerId }) return 2
        return 1
    }

    fun getPlayerTeamId(player: Player): Int {
        if (team1Players.any { it.id == player.id || it.name.equals(player.name, ignoreCase = true) }) return 1
        if (team2Players.any { it.id == player.id || it.name.equals(player.name, ignoreCase = true) }) return 2
        return 1
    }

    /**
     * Build the turn rotation order starting from the breaker and alternating teams.
     */
    fun buildRotationOrder(): List<Player> {
        if (!isDoubles) {
            val t1p = team1Players.firstOrNull() ?: Player(1, "Player 1")
            val t2p = team2Players.firstOrNull() ?: Player(2, "Player 2")
            return if (breakingTeamId == 1) listOf(t1p, t2p) else listOf(t2p, t1p)
        }

        val t1p1 = team1Players.getOrNull(0) ?: Player(1, "Player 1")
        val t1p2 = team1Players.getOrNull(1) ?: Player(2, "Player 2")
        val t2p1 = team2Players.getOrNull(0) ?: Player(3, "Player 3")
        val t2p2 = team2Players.getOrNull(1) ?: Player(4, "Player 4")

        return if (breakingTeamId == 1) {
            if (firstBreakerPlayerId == t1p1.id || team1Players.firstOrNull()?.id == firstBreakerPlayerId) {
                listOf(t1p1, t2p1, t1p2, t2p2)
            } else {
                listOf(t1p2, t2p1, t1p1, t2p2)
            }
        } else {
            if (firstBreakerPlayerId == t2p1.id || team2Players.firstOrNull()?.id == firstBreakerPlayerId) {
                listOf(t2p1, t1p1, t2p2, t1p2)
            } else {
                listOf(t2p2, t1p1, t2p1, t1p2)
            }
        }
    }
}

data class TurnActionSnapshot(
    val actionType: ActionType,
    val boardStateBefore: BoardLiveState,
    val turnWhiteBefore: Int,
    val turnBlackBefore: Int,
    val turnQueenPocketedBefore: Boolean,
    val turnQueenCoveredBefore: Boolean,
    val turnPenaltiesBefore: Int
)

enum class ActionType {
    POCKET_WHITE,
    POCKET_BLACK,
    POCKET_QUEEN,
    RECORD_PENALTY
}

data class TurnRecord(
    val turnNumber: Int,
    val handNumber: Int,
    val playerId: Long,
    val playerName: String,
    val teamId: Int,
    val teamColor: TeamColor,
    val whitePocketed: Int,
    val blackPocketed: Int,
    val queenPocketed: Boolean,
    val queenCovered: Boolean,
    val penalties: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class BoardRecord(
    val boardNumber: Int,
    val breakerPlayerId: Long,
    val breakerPlayerName: String,
    val winningTeamId: Int,
    val winningTeamName: String,
    val whiteRemaining: Int,
    val blackRemaining: Int,
    val opponentRemainingCoins: Int,
    val queenCoveredByPlayerId: Long?,
    val queenCoveredByPlayerName: String?,
    val queenCoveredByTeamId: Int?,
    val queenPointsAwarded: Int,
    val boardScore: Int,
    val isNillBoard: Boolean,
    val team1ScoreAfterBoard: Int,
    val team2ScoreAfterBoard: Int,
    val handsPlayed: Int,
    val turnsPlayed: Int
)

data class BoardLiveState(
    val boardNumber: Int = 1,
    val whiteRemaining: Int = 9,
    val blackRemaining: Int = 9,
    val queenStatus: QueenStatus = QueenStatus.AVAILABLE,
    val queenPocketedByPlayerId: Long? = null,
    val queenPocketedByTeamId: Int? = null,
    val queenCoveredByPlayerId: Long? = null,
    val queenCoveredByTeamId: Int? = null,
    val isCompleted: Boolean = false,
    val winnerTeamId: Int? = null,
    val boardScore: Int = 0,
    val isNillBoard: Boolean = false
)

data class TurnLiveState(
    val currentHand: Int = 1,
    val currentTurnIndexInRotation: Int = 0,
    val currentOverallTurnNumber: Int = 1,
    val currentTurnWhite: Int = 0,
    val currentTurnBlack: Int = 0,
    val currentTurnQueenPocketed: Boolean = false,
    val currentTurnQueenCovered: Boolean = false,
    val currentTurnPenalties: Int = 0,
    val undoStack: List<TurnActionSnapshot> = emptyList()
)

data class GameState(
    val matchId: Long = System.currentTimeMillis(),
    val config: MatchConfig,
    val team1Score: Int = 0,
    val team2Score: Int = 0,
    val currentBoardNumber: Int = 1,
    val boardState: BoardLiveState = BoardLiveState(boardNumber = 1),
    val turnState: TurnLiveState = TurnLiveState(),
    val completedBoards: List<BoardRecord> = emptyList(),
    val allTurnLogs: List<TurnRecord> = emptyList(),
    val isMatchOver: Boolean = false,
    val matchWinnerTeamId: Int? = null,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val currentBoardResultDialog: BoardRecord? = null
) {
    val rotationOrder: List<Player> = config.buildRotationOrder()

    /**
     * Returns the Player who has the Break for the given board number (1-based).
     */
    fun getBreakerForBoard(boardNum: Int = currentBoardNumber): Player {
        val order = rotationOrder
        if (order.isEmpty()) return Player(1, "Player 1")
        val idx = ((boardNum - 1) % order.size + order.size) % order.size
        return order[idx]
    }

    val currentBoardBreaker: Player
        get() = getBreakerForBoard(currentBoardNumber)

    /**
     * Returns the Team ID (1 or 2) that has the Break for the given board number.
     */
    fun getBreakingTeamIdForBoard(boardNum: Int = currentBoardNumber): Int {
        return config.getPlayerTeamId(getBreakerForBoard(boardNum))
    }

    val currentBoardBreakingTeamId: Int
        get() = getBreakingTeamIdForBoard(currentBoardNumber)

    /**
     * In Carrom, whichever player/team breaks a given board plays with WHITE coins (9 coins).
     * The opponent team plays with BLACK coins (9 coins).
     * On alternating boards, the break rotates, so the other team/player breaks and plays White.
     */
    fun getTeamColorForBoard(teamId: Int, boardNum: Int = currentBoardNumber): TeamColor {
        val breakerTeamId = getBreakingTeamIdForBoard(boardNum)
        return if (teamId == breakerTeamId) TeamColor.WHITE else TeamColor.BLACK
    }

    val currentPlayer: Player
        get() {
            val order = rotationOrder
            if (order.isEmpty()) return Player(1, "Player 1")
            val index = ((turnState.currentTurnIndexInRotation % order.size) + order.size) % order.size
            return order[index]
        }

    val nextPlayer: Player
        get() {
            val order = rotationOrder
            if (order.isEmpty()) return Player(1, "Player 1")
            val nextIndex = (((turnState.currentTurnIndexInRotation + 1) % order.size) + order.size) % order.size
            return order[nextIndex]
        }

    val currentTeamId: Int
        get() = config.getPlayerTeamId(currentPlayer)

    val nextTeamId: Int
        get() = config.getPlayerTeamId(nextPlayer)

    val currentTeamName: String
        get() = if (currentTeamId == 1) config.team1Name else config.team2Name

    val nextTeamName: String
        get() = if (nextTeamId == 1) config.team1Name else config.team2Name

    val currentTeamColor: TeamColor
        get() = getTeamColorForBoard(currentTeamId, currentBoardNumber)

    val nextTeamColor: TeamColor
        get() = getTeamColorForBoard(nextTeamId, currentBoardNumber)
}
