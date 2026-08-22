package com.example

import com.example.carrom.engine.*
import org.junit.Assert.*
import org.junit.Test

class CarromGameEngineTest {

    @Test
    fun test19PointRule_QueenPointsCeaseAt19OrAbove() {
        val config = MatchConfig(
            team1Name = "Team 1",
            team2Name = "Team 2",
            team1Players = listOf(Player(1, "Alice")),
            team2Players = listOf(Player(2, "Bob")),
            firstBreakerPlayerId = 1L,
            queenStopThreshold = 19,
            enableQueenStopRule = true
        )

        // Case 1: Score is 18 (below 19). Queen pocketed & covered should award 5 points.
        val stateBelow19 = GameState(
            matchId = 1L,
            config = config,
            team1Score = 18,
            team2Score = 5,
            currentBoardNumber = 1
        )
        val engine1 = CarromGameEngine(stateBelow19)
        // Pocket queen
        engine1.pocketQueen()
        // Cover queen by pocketing white
        engine1.pocketWhite()
        // Clear remaining 8 whites to complete board
        repeat(8) { engine1.pocketWhite() }

        val resultState1 = engine1.state
        val boardRecord1 = resultState1.completedBoards.firstOrNull()
        assertNotNull(boardRecord1)
        assertEquals("Queen points should be awarded when score < 19", 5, boardRecord1?.queenPointsAwarded)
        // Score = 9 black coins left + 5 queen points = 14 board score -> 18 + 14 = 32 (won match)
        assertEquals(32, resultState1.team1Score)

        // Case 2: Score is 19 (at threshold). Queen pocketed & covered should award 0 points.
        val stateAt19 = GameState(
            matchId = 2L,
            config = config,
            team1Score = 19,
            team2Score = 10,
            currentBoardNumber = 1
        )
        val engine2 = CarromGameEngine(stateAt19)
        engine2.pocketQueen()
        engine2.pocketWhite()
        repeat(8) { engine2.pocketWhite() }

        val resultState2 = engine2.state
        val boardRecord2 = resultState2.completedBoards.firstOrNull()
        assertNotNull(boardRecord2)
        assertEquals("Queen points must be 0 when score >= 19 under 19-point rule", 0, boardRecord2?.queenPointsAwarded)
        // Score = 9 black coins left + 0 queen points = 9 board score -> 19 + 9 = 28
        assertEquals(28, resultState2.team1Score)
    }
}
