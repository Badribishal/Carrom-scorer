package com.example.carrom.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carrom.engine.GameState
import com.example.carrom.engine.QueenStatus
import com.example.carrom.engine.TeamColor
import com.example.carrom.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScoreboardScreen(
    state: GameState,
    onPocketWhite: () -> Unit,
    onPocketBlack: () -> Unit,
    onPocketQueen: () -> Unit,
    onRecordPenalty: () -> Unit,
    onUndo: () -> Unit,
    onEndTurn: () -> Unit,
    onDismissBoardDialog: () -> Unit,
    onStartNextBoard: () -> Unit,
    onFinishAndSaveMatch: () -> Unit,
    onAbandonMatch: () -> Unit,
    onNewMatch: () -> Unit,
    onHome: () -> Unit
) {
    var showAbandonConfirmDialog by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }
    var showTurnLogSheet by remember { mutableStateOf(false) }

    // Intercept back gesture during live match to prompt confirmation
    BackHandler {
        showAbandonConfirmDialog = true
    }

    // If match is finished, display Match Complete Screen
    if (state.isMatchOver) {
        MatchCompleteScreen(
            state = state,
            onSaveAndFinish = onFinishAndSaveMatch,
            onNewMatch = onNewMatch,
            onHome = onHome
        )
        return
    }

    val config = state.config
    val board = state.boardState
    val turn = state.turnState
    val currentPlayer = state.currentPlayer
    val nextPlayer = state.nextPlayer
    val currentTeamId = state.currentTeamId
    val nextTeamId = state.nextTeamId
    val currentTeamName = state.currentTeamName
    val currentTeamColor = state.currentTeamColor

    val t1Color = state.getTeamColorForBoard(1)
    val t2Color = state.getTeamColorForBoard(2)

    val currentBreaker = state.currentBoardBreaker
    val breakingTeamId = state.currentBoardBreakingTeamId
    val breakingTeamName = if (breakingTeamId == 1) config.team1Name else config.team2Name
    val opponentTeamName = if (breakingTeamId == 1) config.team2Name else config.team1Name

    val isQueenAvailable = board.queenStatus == QueenStatus.AVAILABLE
    val canUndo = turn.undoStack.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "BOARD ${board.boardNumber} • HAND ${turn.currentHand}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Break: ${currentBreaker.name} ($breakingTeamName) ⚪ • Target: ${config.targetPoints} pts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (config.proMode) {
                        IconButton(
                            onClick = { showTurnLogSheet = true },
                            modifier = Modifier.testTag("view_turn_logs_button")
                        ) {
                            Icon(imageVector = Icons.Default.History, contentDescription = "Turn Logs")
                        }
                    }
                    IconButton(
                        onClick = { showRulesDialog = true },
                        modifier = Modifier.testTag("scoreboard_rules_button")
                    ) {
                        Icon(imageVector = Icons.Default.MenuBook, contentDescription = "Rules")
                    }
                    IconButton(
                        onClick = { showAbandonConfirmDialog = true },
                        modifier = Modifier.testTag("abandon_match_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Abandon Match")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. TEAM SCOREBOARD COMPARISON CARD (WITH DYNAMIC ACTIVE TURN HIGHLIGHT)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = CardDefaults.outlinedCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("team_scoreboard_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Team 1 Card (Active turn highlights shift dynamically between Team 1 and Team 2)
                        TeamScoreSummaryColumn(
                            teamName = config.team1Name,
                            score = state.team1Score,
                            color = t1Color,
                            remainingCoins = if (t1Color == TeamColor.WHITE) board.whiteRemaining else board.blackRemaining,
                            isCurrentTurn = currentTeamId == 1,
                            activeShooterName = if (currentTeamId == 1) currentPlayer.name else null,
                            nextShooterName = if (currentTeamId != 1 && nextTeamId == 1) nextPlayer.name else null,
                            modifier = Modifier.weight(1f)
                        )

                        // VS divider
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = "VS",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Board ${board.boardNumber}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Team 2 Card (Active turn highlights shift dynamically between Team 1 and Team 2)
                        TeamScoreSummaryColumn(
                            teamName = config.team2Name,
                            score = state.team2Score,
                            color = t2Color,
                            remainingCoins = if (t2Color == TeamColor.WHITE) board.whiteRemaining else board.blackRemaining,
                            isCurrentTurn = currentTeamId == 2,
                            activeShooterName = if (currentTeamId == 2) currentPlayer.name else null,
                            nextShooterName = if (currentTeamId != 2 && nextTeamId == 2) nextPlayer.name else null,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Breaker and Coin Assignment Strip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CarromCoinBadge(color = TeamColor.WHITE, size = 16.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Break: ${currentBreaker.name} ($breakingTeamName) plays White",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CarromCoinBadge(color = TeamColor.BLACK, size = 16.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$opponentTeamName (Black)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // TURN ROTATION ORDER STRIP
                    TurnRotationStrip(
                        rotationOrder = state.rotationOrder,
                        currentIndex = turn.currentTurnIndexInRotation,
                        breakerPlayerId = currentBreaker.id,
                        config = config
                    )
                }
            }

            // 2. QUEEN STATUS BANNER
            QueenStatusIndicator(status = board.queenStatus)

            // 3. CURRENT TURN ACTIVE PLAYER CARD
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("current_turn_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlayerAvatar(
                                name = currentPlayer.name,
                                avatarColorIndex = currentPlayer.avatarColorIndex,
                                size = 44.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentPlayer.name,
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "SHOOTING",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "$currentTeamName • Playing ${currentTeamColor.displayName} Coins",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = "Turn #${turn.currentOverallTurnNumber}",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Turn statistics badges (White/Black/Queen/Penalty this turn)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TurnBadge(
                            label = "White",
                            count = turn.currentTurnWhite,
                            color = TeamColor.WHITE,
                            modifier = Modifier.weight(1f)
                        )
                        TurnBadge(
                            label = "Black",
                            count = turn.currentTurnBlack,
                            color = TeamColor.BLACK,
                            modifier = Modifier.weight(1f)
                        )
                        TurnBadge(
                            label = "Queen",
                            count = if (turn.currentTurnQueenCovered) 1 else 0,
                            isQueen = true,
                            isQueenPending = turn.currentTurnQueenPocketed && !turn.currentTurnQueenCovered,
                            isQueenCovered = turn.currentTurnQueenCovered,
                            modifier = Modifier.weight(1f)
                        )
                        TurnBadge(
                            label = "Penalties",
                            count = turn.currentTurnPenalties,
                            isPenalty = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. ACTION PAD (BUTTON CONTROLS)
            Text(
                text = "Turn Actions",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Primary Coin Action Buttons: +White, +Black
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val whiteTeamName = if (t1Color == TeamColor.WHITE) config.team1Name else config.team2Name
                val blackTeamName = if (t1Color == TeamColor.BLACK) config.team1Name else config.team2Name
                val isShootingWhite = currentTeamColor == TeamColor.WHITE

                // + White Button
                Button(
                    onClick = onPocketWhite,
                    enabled = board.whiteRemaining > 0,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFAF7F2),
                        contentColor = Color(0xFF3E2723),
                        disabledContainerColor = Color(0xFFE0E0E0),
                        disabledContentColor = Color(0xFF9E9E9E)
                    ),
                    border = if (isShootingWhite) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(68.dp)
                        .testTag("pocket_white_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CarromCoinBadge(color = TeamColor.WHITE, size = 26.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "+ White", fontWeight = FontWeight.Black, fontSize = 15.sp)
                                if (isShootingWhite) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "YOU",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "$whiteTeamName • ${board.whiteRemaining} left",
                                fontSize = 10.sp,
                                color = Color(0xFF6D4C41),
                                maxLines = 1
                            )
                        }
                    }
                }

                // + Black Button
                Button(
                    onClick = onPocketBlack,
                    enabled = board.blackRemaining > 0,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF262626),
                        contentColor = Color(0xFFFAFAFA),
                        disabledContainerColor = Color(0xFF757575),
                        disabledContentColor = Color(0xFFBDBDBD)
                    ),
                    border = if (!isShootingWhite) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(68.dp)
                        .testTag("pocket_black_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CarromCoinBadge(color = TeamColor.BLACK, size = 26.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "+ Black", fontWeight = FontWeight.Black, fontSize = 15.sp)
                                if (!isShootingWhite) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "YOU",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "$blackTeamName • ${board.blackRemaining} left",
                                fontSize = 10.sp,
                                color = Color(0xFFE0E0E0),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Queen & Penalty Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Queen Button
                Button(
                    onClick = onPocketQueen,
                    enabled = isQueenAvailable,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CarromQueenRed,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFE57373).copy(alpha = 0.5f),
                        disabledContentColor = Color(0xFFFFCDD2)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("pocket_queen_button")
                ) {
                    QueenCoinBadge(size = 20.dp, isCovered = board.queenStatus == QueenStatus.COVERED)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (board.queenStatus) {
                            QueenStatus.AVAILABLE -> "Pocket Queen"
                            QueenStatus.PENDING_COVER -> "Pending Cover"
                            QueenStatus.COVERED -> "Queen Covered"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Penalty Button
                FilledTonalButton(
                    onClick = onRecordPenalty,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFFFE0B2),
                        contentColor = Color(0xFFE65100)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("record_penalty_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Penalty", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // Bottom Flow Controls: Undo & End Turn (with clear next player preview)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Undo Button
                OutlinedButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(0.8f)
                        .height(54.dp)
                        .testTag("undo_button")
                ) {
                    Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Undo", fontWeight = FontWeight.SemiBold)
                }

                // End Turn Button with explicit next player preview
                Button(
                    onClick = onEndTurn,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(54.dp)
                        .testTag("end_turn_button")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("End Turn", fontWeight = FontWeight.Black, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "Next: ${nextPlayer.name} (${if (nextTeamId == 1) config.team1Name else config.team2Name})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    // Board Result Dialog when board completes
    if (state.currentBoardResultDialog != null) {
        val completedBoardNum = state.currentBoardResultDialog.boardNumber
        val nextBoardNum = completedBoardNum + 1
        val nextBreaker = state.getBreakerForBoard(nextBoardNum)
        val nextBreakerTeamId = state.getBreakingTeamIdForBoard(nextBoardNum)
        val nextBreakerTeamName = if (nextBreakerTeamId == 1) config.team1Name else config.team2Name

        BoardResultDialog(
            board = state.currentBoardResultDialog,
            team1Name = config.team1Name,
            team2Name = config.team2Name,
            nextBoardNumber = nextBoardNum,
            nextBreakerName = nextBreaker.name,
            nextBreakerTeamName = nextBreakerTeamName,
            onDismiss = onDismissBoardDialog,
            onStartNextBoard = onStartNextBoard
        )
    }

    // Abandon Confirmation Dialog
    if (showAbandonConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showAbandonConfirmDialog = false },
            title = { Text("Abandon Current Match?") },
            text = { Text("Your match progress will be discarded. Are you sure you want to exit?") },
            confirmButton = {
                Button(
                    onClick = {
                        showAbandonConfirmDialog = false
                        onAbandonMatch()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_abandon_match_button")
                ) {
                    Text("Abandon Match")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbandonConfirmDialog = false }) {
                    Text("Continue Playing")
                }
            }
        )
    }

    // Rules Dialog
    if (showRulesDialog) {
        RulesDialog(onDismiss = { showRulesDialog = false })
    }

    // Turn Logs Sheet (Pro Mode)
    if (showTurnLogSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTurnLogSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(max = 500.dp)
            ) {
                Text(
                    text = "Pro Mode Turn Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (state.allTurnLogs.isEmpty()) {
                    Text(
                        text = "No completed turns yet in this match.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.allTurnLogs.reversed()) { log ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Turn #${log.turnNumber} • Hand ${log.handNumber} • ${log.playerName}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Team ${log.teamId} (${log.teamColor.displayName})",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (log.whitePocketed > 0) {
                                            CarromCoinBadge(color = TeamColor.WHITE, size = 20.dp, count = log.whitePocketed)
                                        }
                                        if (log.blackPocketed > 0) {
                                            CarromCoinBadge(color = TeamColor.BLACK, size = 20.dp, count = log.blackPocketed)
                                        }
                                        if (log.queenCovered) {
                                            QueenCoinBadge(size = 20.dp, isCovered = true)
                                        } else if (log.queenPocketed) {
                                            QueenCoinBadge(size = 20.dp, isCovered = false)
                                        }
                                        if (log.penalties > 0) {
                                            Text(
                                                text = "-${log.penalties}P",
                                                color = Color(0xFFC62828),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamScoreSummaryColumn(
    teamName: String,
    score: Int,
    color: TeamColor,
    remainingCoins: Int,
    isCurrentTurn: Boolean,
    activeShooterName: String?,
    nextShooterName: String?,
    modifier: Modifier = Modifier
) {
    val activeBorderColor = MaterialTheme.colorScheme.primary
    val activeBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
    val inactiveBgColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isCurrentTurn) activeBgColor else inactiveBgColor)
            .border(
                width = if (isCurrentTurn) 2.dp else 1.dp,
                color = if (isCurrentTurn) activeBorderColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        // Prominent Active Turn Status Pill
        if (isCurrentTurn) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color(0xFF69F0AE))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ACTIVE TURN",
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = if (nextShooterName != null) "NEXT" else "WAITING",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Team Name & Coin Color Badge
        Text(
            text = teamName,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1
        )

        // Coin piece indicator with clear label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            CarromCoinBadge(color = color, size = 14.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${color.displayName} Coins",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Big Team Score
        Text(
            text = score.toString().padStart(2, '0'),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = if (isCurrentTurn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Remaining coins count
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Left: ",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$remainingCoins / 9",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (remainingCoins == 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
            )
        }

        // Active Shooter display
        if (isCurrentTurn && activeShooterName != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "🎯 $activeShooterName",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TurnRotationStrip(
    rotationOrder: List<com.example.carrom.engine.Player>,
    currentIndex: Int,
    breakerPlayerId: Long? = null,
    config: com.example.carrom.engine.MatchConfig,
    modifier: Modifier = Modifier
) {
    if (rotationOrder.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TURN ROTATION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val currentShooter = rotationOrder[currentIndex % rotationOrder.size]
            val shooterTeamId = config.getPlayerTeamId(currentShooter)
            val shooterTeamName = if (shooterTeamId == 1) config.team1Name else config.team2Name
            Text(
                text = "Now: ${currentShooter.name} ($shooterTeamName)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            rotationOrder.forEachIndexed { idx, player ->
                val isShooting = (idx == currentIndex % rotationOrder.size)
                val isBreaker = (player.id == breakerPlayerId)
                val playerTeamId = config.getPlayerTeamId(player)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isShooting) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    border = if (isShooting) {
                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    } else null,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isShooting) "▶ ${player.name}" else player.name,
                            fontSize = 10.sp,
                            fontWeight = if (isShooting) FontWeight.Black else FontWeight.Normal,
                            color = if (isShooting) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "T$playerTeamId",
                                fontSize = 8.sp,
                                color = if (isShooting) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isBreaker) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "⚪Break",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isShooting) MaterialTheme.colorScheme.onPrimary else Color(0xFF1565C0)
                                )
                            }
                        }
                    }
                }

                if (idx < rotationOrder.size - 1) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TurnBadge(
    label: String,
    count: Int,
    color: TeamColor? = null,
    isQueen: Boolean = false,
    isQueenPending: Boolean = false,
    isQueenCovered: Boolean = false,
    isPenalty: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (isQueen) {
                Text(
                    text = when {
                        isQueenCovered -> "+5"
                        isQueenPending -> "Cover?"
                        else -> "-"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isQueenCovered) Color(0xFF2E7D32) else if (isQueenPending) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface
                )
            } else if (isPenalty) {
                Text(
                    text = if (count > 0) "$count" else "-",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (count > 0) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = if (count > 0) "$count" else "-",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
