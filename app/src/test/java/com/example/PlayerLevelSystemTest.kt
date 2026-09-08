package com.example

import com.example.carrom.data.local.entity.PlayerEntity
import com.example.carrom.engine.LevelTier
import com.example.carrom.engine.PlayerLevelSystem
import org.junit.Assert.*
import org.junit.Test

class PlayerLevelSystemTest {

    @Test
    fun rookie_player_starts_at_level_1() {
        val player = PlayerEntity(
            id = 1L,
            name = "Rookie Striker",
            groupName = "Beginners",
            matchesPlayed = 0,
            matchesWon = 0
        )
        val levelInfo = PlayerLevelSystem.calculatePlayerXp(player)
        assertEquals(1, levelInfo.level)
        assertEquals(0, levelInfo.totalXp)
        assertEquals(LevelTier.ROOKIE, levelInfo.tier)
    }

    @Test
    fun player_with_stats_unlocks_levels_and_achievements() {
        val veteran = PlayerEntity(
            id = 2L,
            name = "Champion",
            groupName = "Champions League",
            matchesPlayed = 25,
            matchesWon = 20,
            whitePocketed = 80,
            blackPocketed = 70,
            queensCovered = 15,
            nillBoardWins = 3
        )
        val levelInfo = PlayerLevelSystem.calculatePlayerXp(veteran)
        assertTrue("Level should rise above 1", levelInfo.level > 5)
        assertTrue("XP should be calculated from stats", levelInfo.totalXp > 1000)

        val achievements = PlayerLevelSystem.getPlayerAchievements(veteran)
        val firstMatch = achievements.first { it.id == "match_first" }
        assertTrue("First match achievement should be unlocked", firstMatch.isUnlocked)

        val queenMaster = achievements.first { it.id == "queen_15" }
        assertTrue("Queen achievement should be unlocked with 15 queens covered", queenMaster.isUnlocked)

        val nillFinisher = achievements.first { it.id == "nill_board" }
        assertTrue("Nill finisher should be unlocked with 3 nill wins", nillFinisher.isUnlocked)
    }
}

