package com.example.carrom.engine

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.carrom.data.local.entity.PlayerEntity

/**
 * Visual and competitive tier ranking based on player levels.
 */
enum class LevelTier(
    val title: String,
    val color: Color,
    val backgroundTint: Color
) {
    ROOKIE("Rookie", Color(0xFF78909C), Color(0xFFECEFF1)),
    BRONZE("Bronze", Color(0xFFCD7F32), Color(0xFFFBE9E7)),
    SILVER("Silver", Color(0xFF9E9E9E), Color(0xFFF5F5F5)),
    GOLD("Gold", Color(0xFFFFB300), Color(0xFFFFF8E1)),
    PLATINUM("Platinum", Color(0xFF00897B), Color(0xFFE0F2F1)),
    DIAMOND("Diamond", Color(0xFF1E88E5), Color(0xFFE3F2FD)),
    MASTER("Master", Color(0xFF8E24AA), Color(0xFFF3E5F5)),
    LEGEND("Legend", Color(0xFFE64A19), Color(0xFFFBE9E7))
}

/**
 * Computed level data for a player based on their career stats and XP.
 */
data class PlayerLevelInfo(
    val level: Int,
    val title: String,
    val tier: LevelTier,
    val totalXp: Int,
    val currentLevelMinXp: Int,
    val nextLevelTargetXp: Int,
    val xpIntoCurrentLevel: Int,
    val xpRequiredForNextLevel: Int,
    val progressPercent: Float,
    val matchesPlayedXp: Int,
    val matchesWonXp: Int,
    val coinsPocketedXp: Int,
    val queenCoveredXp: Int,
    val nillBoardXp: Int,
    val experienceBonusXp: Int,
    val achievementBonusXp: Int = 0,
    val unlockedAchievementsCount: Int = 0
)

/**
 * Category of carrom milestone achievement.
 */
enum class AchievementCategory(val displayName: String) {
    ALL("All"),
    MATCHES("Matches"),
    COINS("Coins"),
    QUEEN("Queen"),
    MASTERY("Mastery")
}

/**
 * Visual badge tier of an achievement.
 */
enum class BadgeTier(val label: String, val color: Color) {
    BRONZE("Bronze", Color(0xFFCD7F32)),
    SILVER("Silver", Color(0xFFA8A8A8)),
    GOLD("Gold", Color(0xFFFFB300)),
    PLATINUM("Platinum", Color(0xFF00ACC1)),
    DIAMOND("Diamond", Color(0xFF8E24AA))
}

/**
 * Individual achievement status for a player.
 */
data class PlayerAchievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val category: AchievementCategory,
    val tier: BadgeTier,
    val target: Int,
    val currentProgress: Int,
    val isUnlocked: Boolean,
    val progressPercent: Float,
    val rewardXp: Int
)

object PlayerLevelSystem {

    /**
     * Level thresholds definition up to Level 20.
     * Above level 20, dynamic scaling applies.
     */
    private val LEVEL_CONFIGS = listOf(
        Triple(1, 0, Pair("Novice Striker", LevelTier.ROOKIE)),
        Triple(2, 120, Pair("Apprentice Pocket", LevelTier.ROOKIE)),
        Triple(3, 300, Pair("Pocket Enthusiast", LevelTier.ROOKIE)),
        Triple(4, 550, Pair("Club Challenger", LevelTier.BRONZE)),
        Triple(5, 900, Pair("Bronze Ace", LevelTier.BRONZE)),
        Triple(6, 1350, Pair("Silver Tactician", LevelTier.SILVER)),
        Triple(7, 1900, Pair("Silver Marksman", LevelTier.SILVER)),
        Triple(8, 2600, Pair("Gold Veteran", LevelTier.GOLD)),
        Triple(9, 3450, Pair("Queen Hunter", LevelTier.GOLD)),
        Triple(10, 4450, Pair("Gold Master", LevelTier.GOLD)),
        Triple(11, 5650, Pair("Platinum Prodigy", LevelTier.PLATINUM)),
        Triple(12, 7050, Pair("Platinum Specialist", LevelTier.PLATINUM)),
        Triple(13, 8700, Pair("Diamond Strategist", LevelTier.DIAMOND)),
        Triple(14, 10600, Pair("Diamond Maestro", LevelTier.DIAMOND)),
        Triple(15, 12800, Pair("Grandmaster", LevelTier.MASTER)),
        Triple(16, 15300, Pair("Board Sovereign", LevelTier.MASTER)),
        Triple(17, 18100, Pair("Supreme Striker", LevelTier.MASTER)),
        Triple(18, 21300, Pair("Crown Champion", LevelTier.LEGEND)),
        Triple(19, 25000, Pair("Master of the Board", LevelTier.LEGEND)),
        Triple(20, 29500, Pair("Carrom Legend", LevelTier.LEGEND))
    )

