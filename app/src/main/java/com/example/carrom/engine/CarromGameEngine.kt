package com.example.carrom.engine

class CarromGameEngine(
    initialState: GameState
) {
    private var _state: GameState = initialState
    val state: GameState get() = _state

    fun pocketWhite(): GameState {
        if (_state.isMatchOver || _state.boardState.isCompleted) return _state
        if (_state.boardState.whiteRemaining <= 0) return _state

        val currentTurn = _state.turnState
        val currentBoard = _state.boardState
        val currentPlayer = _state.currentPlayer
        val playerColor = _state.currentTeamColor

        // Save snapshot for Undo
        val snapshot = TurnActionSnapshot(
            actionType = ActionType.POCKET_WHITE,
            boardStateBefore = currentBoard,
            turnWhiteBefore = currentTurn.currentTurnWhite,
            turnBlackBefore = currentTurn.currentTurnBlack,
            turnQueenPocketedBefore = currentTurn.currentTurnQueenPocketed,
            turnQueenCoveredBefore = currentTurn.currentTurnQueenCovered,
            turnPenaltiesBefore = currentTurn.currentTurnPenalties
        )

        val newWhiteRemaining = currentBoard.whiteRemaining - 1
        var newQueenStatus = currentBoard.queenStatus
        var queenCoveredByPlayerId = currentBoard.queenCoveredByPlayerId
        var queenCoveredByTeamId = currentBoard.queenCoveredByTeamId
        var isTurnQueenCovered = currentTurn.currentTurnQueenCovered

        // Check if this white coin covers the Queen for White team
        if (playerColor == TeamColor.WHITE &&
            currentBoard.queenStatus == QueenStatus.PENDING_COVER &&
            currentBoard.queenPocketedByPlayerId == currentPlayer.id
        ) {
            newQueenStatus = QueenStatus.COVERED
            queenCoveredByPlayerId = currentPlayer.id
            queenCoveredByTeamId = _state.currentTeamId
            isTurnQueenCovered = true
        }

        val updatedBoard = currentBoard.copy(
            whiteRemaining = newWhiteRemaining,
            queenStatus = newQueenStatus,
            queenCoveredByPlayerId = queenCoveredByPlayerId,
            queenCoveredByTeamId = queenCoveredByTeamId
        )

        val updatedTurn = currentTurn.copy(
            currentTurnWhite = currentTurn.currentTurnWhite + 1,
            currentTurnQueenCovered = isTurnQueenCovered,
            undoStack = currentTurn.undoStack + snapshot
        )

        _state = _state.copy(
            boardState = updatedBoard,
            turnState = updatedTurn
        )

        checkBoardCompletion()
        return _state
    }

    fun pocketBlack(): GameState {
        if (_state.isMatchOver || _state.boardState.isCompleted) return _state
        if (_state.boardState.blackRemaining <= 0) return _state

        val currentTurn = _state.turnState
        val currentBoard = _state.boardState
        val currentPlayer = _state.currentPlayer
        val playerColor = _state.currentTeamColor

        val snapshot = TurnActionSnapshot(
            actionType = ActionType.POCKET_BLACK,
            boardStateBefore = currentBoard,
            turnWhiteBefore = currentTurn.currentTurnWhite,
            turnBlackBefore = currentTurn.currentTurnBlack,
            turnQueenPocketedBefore = currentTurn.currentTurnQueenPocketed,
            turnQueenCoveredBefore = currentTurn.currentTurnQueenCovered,
            turnPenaltiesBefore = currentTurn.currentTurnPenalties
        )

        val newBlackRemaining = currentBoard.blackRemaining - 1
        var newQueenStatus = currentBoard.queenStatus
        var queenCoveredByPlayerId = currentBoard.queenCoveredByPlayerId
        var queenCoveredByTeamId = currentBoard.queenCoveredByTeamId
        var isTurnQueenCovered = currentTurn.currentTurnQueenCovered

        // Check if this black coin covers the Queen for Black team
        if (playerColor == TeamColor.BLACK &&
            currentBoard.queenStatus == QueenStatus.PENDING_COVER &&
            currentBoard.queenPocketedByPlayerId == currentPlayer.id
        ) {
            newQueenStatus = QueenStatus.COVERED
            queenCoveredByPlayerId = currentPlayer.id
            queenCoveredByTeamId = _state.currentTeamId
            isTurnQueenCovered = true
        }

        val updatedBoard = currentBoard.copy(
            blackRemaining = newBlackRemaining,
            queenStatus = newQueenStatus,
            queenCoveredByPlayerId = queenCoveredByPlayerId,
            queenCoveredByTeamId = queenCoveredByTeamId
        )

        val updatedTurn = currentTurn.copy(
            currentTurnBlack = currentTurn.currentTurnBlack + 1,
            currentTurnQueenCovered = isTurnQueenCovered,
            undoStack = currentTurn.undoStack + snapshot
        )

        _state = _state.copy(
            boardState = updatedBoard,
            turnState = updatedTurn
        )

        checkBoardCompletion()
        return _state
    }

    fun pocketQueen(): GameState {
        if (_state.isMatchOver || _state.boardState.isCompleted) return _state
        // Queen can only be pocketed when AVAILABLE
        if (_state.boardState.queenStatus != QueenStatus.AVAILABLE) return _state

        val currentTurn = _state.turnState
        val currentBoard = _state.boardState
        val currentPlayer = _state.currentPlayer

        val snapshot = TurnActionSnapshot(
            actionType = ActionType.POCKET_QUEEN,
            boardStateBefore = currentBoard,
            turnWhiteBefore = currentTurn.currentTurnWhite,
            turnBlackBefore = currentTurn.currentTurnBlack,
            turnQueenPocketedBefore = currentTurn.currentTurnQueenPocketed,
            turnQueenCoveredBefore = currentTurn.currentTurnQueenCovered,
            turnPenaltiesBefore = currentTurn.currentTurnPenalties
        )

        val updatedBoard = currentBoard.copy(
            queenStatus = QueenStatus.PENDING_COVER,
            queenPocketedByPlayerId = currentPlayer.id,
            queenPocketedByTeamId = _state.currentTeamId
        )

        val updatedTurn = currentTurn.copy(
            currentTurnQueenPocketed = true,
            undoStack = currentTurn.undoStack + snapshot
        )

        _state = _state.copy(
            boardState = updatedBoard,
            turnState = updatedTurn
        )

        return _state
    }

    fun recordPenalty(): GameState {
        if (_state.isMatchOver || _state.boardState.isCompleted) return _state

        val currentTurn = _state.turnState
        val currentBoard = _state.boardState

        val snapshot = TurnActionSnapshot(
            actionType = ActionType.RECORD_PENALTY,
            boardStateBefore = currentBoard,
            turnWhiteBefore = currentTurn.currentTurnWhite,
            turnBlackBefore = currentTurn.currentTurnBlack,
            turnQueenPocketedBefore = currentTurn.currentTurnQueenPocketed,
            turnQueenCoveredBefore = currentTurn.currentTurnQueenCovered,
            turnPenaltiesBefore = currentTurn.currentTurnPenalties
        )

        val updatedTurn = currentTurn.copy(
            currentTurnPenalties = currentTurn.currentTurnPenalties + 1,
            undoStack = currentTurn.undoStack + snapshot
        )

        _state = _state.copy(
            turnState = updatedTurn
        )

        return _state
    }

    fun undo(): GameState {
        val stack = _state.turnState.undoStack
        if (stack.isEmpty()) return _state

        val lastSnapshot = stack.last()
        val remainingStack = stack.dropLast(1)

        val restoredTurn = _state.turnState.copy(
            currentTurnWhite = lastSnapshot.turnWhiteBefore,
            currentTurnBlack = lastSnapshot.turnBlackBefore,
            currentTurnQueenPocketed = lastSnapshot.turnQueenPocketedBefore,
            currentTurnQueenCovered = lastSnapshot.turnQueenCoveredBefore,
            currentTurnPenalties = lastSnapshot.turnPenaltiesBefore,
            undoStack = remainingStack
        )

        _state = _state.copy(
            boardState = lastSnapshot.boardStateBefore,
            turnState = restoredTurn,
            isMatchOver = false,
            currentBoardResultDialog = null
        )

        return _state
    }

    fun endTurn(): GameState {
        if (_state.isMatchOver || _state.boardState.isCompleted) return _state

        val currentTurn = _state.turnState
        var currentBoard = _state.boardState
        val currentPlayer = _state.currentPlayer
        val teamId = _state.currentTeamId
        val teamColor = _state.currentTeamColor

        // 1. Resolve Queen if pending cover: returns to board as AVAILABLE
        if (currentBoard.queenStatus == QueenStatus.PENDING_COVER) {
            currentBoard = currentBoard.copy(
                queenStatus = QueenStatus.AVAILABLE,
                queenPocketedByPlayerId = null,
                queenPocketedByTeamId = null
            )
        }

        // 2. Create TurnRecord log
        val turnRecord = TurnRecord(
            turnNumber = currentTurn.currentOverallTurnNumber,
            handNumber = currentTurn.currentHand,
            playerId = currentPlayer.id,
            playerName = currentPlayer.name,
            teamId = teamId,
            teamColor = teamColor,
            whitePocketed = currentTurn.currentTurnWhite,
            blackPocketed = currentTurn.currentTurnBlack,
            queenPocketed = currentTurn.currentTurnQueenPocketed,
            queenCovered = currentTurn.currentTurnQueenCovered,
            penalties = currentTurn.currentTurnPenalties,
            timestamp = System.currentTimeMillis()
        )

        // 3. Advance rotation and hand
        val order = _state.rotationOrder
        val nextRotationIndex = (currentTurn.currentTurnIndexInRotation + 1) % order.size
        val nextHand = if (nextRotationIndex == 0) currentTurn.currentHand + 1 else currentTurn.currentHand
        val nextOverallTurn = currentTurn.currentOverallTurnNumber + 1

        val nextTurnState = TurnLiveState(
            currentHand = nextHand,
            currentTurnIndexInRotation = nextRotationIndex,
            currentOverallTurnNumber = nextOverallTurn,
            currentTurnWhite = 0,
            currentTurnBlack = 0,
            currentTurnQueenPocketed = false,
            currentTurnQueenCovered = false,
            currentTurnPenalties = 0,
            undoStack = emptyList()
        )

        _state = _state.copy(
            boardState = currentBoard,
            turnState = nextTurnState,
            allTurnLogs = _state.allTurnLogs + turnRecord
        )

        return _state
    }

    private fun checkBoardCompletion() {
        val board = _state.boardState
        val whiteCleared = board.whiteRemaining == 0
        val blackCleared = board.blackRemaining == 0

        if (!whiteCleared && !blackCleared) return

        // Board is completed!
        val team1Color = _state.getTeamColorForBoard(1, _state.currentBoardNumber)
        val team2Color = _state.getTeamColorForBoard(2, _state.currentBoardNumber)

        val winningColor = if (whiteCleared) TeamColor.WHITE else TeamColor.BLACK
        val winningTeamId = if (team1Color == winningColor) 1 else 2
        val winningTeamName = if (winningTeamId == 1) _state.config.team1Name else _state.config.team2Name
        val opponentTeamId = if (winningTeamId == 1) 2 else 1

        val opponentRemainingCoins = if (winningColor == TeamColor.WHITE) board.blackRemaining else board.whiteRemaining

        // Check Queen scoring
        var queenPointsAwarded = 0
        val queenCoveredByTeam = board.queenCoveredByTeamId
        val queenCoveredByPlayer = board.queenCoveredByPlayerId
        val queenCoveredPlayerName = _state.rotationOrder.find { it.id == queenCoveredByPlayer }?.name

        if (queenCoveredByTeam == winningTeamId && board.queenStatus == QueenStatus.COVERED) {
            val teamScoreBeforeBoard = if (winningTeamId == 1) _state.team1Score else _state.team2Score
            // 19-Point Rule (Official ICF Rule): Once a team reaches or crosses 19 (or configured threshold) points, no premium Queen points are credited to their score.
            val threshold = _state.config.queenStopThreshold
            if (_state.config.enableQueenStopRule && teamScoreBeforeBoard >= threshold) {
                queenPointsAwarded = 0
            } else {
                queenPointsAwarded = _state.config.queenPoints
            }
        }

        val boardScore = opponentRemainingCoins + queenPointsAwarded

        val newTeam1Score = if (winningTeamId == 1) _state.team1Score + boardScore else _state.team1Score
        val newTeam2Score = if (winningTeamId == 2) _state.team2Score + boardScore else _state.team2Score

        // Nill Board rule: if losing team score < threshold (7 points)
        val losingTeamScore = if (winningTeamId == 1) _state.team2Score else _state.team1Score
        val isNillBoard = losingTeamScore < _state.config.nillBoardThreshold

        // Match victory conditions:
        // 1. Target points reached (e.g. 29 points or 25 points)
        // 2. Nill Board Victory (19+ vs <7 Rule): If a team reaches or scores 19 or higher (queenStopThreshold)
        //    while the opponent fails to reach at least 7 points (nillBoardThreshold), that team wins the match immediately.
        val team1NillWin = newTeam1Score >= _state.config.queenStopThreshold && newTeam2Score < _state.config.nillBoardThreshold
        val team2NillWin = newTeam2Score >= _state.config.queenStopThreshold && newTeam1Score < _state.config.nillBoardThreshold

        val team1TargetWin = newTeam1Score >= _state.config.targetPoints
        val team2TargetWin = newTeam2Score >= _state.config.targetPoints

        val isMatchWon = team1TargetWin || team2TargetWin || team1NillWin || team2NillWin
        val matchWinnerId = when {
            team1TargetWin || team1NillWin -> 1
            team2TargetWin || team2NillWin -> 2
            else -> null
        }
        val isWonByNillRule = (team1NillWin && matchWinnerId == 1) || (team2NillWin && matchWinnerId == 2)

        val currentBreaker = _state.getBreakerForBoard(_state.currentBoardNumber)
        val boardRecord = BoardRecord(
            boardNumber = _state.currentBoardNumber,
            breakerPlayerId = currentBreaker.id,
            breakerPlayerName = currentBreaker.name,
            winningTeamId = winningTeamId,
            winningTeamName = winningTeamName,
            whiteRemaining = board.whiteRemaining,
            blackRemaining = board.blackRemaining,
            opponentRemainingCoins = opponentRemainingCoins,
            queenCoveredByPlayerId = queenCoveredByPlayer,
            queenCoveredByPlayerName = queenCoveredPlayerName,
            queenCoveredByTeamId = queenCoveredByTeam,
            queenPointsAwarded = queenPointsAwarded,
            boardScore = boardScore,
            isNillBoard = isNillBoard,
            isNillMatchWin = isWonByNillRule,
            team1ScoreAfterBoard = newTeam1Score,
            team2ScoreAfterBoard = newTeam2Score,
            handsPlayed = _state.turnState.currentHand,
            turnsPlayed = _state.turnState.currentOverallTurnNumber
        )

        val completedBoardState = board.copy(
            isCompleted = true,
            winnerTeamId = winningTeamId,
            boardScore = boardScore,
            isNillBoard = isNillBoard
        )

        _state = _state.copy(
            team1Score = newTeam1Score,
            team2Score = newTeam2Score,
            boardState = completedBoardState,
            completedBoards = _state.completedBoards + boardRecord,
            isMatchOver = isMatchWon,
            matchWinnerTeamId = matchWinnerId,
            isWonByNillRule = isWonByNillRule,
            endTime = if (isMatchWon) System.currentTimeMillis() else null,
            currentBoardResultDialog = boardRecord
        )
    }

    fun dismissBoardResultDialog(): GameState {
        _state = _state.copy(currentBoardResultDialog = null)
        return _state
    }

    fun startNextBoard(): GameState {
        if (_state.isMatchOver) return _state

        val nextBoardNum = _state.currentBoardNumber + 1
        val nextBoardState = BoardLiveState(
            boardNumber = nextBoardNum,
            whiteRemaining = 9,
            blackRemaining = 9,
            queenStatus = QueenStatus.AVAILABLE,
            queenPocketedByPlayerId = null,
            queenPocketedByTeamId = null,
            queenCoveredByPlayerId = null,
            queenCoveredByTeamId = null,
            isCompleted = false,
            winnerTeamId = null,
            boardScore = 0,
            isNillBoard = false
        )

        // Starting breaker rotates each board in carrom match
        val order = _state.rotationOrder
        val startRotationIndex = (nextBoardNum - 1) % order.size

        val nextTurnState = TurnLiveState(
            currentHand = 1,
            currentTurnIndexInRotation = startRotationIndex,
            currentOverallTurnNumber = 1,
            currentTurnWhite = 0,
            currentTurnBlack = 0,
            currentTurnQueenPocketed = false,
            currentTurnQueenCovered = false,
            currentTurnPenalties = 0,
            undoStack = emptyList()
        )

        _state = _state.copy(
            currentBoardNumber = nextBoardNum,
            boardState = nextBoardState,
            turnState = nextTurnState,
            currentBoardResultDialog = null
        )

        return _state
    }

    /**
     * Records a completed board directly in Simplified Scoreboard mode.
     * Uses the exact same ICF scoring rules (Opponent remaining coins + Queen points with 19-point cutoff).
     */
    fun recordSimplifiedBoard(
        winningTeamId: Int,
        opponentRemainingCoins: Int,
        queenCoveredByPlayerId: Long?,
        queenCoveredByTeamId: Int?
    ): GameState {
        if (_state.isMatchOver) return _state

        val currentBoardNum = _state.currentBoardNumber
        val winningTeamName = if (winningTeamId == 1) _state.config.team1Name else _state.config.team2Name
        val currentBreaker = _state.getBreakerForBoard(currentBoardNum)
        val queenCoveredPlayerName = _state.rotationOrder.find { it.id == queenCoveredByPlayerId }?.name

        var queenPointsAwarded = 0
        if (queenCoveredByTeamId == winningTeamId && queenCoveredByPlayerId != null) {
            val teamScoreBeforeBoard = if (winningTeamId == 1) _state.team1Score else _state.team2Score
            val threshold = _state.config.queenStopThreshold
            if (_state.config.enableQueenStopRule && teamScoreBeforeBoard >= threshold) {
                queenPointsAwarded = 0
            } else {
                queenPointsAwarded = _state.config.queenPoints
            }
        }

        val boardScore = opponentRemainingCoins + queenPointsAwarded
        val newTeam1Score = if (winningTeamId == 1) _state.team1Score + boardScore else _state.team1Score
        val newTeam2Score = if (winningTeamId == 2) _state.team2Score + boardScore else _state.team2Score

        val losingTeamScore = if (winningTeamId == 1) _state.team2Score else _state.team1Score
        val isNillBoard = losingTeamScore < _state.config.nillBoardThreshold

        val team1NillWin = newTeam1Score >= _state.config.queenStopThreshold && newTeam2Score < _state.config.nillBoardThreshold
        val team2NillWin = newTeam2Score >= _state.config.queenStopThreshold && newTeam1Score < _state.config.nillBoardThreshold
        val team1TargetWin = newTeam1Score >= _state.config.targetPoints
        val team2TargetWin = newTeam2Score >= _state.config.targetPoints

        val isMatchWon = team1TargetWin || team2TargetWin || team1NillWin || team2NillWin
        val matchWinnerId = when {
            team1TargetWin || team1NillWin -> 1
            team2TargetWin || team2NillWin -> 2
            else -> null
        }
        val isWonByNillRule = (team1NillWin && matchWinnerId == 1) || (team2NillWin && matchWinnerId == 2)

        val boardRecord = BoardRecord(
            boardNumber = currentBoardNum,
            breakerPlayerId = currentBreaker.id,
            breakerPlayerName = currentBreaker.name,
            winningTeamId = winningTeamId,
            winningTeamName = winningTeamName,
            whiteRemaining = if (winningTeamId == 1) 0 else opponentRemainingCoins,
            blackRemaining = if (winningTeamId == 2) 0 else opponentRemainingCoins,
            opponentRemainingCoins = opponentRemainingCoins,
            queenCoveredByPlayerId = queenCoveredByPlayerId,
            queenCoveredByPlayerName = queenCoveredPlayerName,
            queenCoveredByTeamId = queenCoveredByTeamId,
            queenPointsAwarded = queenPointsAwarded,
            boardScore = boardScore,
            isNillBoard = isNillBoard,
            isNillMatchWin = isWonByNillRule,
            team1ScoreAfterBoard = newTeam1Score,
            team2ScoreAfterBoard = newTeam2Score,
            handsPlayed = 1,
            turnsPlayed = 1
        )

        // Add synthetic turn log for player stats persistence if queen was potted
        val newTurnLogs = if (queenCoveredByPlayerId != null && queenCoveredByTeamId != null) {
            _state.allTurnLogs + TurnRecord(
                turnNumber = _state.completedBoards.size + 1,
                handNumber = 1,
                playerId = queenCoveredByPlayerId,
                playerName = queenCoveredPlayerName ?: "Player",
                teamId = queenCoveredByTeamId,
                teamColor = _state.getTeamColorForBoard(queenCoveredByTeamId, currentBoardNum),
                whitePocketed = if (_state.getTeamColorForBoard(queenCoveredByTeamId, currentBoardNum) == TeamColor.WHITE) (9 - opponentRemainingCoins) else 0,
                blackPocketed = if (_state.getTeamColorForBoard(queenCoveredByTeamId, currentBoardNum) == TeamColor.BLACK) (9 - opponentRemainingCoins) else 0,
                queenPocketed = true,
                queenCovered = true,
                penalties = 0
            )
        } else {
            _state.allTurnLogs
        }

        if (isMatchWon) {
            _state = _state.copy(
                team1Score = newTeam1Score,
                team2Score = newTeam2Score,
                completedBoards = _state.completedBoards + boardRecord,
                allTurnLogs = newTurnLogs,
                isMatchOver = true,
                matchWinnerTeamId = matchWinnerId,
                isWonByNillRule = isWonByNillRule,
                endTime = System.currentTimeMillis(),
                currentBoardResultDialog = null
            )
        } else {
            val nextBoardNum = currentBoardNum + 1
            val nextBoardState = BoardLiveState(
                boardNumber = nextBoardNum,
                whiteRemaining = 9,
                blackRemaining = 9,
                queenStatus = QueenStatus.AVAILABLE
            )
            _state = _state.copy(
                currentBoardNumber = nextBoardNum,
                team1Score = newTeam1Score,
                team2Score = newTeam2Score,
                boardState = nextBoardState,
                completedBoards = _state.completedBoards + boardRecord,
                allTurnLogs = newTurnLogs,
                isMatchOver = false,
                matchWinnerTeamId = null,
                currentBoardResultDialog = null
            )
        }

        return _state
    }

    /**
     * Undoes the last completed board in simplified mode.
     */
    fun undoLastBoard(): GameState {
        if (_state.completedBoards.isEmpty()) return _state

        val updatedBoards = _state.completedBoards.dropLast(1)
        val lastBoard = updatedBoards.lastOrNull()

        val newT1Score = lastBoard?.team1ScoreAfterBoard ?: 0
        val newT2Score = lastBoard?.team2ScoreAfterBoard ?: 0
        val newBoardNum = (lastBoard?.boardNumber ?: 0) + 1

        _state = _state.copy(
            currentBoardNumber = newBoardNum,
            team1Score = newT1Score,
            team2Score = newT2Score,
            completedBoards = updatedBoards,
            isMatchOver = false,
            matchWinnerTeamId = null,
            isWonByNillRule = false,
            endTime = null,
            currentBoardResultDialog = null
        )

        return _state
    }

    fun updateState(newState: GameState) {
        _state = newState
    }
}
