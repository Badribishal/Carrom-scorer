package com.example

import com.example.carrom.data.local.CarromJsonParser
import com.example.carrom.engine.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CarromEngineTest {

    private lateinit var sampleConfig: MatchConfig
    private lateinit var engine: CarromGameEngine

    @Before
    fun setUp() {
        sampleConfig = MatchConfig(
            team1Name = "Team 1",
            team2Name = "Team 2",
            team1Players = listOf(Player(1, "Player A"), Player(2, "Player B")),
            team2Players = listOf(Player(3, "Player C"), Player(4, "Player D")),
            firstBreakerPlayerId = 1L, // Team 1 breaks -> White
            proMode = true,
            targetPoints = 29,
            nillBoardThreshold = 7,
            queenPoints = 5,
            enable24PlusQueenRule = true
        )

        val initialState = GameState(
            matchId = 100L,
            config = sampleConfig,
            team1Score = 0,
            team2Score = 0,
            currentBoardNumber = 1,
            boardState = BoardLiveState(boardNumber = 1),
            turnState = TurnLiveState(currentHand = 1, currentTurnIndexInRotation = 0, currentOverallTurnNumber = 1)
        )

        engine = CarromGameEngine(initialState)
    }

    @Test
    fun test1_white9Cleared_black4Remaining_expectedBoardScore4() {
        // Team 1 plays White. Black has 4 remaining -> 5 black pocketed.
        repeat(5) { engine.pocketBlack() }
        assertEquals(4, engine.state.boardState.blackRemaining)

        // Clear all 9 White coins
        repeat(9) { engine.pocketWhite() }
        assertEquals(0, engine.state.boardState.whiteRemaining)
        assertTrue(engine.state.boardState.isCompleted)
        assertEquals(1, engine.state.boardState.winnerTeamId)
        assertEquals(4, engine.state.boardState.boardScore)
        assertEquals(4, engine.state.team1Score)
    }

    @Test
    fun test2_white9Cleared_black4Remaining_queenCovered_expectedBoardScore9() {
        // Pocket Queen and cover with White in same turn
        engine.pocketQueen()
        assertEquals(QueenStatus.PENDING_COVER, engine.state.boardState.queenStatus)
        engine.pocketWhite() // Player A is White -> covers Queen!
        assertEquals(QueenStatus.COVERED, engine.state.boardState.queenStatus)

        // 5 black pocketed (4 remaining)
        repeat(5) { engine.pocketBlack() }
        assertEquals(4, engine.state.boardState.blackRemaining)

        // Remaining 8 white coins pocketed (total 9)
        repeat(8) { engine.pocketWhite() }
        assertEquals(0, engine.state.boardState.whiteRemaining)

        assertTrue(engine.state.boardState.isCompleted)
        // Score: 4 (opp remaining) + 5 (queen) = 9
        assertEquals(9, engine.state.boardState.boardScore)
        assertEquals(9, engine.state.team1Score)
    }

    @Test
    fun test3_queenPocketed_notCovered_turnEnds_queenReturnsToBoard() {
        engine.pocketQueen()
        assertEquals(QueenStatus.PENDING_COVER, engine.state.boardState.queenStatus)

        // Player ends turn without pocketing White
        engine.endTurn()
        assertEquals(QueenStatus.AVAILABLE, engine.state.boardState.queenStatus)
        assertNull(engine.state.boardState.queenPocketedByPlayerId)
    }

    @Test
    fun test4_queenPocketed_and_correctColourPocketedInSameTurn_queenCovered() {
        engine.pocketQueen()
        assertEquals(QueenStatus.PENDING_COVER, engine.state.boardState.queenStatus)
        engine.pocketWhite() // Current player is White
        assertEquals(QueenStatus.COVERED, engine.state.boardState.queenStatus)
        assertEquals(1L, engine.state.boardState.queenCoveredByPlayerId)
    }

    @Test
    fun test5_queenCoveredOnce_attemptAnotherQueen_noSecondQueenScore() {
        engine.pocketQueen()
        engine.pocketWhite()
        assertEquals(QueenStatus.COVERED, engine.state.boardState.queenStatus)

        // Attempt pocketing queen again
        val stateBefore = engine.state
        engine.pocketQueen()
        assertEquals(stateBefore, engine.state)
    }

    @Test
    fun test6_teamScoreReaches29_matchWinner() {
        // Set Team 1 score to 25
        val customState = engine.state.copy(team1Score = 25)
        engine.updateState(customState)

        // Team 1 clears white (opp 4 black remaining) = +4 points -> total 29
        repeat(5) { engine.pocketBlack() }
        repeat(9) { engine.pocketWhite() }

        assertEquals(29, engine.state.team1Score)
        assertTrue(engine.state.isMatchOver)
        assertEquals(1, engine.state.matchWinnerTeamId)
    }

    @Test
    fun test7_teamScoreReaches30_matchWinner() {
        // Set Team 1 score to 28
        val customState = engine.state.copy(team1Score = 28)
        engine.updateState(customState)

        // Team 1 clears white (opp 2 black remaining) = +2 points -> total 30
        repeat(7) { engine.pocketBlack() }
        repeat(9) { engine.pocketWhite() }

        assertEquals(30, engine.state.team1Score)
        assertTrue(engine.state.isMatchOver)
        assertEquals(1, engine.state.matchWinnerTeamId)
    }

    @Test
    fun test8_winningTeamCompletesBoard_opponentHasOnly5Points_nillBoardTriggered() {
        // Team 2 has 5 points total in match
        val customState = engine.state.copy(team1Score = 10, team2Score = 5)
        engine.updateState(customState)

        // Team 1 wins board
        repeat(9) { engine.pocketWhite() }

        assertTrue(engine.state.boardState.isCompleted)
        assertTrue(engine.state.boardState.isNillBoard)
    }

    @Test
    fun test9_opponentHasExactly7Points_notNillBoard() {
        // Team 2 has 7 points total in match
        val customState = engine.state.copy(team1Score = 10, team2Score = 7)
        engine.updateState(customState)

        // Team 1 wins board
        repeat(9) { engine.pocketWhite() }

        assertTrue(engine.state.boardState.isCompleted)
        assertFalse(engine.state.boardState.isNillBoard)
    }

    @Test
    fun test10_endTurn_countersRemainUnchanged() {
        engine.pocketWhite()
        engine.pocketWhite()
        assertEquals(7, engine.state.boardState.whiteRemaining)
        assertEquals(9, engine.state.boardState.blackRemaining)

        engine.endTurn()
        // White & Black remaining counters must remain 7 and 9
        assertEquals(7, engine.state.boardState.whiteRemaining)
        assertEquals(9, engine.state.boardState.blackRemaining)
    }

    @Test
    fun test11_whiteReaches0_remains0_notResetTo9() {
        repeat(9) { engine.pocketWhite() }
        assertEquals(0, engine.state.boardState.whiteRemaining)
        // End turn or check board state: white remaining is 0
        assertEquals(0, engine.state.boardState.whiteRemaining)
    }

    @Test
    fun test12_playerPocketsQueen_failsToCover_turnEnds_queenReturns() {
        assertEquals(QueenStatus.AVAILABLE, engine.state.boardState.queenStatus)
        engine.pocketQueen()
        assertEquals(QueenStatus.PENDING_COVER, engine.state.boardState.queenStatus)

        engine.endTurn()
        assertEquals(QueenStatus.AVAILABLE, engine.state.boardState.queenStatus)
        assertNull(engine.state.boardState.queenPocketedByPlayerId)
    }

    @Test
    fun test13_playerPocketsQueen_pocketsCorrectColour_queenCovered() {
        engine.pocketQueen()
        engine.pocketWhite()
        assertEquals(QueenStatus.COVERED, engine.state.boardState.queenStatus)
        assertEquals(1L, engine.state.boardState.queenCoveredByPlayerId)
        assertEquals(1, engine.state.boardState.queenCoveredByTeamId)
    }

    @Test
    fun test14_undoWhitePocket_whiteCounterRestored() {
        assertEquals(9, engine.state.boardState.whiteRemaining)
        engine.pocketWhite()
        assertEquals(8, engine.state.boardState.whiteRemaining)

        engine.undo()
        assertEquals(9, engine.state.boardState.whiteRemaining)
        assertEquals(0, engine.state.turnState.currentTurnWhite)
    }

    @Test
    fun test15_completeAllPlayersTurns_handIncrementsBy1() {
        assertEquals(1, engine.state.turnState.currentHand)

        // 4 players in rotation: P1 -> P3 -> P2 -> P4
        engine.endTurn() // Turn 1 (P1)
        assertEquals(1, engine.state.turnState.currentHand)
        engine.endTurn() // Turn 2 (P3)
        assertEquals(1, engine.state.turnState.currentHand)
        engine.endTurn() // Turn 3 (P2)
        assertEquals(1, engine.state.turnState.currentHand)
        engine.endTurn() // Turn 4 (P4)

        // Now full rotation completed -> Hand 2 begins!
        assertEquals(2, engine.state.turnState.currentHand)
    }

    @Test
    fun test16_closeAppDuringActiveMatch_serializeDeserialize_resumesIdenticalState() {
        engine.pocketWhite()
        engine.pocketWhite()
        engine.pocketQueen()
        engine.pocketWhite() // Covered queen
        engine.recordPenalty()

        val json = CarromJsonParser.serializeGameState(engine.state)
        val restored = CarromJsonParser.deserializeGameState(json)

        assertEquals(engine.state.matchId, restored.matchId)
        assertEquals(engine.state.boardState.whiteRemaining, restored.boardState.whiteRemaining)
        assertEquals(engine.state.boardState.blackRemaining, restored.boardState.blackRemaining)
        assertEquals(engine.state.boardState.queenStatus, restored.boardState.queenStatus)
        assertEquals(engine.state.boardState.queenCoveredByPlayerId, restored.boardState.queenCoveredByPlayerId)
        assertEquals(engine.state.turnState.currentTurnWhite, restored.turnState.currentTurnWhite)
        assertEquals(engine.state.turnState.currentTurnPenalties, restored.turnState.currentTurnPenalties)
        assertEquals(engine.state.turnState.currentHand, restored.turnState.currentHand)
        assertEquals(engine.state.config.team1Name, restored.config.team1Name)
        assertEquals(engine.state.config.team2Name, restored.config.team2Name)
    }
}
