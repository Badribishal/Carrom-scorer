package com.example.carrom.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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

    val isQueenAvailable = board.queenStatus == QueenStatus.AVAILABLE
    val canUndo = turn.undoStack.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "B${board.boardNumber} • H${turn.currentHand}",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "First to ${config.targetPoints} pts",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Break: ${currentBreaker.name} ($breakingTeamName)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
        },
        bottomBar = {
            // DOCKED BOTTOM CONTROLS: Always accessible without scrolling
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo Button
                    OutlinedButton(
                        onClick = onUndo,
                        enabled = canUndo,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(0.35f)
                            .height(50.dp)
                            .testTag("undo_button")
                    ) {
                        Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo", modifier = Modifier.size(18.dp))
                        if (canUndo) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("(${turn.undoStack.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // End Turn Button
                    Button(
                        onClick = onEndTurn,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(0.65f)
                            .height(50.dp)
                            .testTag("end_turn_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("End Turn", fontWeight = FontWeight.Black, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "Next: ${nextPlayer.name}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. MINIMALIST DUAL SCOREBOARD HEADER
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("team_scoreboard_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Team 1 Block
                    MinimalTeamScoreColumn(
                        teamName = config.team1Name,
                        score = state.team1Score,
                        color = t1Color,
                        remainingCoins = if (t1Color == TeamColor.WHITE) board.whiteRemaining else board.blackRemaining,
                        isCurrentTurn = currentTeamId == 1,
                        modifier = Modifier.weight(1f)
                    )

                    // Center VS & Queen Pill
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = "VS",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        // Compact Queen Status Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (board.queenStatus) {
                                QueenStatus.AVAILABLE -> CarromQueenRed.copy(alpha = 0.15f)
                                QueenStatus.PENDING_COVER -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                QueenStatus.COVERED -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (board.queenStatus) {
                                                QueenStatus.AVAILABLE -> CarromQueenRed
                                                QueenStatus.PENDING_COVER -> Color(0xFFFF9800)
                                                QueenStatus.COVERED -> Color(0xFF4CAF50)
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (board.queenStatus) {
                                        QueenStatus.AVAILABLE -> "Queen"
                                        QueenStatus.PENDING_COVER -> "Cover Pending"
                                        QueenStatus.COVERED -> "Covered"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (board.queenStatus) {
                                        QueenStatus.AVAILABLE -> CarromQueenRed
                                        QueenStatus.PENDING_COVER -> Color(0xFFE65100)
                                        QueenStatus.COVERED -> Color(0xFF2E7D32)
                                    }
                                )
                            }
                        }
                    }

                    // Team 2 Block
                    MinimalTeamScoreColumn(
                        teamName = config.team2Name,
                        score = state.team2Score,
                        color = t2Color,
                        remainingCoins = if (t2Color == TeamColor.WHITE) board.whiteRemaining else board.blackRemaining,
                        isCurrentTurn = currentTeamId == 2,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. COMPACT CURRENT SHOOTER & TURN STATS BAR
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("current_turn_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Shooter Info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlayerAvatar(
                            name = currentPlayer.name,
                            avatarColorIndex = currentPlayer.avatarColorIndex,
                            size = 32.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentPlayer.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                CarromCoinBadge(color = currentTeamColor, size = 10.dp)
                            }
                            Text(
                                text = "$currentTeamName • Turn #${turn.currentOverallTurnNumber}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Live Turn Badges: White, Black, Queen, Penalty
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MinimalTurnChip(label = "⚪", count = turn.currentTurnWhite)
                        MinimalTurnChip(label = "⚫", count = turn.currentTurnBlack)
                        if (turn.currentTurnQueenPocketed || turn.currentTurnQueenCovered) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (turn.currentTurnQueenCovered) Color(0xFF4CAF50).copy(alpha = 0.2f) else CarromQueenRed.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (turn.currentTurnQueenCovered) "👑✓" else "👑?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (turn.currentTurnQueenCovered) Color(0xFF2E7D32) else CarromQueenRed,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (turn.currentTurnPenalties > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFE0B2)
                            ) {
                                Text(
                                    text = "-${turn.currentTurnPenalties}P",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. ACTION PAD: COMPACT 2x2 GRID (NO SCROLL REQUIRED)
            val whiteTeamName = if (t1Color == TeamColor.WHITE) config.team1Name else config.team2Name
            val blackTeamName = if (t1Color == TeamColor.BLACK) config.team1Name else config.team2Name
            val isShootingWhite = currentTeamColor == TeamColor.WHITE

            // Primary Coin Buttons (+White, +Black)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // + White Button
                Button(
                    onClick = onPocketWhite,
                    enabled = board.whiteRemaining > 0,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFAF7F2),
                        contentColor = Color(0xFF3E2723),
                        disabledContainerColor = Color(0xFFE0E0E0),
                        disabledContentColor = Color(0xFF9E9E9E)
                    ),
                    border = if (isShootingWhite) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .testTag("pocket_white_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CarromCoinBadge(color = TeamColor.WHITE, size = 22.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "+ White", fontWeight = FontWeight.Black, fontSize = 14.sp)
                                if (isShootingWhite) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "YOU",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${board.whiteRemaining} left",
                                fontSize = 10.sp,
                                color = Color(0xFF6D4C41)
                            )
                        }
                    }
                }

                // + Black Button
                Button(
                    onClick = onPocketBlack,
                    enabled = board.blackRemaining > 0,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF212121),
                        contentColor = Color(0xFFFAFAFA),
                        disabledContainerColor = Color(0xFF757575),
                        disabledContentColor = Color(0xFFBDBDBD)
                    ),
                    border = if (!isShootingWhite) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .testTag("pocket_black_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CarromCoinBadge(color = TeamColor.BLACK, size = 22.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "+ Black", fontWeight = FontWeight.Black, fontSize = 14.sp)
                                if (!isShootingWhite) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "YOU",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${board.blackRemaining} left",
                                fontSize = 10.sp,
                                color = Color(0xFFE0E0E0)
                            )
                        }
                    }
                }
            }

            // Secondary Quick Actions (Queen & Penalty)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Queen Button
                Button(
                    onClick = onPocketQueen,
                    enabled = isQueenAvailable,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CarromQueenRed,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFE57373).copy(alpha = 0.4f),
                        disabledContentColor = Color(0xFFFFCDD2)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("pocket_queen_button")
                ) {
                    QueenCoinBadge(size = 18.dp, isCovered = board.queenStatus == QueenStatus.COVERED)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (board.queenStatus) {
                            QueenStatus.AVAILABLE -> "+ Queen"
                            QueenStatus.PENDING_COVER -> "Pending Cover"
                            QueenStatus.COVERED -> "Covered ✓"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Penalty Button
                FilledTonalButton(
                    onClick = onRecordPenalty,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFFFE0B2),
                        contentColor = Color(0xFFE65100)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("record_penalty_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = "Penalty", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Compact Turn Rotation Order
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Turn Order",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        state.rotationOrder.forEachIndexed { idx, player ->
                            val isCurrent = idx == turn.currentTurnIndexInRotation
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = player.name,
                                    fontSize = 9.sp,
                                    fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Normal,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
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
private fun MinimalTeamScoreColumn(
    teamName: String,
    score: Int,
    color: TeamColor,
    remainingCoins: Int,
    isCurrentTurn: Boolean,
    modifier: Modifier = Modifier
) {
    val activeBorderColor = MaterialTheme.colorScheme.primary
    val activeBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    val inactiveBgColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrentTurn) activeBgColor else inactiveBgColor)
            .border(
                width = if (isCurrentTurn) 1.5.dp else 0.5.dp,
                color = if (isCurrentTurn) activeBorderColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 6.dp, horizontal = 6.dp)
    ) {
        if (isCurrentTurn) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Text(
                    text = "SHOOTING",
                    fontWeight = FontWeight.Black,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }

        Text(
            text = teamName,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1
        )

        Text(
            text = score.toString().padStart(2, '0'),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = if (isCurrentTurn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CarromCoinBadge(color = color, size = 10.dp)
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "$remainingCoins left",
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MinimalTurnChip(
    label: String,
    count: Int
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Text(
            text = "$label $count",
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}