    /**
     * Calculates deterministic Experience Points (XP) from a player's statistics.
     * Works seamlessly on newly recorded games as well as historical / imported data.
     * Unlocked achievement milestones grant bonus XP rewards directly into the leveling system!
     */
    fun calculatePlayerXp(player: PlayerEntity): PlayerLevelInfo {
        val matchesPlayedXp = player.matchesPlayed * 40
        val matchesWonXp = player.matchesWon * 100
        val coinsPocketedXp = (player.totalCoinsPocketed * 12) + (player.whitePocketed * 4)
        val queenCoveredXp = player.queensCovered * 50
        val nillBoardXp = player.nillBoardWins * 80
        val experienceBonusXp = (player.boardsWon * 15) + (player.totalPointsContributed * 2)

        val baseStatXp = matchesPlayedXp + matchesWonXp + coinsPocketedXp + queenCoveredXp + nillBoardXp + experienceBonusXp

        // Performance milestones tie directly into the leveling progression as XP rewards
        val statAchievements = computeStatAchievements(player)
        val unlockedCount = statAchievements.count { it.isUnlocked }
        val achievementBonusXp = statAchievements.filter { it.isUnlocked }.sumOf { it.rewardXp }

        val totalXp = baseStatXp + achievementBonusXp

        var level = 1
        var title = "Novice Striker"
        var tier = LevelTier.ROOKIE
        var currentLevelMinXp = 0
        var nextLevelTargetXp = 120

        for (i in LEVEL_CONFIGS.indices) {
            val (lvl, requiredXp, config) = LEVEL_CONFIGS[i]
            if (totalXp >= requiredXp) {
                level = lvl
                title = config.first
                tier = config.second
                currentLevelMinXp = requiredXp
                nextLevelTargetXp = if (i + 1 < LEVEL_CONFIGS.size) {
                    LEVEL_CONFIGS[i + 1].second
                } else {
                    requiredXp + 5000
                }
            } else {
                break
            }
        }

        // For levels > 20
        if (totalXp >= 29500) {
            val extraXp = totalXp - 29500
            val extraLevels = extraXp / 5000
            level = 20 + extraLevels
            title = "Carrom Legend"
            tier = LevelTier.LEGEND
            currentLevelMinXp = 29500 + (extraLevels * 5000)
            nextLevelTargetXp = currentLevelMinXp + 5000
        }

        val xpIntoCurrent = (totalXp - currentLevelMinXp).coerceAtLeast(0)
        val xpRequiredForNext = (nextLevelTargetXp - currentLevelMinXp).coerceAtLeast(1)
        val progress = (xpIntoCurrent.toFloat() / xpRequiredForNext.toFloat()).coerceIn(0f, 1f)

        return PlayerLevelInfo(
            level = level,
            title = title,
            tier = tier,
            totalXp = totalXp,
            currentLevelMinXp = currentLevelMinXp,
            nextLevelTargetXp = nextLevelTargetXp,
            xpIntoCurrentLevel = xpIntoCurrent,
            xpRequiredForNextLevel = xpRequiredForNext,
            progressPercent = progress,
            matchesPlayedXp = matchesPlayedXp,
            matchesWonXp = matchesWonXp,
            coinsPocketedXp = coinsPocketedXp,
            queenCoveredXp = queenCoveredXp,
            nillBoardXp = nillBoardXp,
            experienceBonusXp = experienceBonusXp,
            achievementBonusXp = achievementBonusXp,
            unlockedAchievementsCount = unlockedCount
        )
    }

