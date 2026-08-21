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
            // 24+ Queen rule check: Once team score >= 24, Queen points are no longer added
            if (_state.config.enable24PlusQueenRule && teamScoreBeforeBoard >= 24) {
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

        val winningTeamFinalScore = if (winningTeamId == 1) newTeam1Score else newTeam2Score
        val isMatchWon = winningTeamFinalScore >= _state.config.targetPoints
        val matchWinnerId = if (isMatchWon) winningTeamId else null

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

    fun updateState(newState: GameState) {
        _state = newState
    }
}
