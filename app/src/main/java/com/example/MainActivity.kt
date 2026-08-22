package com.example

import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.carrom.ui.screens.*
import com.example.carrom.viewmodel.*
import com.example.ui.theme.CarromScoreboardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureHighRefreshRate()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val themePreset by settingsViewModel.themePreset.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            CarromScoreboardTheme(preset = themePreset, darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CarromAppNavigation(settingsViewModel = settingsViewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        configureHighRefreshRate()
    }

    /**
     * Unlocks 120Hz / highest available display refresh rate for buttery-smooth animations and input.
     */
    private fun configureHighRefreshRate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.attributes.let { params ->
                    val display = display
                    if (display != null) {
                        val supportedModes = display.supportedModes
                        val highestRefreshRateMode = supportedModes.maxByOrNull { it.refreshRate }
                        if (highestRefreshRateMode != null) {
                            params.preferredDisplayModeId = highestRefreshRateMode.modeId
                            window.attributes = params
                        }
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
                val display = windowManager?.defaultDisplay
                if (display != null) {
                    val supportedModes = display.supportedModes
                    val highestRefreshRateMode = supportedModes.maxByOrNull { it.refreshRate }
                    if (highestRefreshRateMode != null) {
                        val params = window.attributes
                        params.preferredDisplayModeId = highestRefreshRateMode.modeId
                        window.attributes = params
                    }
                }
            }
        } catch (_: Exception) {
            // Graceful fallback on devices with restrictive display policies
        }
    }
}

object Destinations {
    const val HOME = "home"
    const val MATCH_SETUP = "match_setup"
    const val LIVE_SCOREBOARD = "live_scoreboard"
    const val MATCH_COMPLETE = "match_complete"
    const val MATCH_HISTORY = "match_history"
    const val PLAYER_STATS = "player_stats"
    const val SETTINGS = "settings"
}