    /**
     * Evaluates performance milestone achievements tied to cumulative player metrics.
     */
    fun computeStatAchievements(player: PlayerEntity): List<PlayerAchievement> {
        return listOf(
            // MATCHES CATEGORY
            PlayerAchievement(
                id = "match_first",
                title = "Board Debut",
                description = "Complete your first carrom match",
                icon = Icons.Default.SportsEsports,
                category = AchievementCategory.MATCHES,
                tier = BadgeTier.BRONZE,
                target = 1,
                currentProgress = player.matchesPlayed,
                isUnlocked = player.matchesPlayed >= 1,
                progressPercent = (player.matchesPlayed.toFloat() / 1f).coerceIn(0f, 1f),
                rewardXp = 50
            ),
            PlayerAchievement(
                id = "match_5",
                title = "Club Regular",
                description = "Play 5 competitive carrom matches",
                icon = Icons.Default.SportsEsports,
                category = AchievementCategory.MATCHES,
                tier = BadgeTier.BRONZE,
                target = 5,
                currentProgress = player.matchesPlayed,
                isUnlocked = player.matchesPlayed >= 5,
                progressPercent = (player.matchesPlayed.toFloat() / 5f).coerceIn(0f, 1f),
                rewardXp = 100
            ),
            PlayerAchievement(
                id = "match_15",
                title = "Tournament Veteran",
                description = "Play 15 carrom matches",
                icon = Icons.Default.EmojiEvents,
                category = AchievementCategory.MATCHES,
                tier = BadgeTier.SILVER,
                target = 15,
                currentProgress = player.matchesPlayed,
                isUnlocked = player.matchesPlayed >= 15,
                progressPercent = (player.matchesPlayed.toFloat() / 15f).coerceIn(0f, 1f),
                rewardXp = 200
            ),
            PlayerAchievement(
                id = "match_30",
                title = "Board Centurion",
                description = "Play 30 carrom matches",
                icon = Icons.Default.WorkspacePremium,
                category = AchievementCategory.MATCHES,
                tier = BadgeTier.GOLD,
                target = 30,
                currentProgress = player.matchesPlayed,
                isUnlocked = player.matchesPlayed >= 30,
                progressPercent = (player.matchesPlayed.toFloat() / 30f).coerceIn(0f, 1f),
                rewardXp = 400
            ),
            PlayerAchievement(
                id = "win_first",
                title = "First Victory",
                description = "Win your first full match",
                icon = Icons.Default.Star,
                category = AchievementCategory.MATCHES,
                tier = BadgeTier.BRONZE,
                target = 1,
                currentProgress = player.matchesWon,
                isUnlocked = player.matchesWon >= 1,
                progressPercent = (player.matchesWon.toFloat() / 1f).coerceIn(0f, 1f),
                rewardXp = 80
            ),
            PlayerAchievement(
                id = "win_5",
                title = "Five-Star Striker",
                description = "Win 5 matches",
                icon = Icons.Default.Star,
                category = AchievementCategory.MATCHES,
                tier = BadgeTier.SILVER,
                target = 5,
                currentProgress = player.matchesWon,
                isUnlocked = player.matchesWon >= 5,
                progressPercent = (player.matchesWon.toFloat() / 5f).coerceIn(0f, 1f),
                rewardXp = 150
            ),
            PlayerAchievement(
                id = "win_15",
                title = "Carrom Champion",
                description = "Win 15 matches",
                icon = Icons.Default.EmojiEvents,
                category = AchievementCategory.MATCHES,
                tier = BadgeTier.GOLD,
                target = 15,
                currentProgress = player.matchesWon,
                isUnlocked = player.matchesWon >= 15,
                progressPercent = (player.matchesWon.toFloat() / 15f).coerceIn(0f, 1f),
                rewardXp = 350
            ),

            // COINS / DOTS POCKETED CATEGORY
            PlayerAchievement(
                id = "coin_first",
                title = "First Strike",
                description = "Pocket your first carrom coin / dot",
                icon = Icons.Default.Adjust,
                category = AchievementCategory.COINS,
                tier = BadgeTier.BRONZE,
                target = 1,
                currentProgress = player.totalCoinsPocketed,
                isUnlocked = player.totalCoinsPocketed >= 1,
                progressPercent = (player.totalCoinsPocketed.toFloat() / 1f).coerceIn(0f, 1f),
                rewardXp = 30
            ),
            PlayerAchievement(
                id = "coin_25",
                title = "Coin Collector",
                description = "Pocket 25 coins / dots",
                icon = Icons.Default.Adjust,
                category = AchievementCategory.COINS,
                tier = BadgeTier.BRONZE,
                target = 25,
                currentProgress = player.totalCoinsPocketed,
                isUnlocked = player.totalCoinsPocketed >= 25,
                progressPercent = (player.totalCoinsPocketed.toFloat() / 25f).coerceIn(0f, 1f),
                rewardXp = 80
            ),
            PlayerAchievement(
                id = "coin_100",
                title = "Century Striker",
                description = "Pocket 100 total coins / dots across matches",
                icon = Icons.Default.Grain,
                category = AchievementCategory.COINS,
                tier = BadgeTier.SILVER,
                target = 100,
                currentProgress = player.totalCoinsPocketed,
                isUnlocked = player.totalCoinsPocketed >= 100,
                progressPercent = (player.totalCoinsPocketed.toFloat() / 100f).coerceIn(0f, 1f),
                rewardXp = 200
            ),
            PlayerAchievement(
                id = "coin_250",
                title = "Sharpshooter",
                description = "Pocket 250 coins / dots",
                icon = Icons.Default.FlashOn,
                category = AchievementCategory.COINS,
                tier = BadgeTier.GOLD,
                target = 250,
                currentProgress = player.totalCoinsPocketed,
                isUnlocked = player.totalCoinsPocketed >= 250,
                progressPercent = (player.totalCoinsPocketed.toFloat() / 250f).coerceIn(0f, 1f),
                rewardXp = 400
            ),
            PlayerAchievement(
                id = "white_specialist",
                title = "White Specialist",
                description = "Pocket 50 White coins (First Break mastery)",
                icon = Icons.Default.Brightness1,
                category = AchievementCategory.COINS,
                tier = BadgeTier.SILVER,
                target = 50,
                currentProgress = player.whitePocketed,
                isUnlocked = player.whitePocketed >= 50,
                progressPercent = (player.whitePocketed.toFloat() / 50f).coerceIn(0f, 1f),
                rewardXp = 150
            ),
            PlayerAchievement(
                id = "black_specialist",
                title = "Black Specialist",
                description = "Pocket 50 Black coins",
                icon = Icons.Default.Brightness1,
                category = AchievementCategory.COINS,
                tier = BadgeTier.SILVER,
                target = 50,
                currentProgress = player.blackPocketed,
                isUnlocked = player.blackPocketed >= 50,
                progressPercent = (player.blackPocketed.toFloat() / 50f).coerceIn(0f, 1f),
                rewardXp = 150
            ),

            // QUEEN COVERED CATEGORY
            PlayerAchievement(
                id = "queen_first",
                title = "Royal Coronation",
                description = "Pocket and successfully cover your first Queen",
                icon = Icons.Default.Stars,
                category = AchievementCategory.QUEEN,
                tier = BadgeTier.BRONZE,
                target = 1,
                currentProgress = player.queensCovered,
                isUnlocked = player.queensCovered >= 1,
                progressPercent = (player.queensCovered.toFloat() / 1f).coerceIn(0f, 1f),
                rewardXp = 100
            ),
            PlayerAchievement(
                id = "queen_5",
                title = "Queen Specialist",
                description = "Successfully cover 5 Queens",
                icon = Icons.Default.AutoAwesome,
                category = AchievementCategory.QUEEN,
                tier = BadgeTier.SILVER,
                target = 5,
                currentProgress = player.queensCovered,
                isUnlocked = player.queensCovered >= 5,
                progressPercent = (player.queensCovered.toFloat() / 5f).coerceIn(0f, 1f),
                rewardXp = 250
            ),
            PlayerAchievement(
                id = "queen_15",
                title = "Queen Expert",
                description = "Successfully cover 15 Queens to earn Queen Expert status",
                icon = Icons.Default.MilitaryTech,
                category = AchievementCategory.QUEEN,
                tier = BadgeTier.GOLD,
                target = 15,
                currentProgress = player.queensCovered,
                isUnlocked = player.queensCovered >= 15,
                progressPercent = (player.queensCovered.toFloat() / 15f).coerceIn(0f, 1f),
                rewardXp = 500
            ),
            PlayerAchievement(
                id = "queen_30",
                title = "Absolute Monarch",
                description = "Cover 30 Queens in competitive play",
                icon = Icons.Default.WorkspacePremium,
                category = AchievementCategory.QUEEN,
                tier = BadgeTier.DIAMOND,
                target = 30,
                currentProgress = player.queensCovered,
                isUnlocked = player.queensCovered >= 30,
                progressPercent = (player.queensCovered.toFloat() / 30f).coerceIn(0f, 1f),
                rewardXp = 800
            ),

            // MASTERY & EXPERIENCE CATEGORY
            PlayerAchievement(
                id = "nill_board",
                title = "White-Wash Master",
                description = "Win a match or board by Nill Rule (opponent scores 0)",
                icon = Icons.Default.Whatshot,
                category = AchievementCategory.MASTERY,
                tier = BadgeTier.GOLD,
                target = 1,
                currentProgress = player.nillBoardWins,
                isUnlocked = player.nillBoardWins >= 1,
                progressPercent = (player.nillBoardWins.toFloat() / 1f).coerceIn(0f, 1f),
                rewardXp = 300
            ),
            PlayerAchievement(
                id = "board_10",
                title = "Board Sweeper",
                description = "Win 10 individual boards",
                icon = Icons.Default.Dashboard,
                category = AchievementCategory.MASTERY,
                tier = BadgeTier.SILVER,
                target = 10,
                currentProgress = player.boardsWon,
                isUnlocked = player.boardsWon >= 10,
                progressPercent = (player.boardsWon.toFloat() / 10f).coerceIn(0f, 1f),
                rewardXp = 180
            ),
            PlayerAchievement(
                id = "high_scorer",
                title = "High Scorer",
                description = "Contribute 75+ points to team scores in matches",
                icon = Icons.Default.Score,
                category = AchievementCategory.MASTERY,
                tier = BadgeTier.GOLD,
                target = 75,
                currentProgress = player.totalPointsContributed,
                isUnlocked = player.totalPointsContributed >= 75,
                progressPercent = (player.totalPointsContributed.toFloat() / 75f).coerceIn(0f, 1f),
                rewardXp = 350
            )
        )
    }

