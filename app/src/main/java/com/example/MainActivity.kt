package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.carrom.ui.screens.*
import com.example.ui.theme.CarromTheme
import com.example.carrom.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val themePreset by settingsViewModel.themePreset.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            CarromTheme(preset = themePreset, darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CarromAppNavigation(settingsViewModel = settingsViewModel)
                }
            }
        }
    }
}

object Destinations {
    const val HOME = "home"
    const val MATCH_SETUP = "match_setup"
    const val LIVE_SCOREBOARD = "live_scoreboard"
    const val PLAYER_STATS = "player_stats"
    const val MATCH_HISTORY = "match_history"
    const val SETTINGS = "settings"
}

@Composable
fun CarromAppNavigation(
    settingsViewModel: SettingsViewModel,
    carromViewModel: CarromViewModel = viewModel(),
    playerStatsViewModel: PlayerStatsViewModel = viewModel(),
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
        startDestination = Destinations.HOME
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
                onStartMatch = { team1Name, team2Name, team1Players, team2Players, firstBreakerPlayerId, proMode, targetPoints, nillThreshold, queenPts, enable24Rule ->
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
                        enable24PlusQueenRule = enable24Rule
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
                LaunchedEffect(Unit) {
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.HOME) { inclusive = true }
                    }
                }
            }
        }

        // PLAYER STATS SCREEN
        composable(Destinations.PLAYER_STATS) {
            PlayerStatsScreen(
                players = allPlayers,
                onBack = { navController.popBackStack() },
                onAddNewPlayer = { name, colorIndex ->
                    playerStatsViewModel.addPlayer(name, colorIndex)
                }
            )
        }

        // MATCH HISTORY SCREEN
        composable(Destinations.MATCH_HISTORY) {
            MatchHistoryScreen(
                matches = allMatches,
                selectedMatchData = selectedMatchData,
                onSelectMatch = { match -> historyViewModel.selectMatch(match) },
                onDeleteMatch = { id -> historyViewModel.deleteMatch(id) },
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
                onThemeChange = { mode -> settingsViewModel.setThemeMode(mode) },
                onThemePresetChange = { preset -> settingsViewModel.setThemePreset(preset) },
                onSoundChange = { enabled -> settingsViewModel.setSoundEnabled(enabled) },
                onVibrationChange = { enabled -> settingsViewModel.setVibrationEnabled(enabled) },
                onRulesChange = { rules -> settingsViewModel.updateRuleDefaults(rules) },
                onResetAllData = {
                    settingsViewModel.resetAllData {
                        navController.navigate(Destinations.HOME) {
                            popUpTo(Destinations.HOME) { inclusive = true }
                        }
                    }
                },
                onOpenRulesDialog = { showGlobalRulesDialog = true },
                onBack = { navController.popBackStack() }
            )
        }
    }

    if (showGlobalRulesDialog) {
        RulesDialog(onDismiss = { showGlobalRulesDialog = false })
    }
}
