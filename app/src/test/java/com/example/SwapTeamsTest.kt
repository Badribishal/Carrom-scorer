package com.example

import com.example.carrom.data.local.entity.GroupEntity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SwapTeamsTest {

    @Test
    fun testSwapTeamsLogic() {
        val group = GroupEntity(
            id = 1L,
            name = "Championship Group",
            team1Name = "Red Dragons",
            team2Name = "Blue Tigers",
            team1Player1 = "Alice",
            team1Player2 = "Bob",
            team2Player1 = "Charlie",
            team2Player2 = "David",
            isDoubles = true
        )

        var team1Name = group.team1Name
        var team2Name = group.team2Name
        var t1p1Name = group.team1Player1
        var t1p2Name = group.team1Player2
        var t2p1Name = group.team2Player1
        var t2p2Name = group.team2Player2
        var breakerIndex = 0 // Alice (T1P1) breaks

        val swap = {
            val tempTeamName = team1Name
            team1Name = team2Name
            team2Name = tempTeamName

            val tempP1 = t1p1Name
            t1p1Name = t2p1Name
            t2p1Name = tempP1

            val tempP2 = t1p2Name
            t1p2Name = t2p2Name
            t2p2Name = tempP2

            breakerIndex = when (breakerIndex) {
                0 -> 1
                1 -> 0
                2 -> 3
                3 -> 2
                else -> 0
            }
        }

        // Execute Swap
        swap()

        assertEquals("Blue Tigers", team1Name)
        assertEquals("Red Dragons", team2Name)
        assertEquals("Charlie", t1p1Name)
        assertEquals("David", t1p2Name)
        assertEquals("Alice", t2p1Name)
        assertEquals("Bob", t2p2Name)
        assertEquals(1, breakerIndex) // Alice is now in T2P1, so index 1 keeps Alice as breaker

        // Swap back
        swap()

        assertEquals("Red Dragons", team1Name)
        assertEquals("Blue Tigers", team2Name)
        assertEquals("Alice", t1p1Name)
        assertEquals("Bob", t1p2Name)
        assertEquals("Charlie", t2p1Name)
        assertEquals("David", t2p2Name)
        assertEquals(0, breakerIndex)
    }
}