    /**
     * Computes all achievements and their unlock status for any player entity.
     * Pure and deterministic: works immediately on imported players as well!
     */
    fun getPlayerAchievements(player: PlayerEntity): List<PlayerAchievement> {
        val levelInfo = calculatePlayerXp(player)
        val statAchievements = computeStatAchievements(player)

        val levelAchievements = listOf(
            PlayerAchievement(
                id = "level_5",
                title = "Rising Prodigy",
                description = "Attain Level 5 (Bronze Ace)",
                icon = Icons.Default.MilitaryTech,
                category = AchievementCategory.MASTERY,
                tier = BadgeTier.BRONZE,
                target = 5,
                currentProgress = levelInfo.level,
                isUnlocked = levelInfo.level >= 5,
                progressPercent = (levelInfo.level.toFloat() / 5f).coerceIn(0f, 1f),
                rewardXp = 200
            ),
            PlayerAchievement(
                id = "level_10",
                title = "Elite Striker",
                description = "Attain Level 10 (Gold Master)",
                icon = Icons.Default.WorkspacePremium,
                category = AchievementCategory.MASTERY,
                tier = BadgeTier.GOLD,
                target = 10,
                currentProgress = levelInfo.level,
                isUnlocked = levelInfo.level >= 10,
                progressPercent = (levelInfo.level.toFloat() / 10f).coerceIn(0f, 1f),
                rewardXp = 500
            ),
            PlayerAchievement(
                id = "level_15",
                title = "Supreme Master",
                description = "Attain Level 15 (Diamond Legend)",
                icon = Icons.Default.EmojiEvents,
                category = AchievementCategory.MASTERY,
                tier = BadgeTier.DIAMOND,
                target = 15,
                currentProgress = levelInfo.level,
                isUnlocked = levelInfo.level >= 15,
                progressPercent = (levelInfo.level.toFloat() / 15f).coerceIn(0f, 1f),
                rewardXp = 1000
            )
        )

        return statAchievements + levelAchievements
    }
}
