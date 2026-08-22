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
            team1Players = listOf(Player(1, "Player 1"), Player(3, "Player 3")),
            team2Players = listOf(Player(2, "Player 2"), Player(4, "Player 4")),
            firstBreakerPlayerId = 1L, // Team 1 breaks -> White
            proMode = true,
            targetPoints = 29,
            nillBoardThreshold = 7,
            queenPoints = 5,
            queenStopThreshold = 19,
            enableQueenStopRule = true
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
    fun test_sequentialTurnRotation_player1_then_player2_then_player3_then_player4() {
        val rotation = sampleConfig.buildRotationOrder()
        assertEquals(4, rotation.size)
        assertEquals("Player 1", rotation[0].name)
        assertEquals("Player 2", rotation[1].name)
        assertEquals("Player 3", rotation[2].name)
        assertEquals("Player 4", rotation[3].name)

        assertEquals("Player 1", engine.state.currentPlayer.name)
        assertEquals("Player 2", engine.state.nextPlayer.name)

        engine.endTurn()
        assertEquals("Player 2", engine.state.currentPlayer.name)
        assertEquals("Player 3", engine.state.nextPlayer.name)

        engine.endTurn()
        assertEquals("Player 3", engine.state.currentPlayer.name)
        assertEquals("Player 4", engine.state.nextPlayer.name)

        engine.endTurn()
        assertEquals("Player 4", engine.state.currentPlayer.name)
        assertEquals("Player 1", engine.state.nextPlayer.name)

        engine.endTurn()
        assertEquals("Player 1", engine.state.currentPlayer.name)
        assertEquals("Player 2", engine.state.nextPlayer.name)
        assertEquals(2, engine.state.turnState.currentHand)
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
        // Team 2 has 5 points total in match, Team 1 has 5
        val customState = engine.state.copy(team1Score = 5, team2Score = 5)
        engine.updateState(customState)

        // Team 1 wins board with 3 points (6 black pocketed -> 3 left)
        repeat(6) { engine.pocketBlack() }
        repeat(9) { engine.pocketWhite() }

        assertTrue(engine.state.boardState.isCompleted)
        assertTrue(engine.state.boardState.isNillBoard)
        assertEquals(8, engine.state.team1Score)
        assertFalse(engine.state.isMatchOver)
    }

    @Test
    fun test8b_team1Scores19WithQueenPotted_team2Under7_team1WinsMatchByNillBoard() {
        // Team 1 has 10 points, Team 2 has 4 points (< 7)
        val customState = engine.state.copy(team1Score = 10, team2Score = 4)
        engine.updateState(customState)

        // Team 1 pockets queen and covers with white (+5 pts)
        engine.pocketQueen()
        engine.pocketWhite()

        // 5 black pocketed (4 black left = +4 pts)
        repeat(5) { engine.pocketBlack() }

        // Pocket remaining 8 white (9 total white) -> Board total = 4 coins + 5 queen = 9 pts -> Team 1 total = 19 pts!
        repeat(8) { engine.pocketWhite() }

        assertEquals(19, engine.state.team1Score)
        assertEquals(4, engine.state.team2Score)
        assertTrue(engine.state.boardState.isCompleted)
        assertTrue(engine.state.boardState.isNillBoard)
        assertTrue(engine.state.isMatchOver)
        assertTrue(engine.state.isWonByNillRule)
        assertEquals(1, engine.state.matchWinnerTeamId)
    }

    @Test
    fun test8c_team2Scores19WithQueenPotted_team1Under7_team2WinsMatchByNillBoard() {
        // Team 2 plays Black (e.g. breaker is Player 1 White, but Player 2 takes turn)
        // Set state where Team 2 is active, has 10 points, Team 1 has 5 points (< 7)
        engine.endTurn() // Now Player 2 (Team 2, Black) is shooting
        assertEquals(2, engine.state.currentTeamId)

        val customState = engine.state.copy(team1Score = 5, team2Score = 10)
        engine.updateState(customState)

        // Team 2 pockets queen and covers with black (+5 pts)
        engine.pocketQueen()
        engine.pocketBlack()

        // 5 white pocketed (4 white left = +4 pts)
        repeat(5) { engine.pocketWhite() }

        // Pocket remaining 8 black (9 total black) -> Board total = 4 coins + 5 queen = 9 pts -> Team 2 total = 19 pts!
        repeat(8) { engine.pocketBlack() }

        assertEquals(5, engine.state.team1Score)
        assertEquals(19, engine.state.team2Score)
        assertTrue(engine.state.boardState.isCompleted)
        assertTrue(engine.state.boardState.isNillBoard)
        assertTrue(engine.state.isMatchOver)
        assertTrue(engine.state.isWonByNillRule)
        assertEquals(2, engine.state.matchWinnerTeamId)
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
        assertFalse(engine.state.isWonByNillRule)
        // Since Team 2 has 7 points (>= 7), match is NOT over via Nill rule (19 vs 7), continues towards 29 points
        assertFalse(engine.state.isMatchOver)
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

        // 4 players in rotation: P1 -> P2 -> P3 -> P4
        engine.endTurn() // Turn 1 (P1)
        assertEquals(1, engine.state.turnState.currentHand)
        engine.endTurn() // Turn 2 (P2)
        assertEquals(1, engine.state.turnState.currentHand)
        engine.endTurn() // Turn 3 (P3)
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