@Composable
fun CarromAppNavigation(
    settingsViewModel: SettingsViewModel,
    carromViewModel: CarromViewModel = viewModel(),
    historyViewModel: MatchHistoryViewModel = viewModel()
) {
    val navController = rememberNavController()

    val liveGameState by carromViewModel.liveGameState.collectAsStateWithLifecycle()
    val activeSavedMatch by carromViewModel.activeSavedMatch.collectAsStateWithLifecycle()
    val allPlayers by carromViewModel.allPlayers.collectAsStateWithLifecycle()
    val allMatches by historyViewModel.matches.collectAsStateWithLifecycle()
    val selectedMatchData by historyViewModel.selectedMatch.collectAsStateWithLifecycle()

    val soundEnabled by settingsViewModel.soundEnabled.collectAsStateWithLifecycle()
    val vibrationEnabled by settingsViewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val ruleDefaults by settingsViewModel.ruleDefaults.collectAsStateWithLifecycle()
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val themePreset by settingsViewModel.themePreset.collectAsStateWithLifecycle()

    var showGlobalRulesDialog by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = Destinations.HOME,
        enterTransition = {
            fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            )
        }
    ) {
        // HOME SCREEN
        composable(Destinations.HOME) {
            HomeScreen(
                activeMatch = liveGameState ?: activeSavedMatch,
                onResumeMatch = {
                    if (liveGameState != null) {
                        navController.navigate(Destinations.LIVE_SCOREBOARD)
                    } else if (activeSavedMatch != null) {
                        carromViewModel.resumeMatch(activeSavedMatch!!)
                        navController.navigate(Destinations.LIVE_SCOREBOARD)
                    }
                },
                onNewMatch = {
                    navController.navigate(Destinations.MATCH_SETUP)
                },
                onPlayerStats = {
                    navController.navigate(Destinations.PLAYER_STATS)
                },
                onMatchHistory = {
                    navController.navigate(Destinations.MATCH_HISTORY)
                },
                onSettings = {
                    navController.navigate(Destinations.SETTINGS)
                },
                onRules = {
                    showGlobalRulesDialog = true
                }
            )
        }

        // MATCH SETUP SCREEN
        composable(Destinations.MATCH_SETUP) {
            MatchSetupScreen(
                savedPlayers = allPlayers,
                onBack = { navController.popBackStack() },
                onStartMatch = { team1Name, team2Name, team1Players, team2Players, firstBreakerPlayerId, proMode, targetPoints, nillThreshold, queenPts, queenStopThreshold, enableQueenStopRule ->
                    carromViewModel.startNewMatch(
                        team1Name = team1Name,
                        team2Name = team2Name,
                        team1Players = team1Players,
                        team2Players = team2Players,
                        firstBreakerPlayerId = firstBreakerPlayerId,
                        proMode = proMode,
                        targetPoints = targetPoints,
                        nillBoardThreshold = nillThreshold,
                        queenPoints = queenPts,
                        queenStopThreshold = queenStopThreshold,
                        enableQueenStopRule = enableQueenStopRule
                    )
                    navController.navigate(Destinations.LIVE_SCOREBOARD) {
                        popUpTo(Destinations.HOME)
                    }
                }
            )
        }

        // LIVE SCOREBOARD SCREEN
        composable(Destinations.LIVE_SCOREBOARD) {
            val currentState = liveGameState ?: activeSavedMatch
            if (currentState != null) {
                // If match is over, navigate automatically to match complete screen
                if (currentState.isMatchOver) {
                    LaunchedEffect(currentState.matchId, currentState.isMatchOver) {
                        navController.navigate(Destinations.MATCH_COMPLETE) {
                            popUpTo(Destinations.HOME)
                        }
                    }
                }

                LiveScoreboardScreen(
                    state = currentState,
                    onPocketWhite = { carromViewModel.pocketWhite() },
                    onPocketBlack = { carromViewModel.pocketBlack() },
                    onPocketQueen = { carromViewModel.pocketQueen() },
                    onRecordPenalty = { carromViewModel.recordPenalty() },
                    onUndo = { carromViewModel.undo() },
                    onEndTurn = { carromViewModel.endTurn() },
                    onDismissBoardDialog = { carromViewModel.dismissBoardResultDialog() },
                    onStartNextBoard = { carromViewModel.startNextBoard() },
                    onFinishAndSaveMatch = {
                        carromViewModel.finishAndSaveMatch()
                        navController.navigate(Destinations.HOME) {
                            popUpTo(Destinations.HOME) { inclusive = true }
                        }
                    },
                    onAbandonMatch = {
                        carromViewModel.abandonMatch()
                        navController.navigate(Destinations.HOME) {
                            popUpTo(Destinations.HOME) { inclusive = true }
                        }
                    },
                    onNewMatch = {
                        carromViewModel.finishAndSaveMatch()
                        navController.navigate(Destinations.MATCH_SETUP) {
                            popUpTo(Destinations.HOME)
                        }
                    },
                    onHome = {
                        navController.navigate(Destinations.HOME) {
                            popUpTo(Destinations.HOME) { inclusive = true }
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // MATCH COMPLETE SCREEN
        composable(Destinations.MATCH_COMPLETE) {
            val currentState = liveGameState ?: activeSavedMatch
            if (currentState != null) {
                MatchCompleteScreen(
                    state = currentState,
                    onSaveAndFinish = {
                        carromViewModel.finishAndSaveMatch()
                        navController.navigate(Destinations.HOME) {
                            popUpTo(Destinations.HOME) { inclusive = true }
                        }
                    },
                    onNewMatch = {
                        carromViewModel.finishAndSaveMatch()
                        navController.navigate(Destinations.MATCH_SETUP) {
                            popUpTo(Destinations.HOME)
                        }
                    },
                    onHome = {
                        carromViewModel.finishAndSaveMatch()
                        navController.navigate(Destinations.HOME) {
                            popUpTo(Destinations.HOME) { inclusive = true }
                        }
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.HOME) { inclusive = true }
                    }
                }
            }
        }

        // MATCH HISTORY SCREEN
        composable(Destinations.MATCH_HISTORY) {
            MatchHistoryScreen(
                matches = allMatches,
                selectedMatchData = selectedMatchData,
                onSelectMatch = { matchEntity ->
                    historyViewModel.selectMatch(matchEntity)
                },
                onDeleteMatch = { matchId ->
                    historyViewModel.deleteMatch(matchId)
                },
                onBack = { navController.popBackStack() }
            )
        }

        // PLAYER STATS SCREEN
        composable(Destinations.PLAYER_STATS) {
            PlayerStatsScreen(
                players = allPlayers,
                onAddNewPlayer = { name, colorIndex ->
                    carromViewModel.startNewMatch(
                        team1Name = "Team 1",
                        team2Name = "Team 2",
                        team1Players = emptyList(),
                        team2Players = emptyList(),
                        firstBreakerPlayerId = 0L,
                        proMode = true
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }

        // SETTINGS SCREEN
        composable(Destinations.SETTINGS) {
            SettingsScreen(
                themeMode = themeMode,
                themePreset = themePreset,
                soundEnabled = soundEnabled,
                vibrationEnabled = vibrationEnabled,
                ruleDefaults = ruleDefaults,
                onThemeChange = { settingsViewModel.setThemeMode(it) },
                onThemePresetChange = { settingsViewModel.setThemePreset(it) },
                onSoundChange = { settingsViewModel.setSoundEnabled(it) },
                onVibrationChange = { settingsViewModel.setVibrationEnabled(it) },
                onRulesChange = { settingsViewModel.updateRuleDefaults(it) },
                onResetAllData = {
                    settingsViewModel.resetAllData {
                        carromViewModel.clearLiveState()
                        navController.navigate(Destinations.HOME) {
                            popUpTo(Destinations.HOME) { inclusive = true }
                        }
                    }
                },
                onOpenRulesDialog = {
                    showGlobalRulesDialog = true
                },
                onBack = { navController.popBackStack() }
            )
        }
    }

    if (showGlobalRulesDialog) {
        RulesDialog(
            onDismiss = { showGlobalRulesDialog = false }
        )
    }
}
