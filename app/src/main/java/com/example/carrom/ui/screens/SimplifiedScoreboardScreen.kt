package com.example.carrom.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carrom.engine.GameState
import com.example.carrom.engine.Player
import com.example.carrom.engine.QueenStatus
import com.example.carrom.engine.TeamColor
import com.example.carrom.ui.components.CarromCoinBadge
import com.example.carrom.ui.components.PlayerAvatar
import com.example.carrom.ui.components.QueenCoinBadge
import com.example.ui.theme.CarromQueenRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimplifiedScoreboardScreen(
    state: GameState,
    onRecordBoardScore: (winningTeamId: Int, opponentCoinsLeft: Int, queenCoveredPlayerId: Long?, queenCoveredTeamId: Int?) -> Unit,
    onUndoLastBoard: () -> Unit,
    onFinishAndSaveMatch: () -> Unit,
    onAbandonMatch: () -> Unit,
    onNewMatch: () -> Unit,
    onHome: () -> Unit
) {
    val config = state.config
    val currentBoardNumber = state.currentBoardNumber
    val currentBreaker = state.getBreakerForBoard(currentBoardNumber)
    val breakerTeamId = config.getPlayerTeamId(currentBreaker.id)

    // Form states for entering the current board's result
    var selectedWinningTeamId by remember(currentBoardNumber) { mutableIntStateOf(breakerTeamId) }
    var opponentCoinsRemaining by remember(currentBoardNumber) { mutableIntStateOf(1) }
    
    // Queen possession state: null = not covered, 1 = Team 1, 2 = Team 2
    var selectedQueenTeamId by remember(currentBoardNumber) { mutableStateOf<Int?>(breakerTeamId) }
    var selectedQueenPlayerId by remember(currentBoardNumber, selectedQueenTeamId) {
        mutableStateOf(
            when (selectedQueenTeamId) {
                1 -> config.team1Players.firstOrNull()?.id
                2 -> config.team2Players.firstOrNull()?.id
                else -> null
            }
        )
    }

    var showAbandonDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    BackHandler {
        showAbandonDialog = true
    }

    // Calculate score live preview according to official rules
    val teamScoreBeforeBoard = if (selectedWinningTeamId == 1) state.team1Score else state.team2Score
    val isQueenCoveredByWinner = selectedQueenTeamId != null && selectedQueenTeamId == selectedWinningTeamId
    val isQueenStopApplied = config.enableQueenStopRule && teamScoreBeforeBoard >= config.queenStopThreshold
    val queenPointsAwarded = if (isQueenCoveredByWinner) {
        if (isQueenStopApplied) 0 else config.queenPoints
    } else 0

    val calculatedBoardScore = opponentCoinsRemaining + queenPointsAwarded
    val newPreviewT1Score = if (selectedWinningTeamId == 1) state.team1Score + calculatedBoardScore else state.team1Score
    val newPreviewT2Score = if (selectedWinningTeamId == 2) state.team2Score + calculatedBoardScore else state.team2Score

    val losingTeamScore = if (selectedWinningTeamId == 1) state.team2Score else state.team1Score
    val isNillBoardPreview = losingTeamScore < config.nillBoardThreshold
    val previewT1NillWin = newPreviewT1Score >= config.nillWinThreshold && newPreviewT2Score < config.nillBoardThreshold
    val previewT2NillWin = newPreviewT2Score >= config.nillWinThreshold && newPreviewT1Score < config.nillBoardThreshold
    val isPreviewNillMatchWin = (selectedWinningTeamId == 1 && previewT1NillWin) || (selectedWinningTeamId == 2 && previewT2NillWin)
    val isPreviewTargetWin = (selectedWinningTeamId == 1 && newPreviewT1Score >= config.targetPoints) || (selectedWinningTeamId == 2 && newPreviewT2Score >= config.targetPoints)
    val isPreviewMatchOver = isPreviewNillMatchWin || isPreviewTargetWin

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Simplified Scoreboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Board $currentBoardNumber • Target: ${config.targetPoints} pts",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { showAbandonDialog = true },
                        modifier = Modifier.testTag("simplified_back_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Exit Match")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("New Match") },
                            onClick = {
                                showMenu = false
                                onNewMatch()
                            },
                            leadingIcon = { Icon(Icons.Default.Replay, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Save & Finish Early") },
                            onClick = {
                                showMenu = false
                                onFinishAndSaveMatch()
                            },
                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Abandon Match", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showAbandonDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(modifier = Modifier.height(2.dp))

                // 1. MATCH SCORE HEADER CARD
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Team 1 Block
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = config.team1Name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${state.team1Score}",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Center VS & Breaker badge
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Text(
                                        text = "BOARD $currentBoardNumber",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Break: ${currentBreaker.name}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Team 2 Block
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = config.team2Name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${state.team2Score}",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // 2. BOARD RESULT ENTRY FORM CARD
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simplified_board_entry_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Record Board #$currentBoardNumber Score",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "ICF Standard",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // A. SELECT WINNING TEAM
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "1. Which team won this board?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Team 1 button
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedWinningTeamId == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        if (selectedWinningTeamId == 1) 2.dp else 1.dp,
                                        if (selectedWinningTeamId == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedWinningTeamId = 1
                                            selectedQueenPlayerId = config.team1Players.firstOrNull()?.id
                                        }
                                        .testTag("simplified_select_team1_winner")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        if (selectedWinningTeamId == 1) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = config.team1Name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (selectedWinningTeamId == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // Team 2 button
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedWinningTeamId == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        if (selectedWinningTeamId == 2) 2.dp else 1.dp,
                                        if (selectedWinningTeamId == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedWinningTeamId = 2
                                            selectedQueenPlayerId = config.team2Players.firstOrNull()?.id
                                        }
                                        .testTag("simplified_select_team2_winner")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        if (selectedWinningTeamId == 2) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = config.team2Name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (selectedWinningTeamId == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // B. OPPONENT'S REMAINING COINS ON BOARD
                        val losingTeamName = if (selectedWinningTeamId == 1) config.team2Name else config.team1Name
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "2. $losingTeamName's remaining coins on board:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$opponentCoinsRemaining pts",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items((1..9).toList()) { count ->
                                    val isSelected = opponentCoinsRemaining == count
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            if (isSelected) 1.5.dp else 1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clickable { opponentCoinsRemaining = count }
                                            .testTag("simplified_coins_chip_$count")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$count",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // C. QUEEN POSSESSION & BONUS RECORDING
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    QueenCoinBadge(
                                        status = if (selectedQueenTeamId != null) QueenStatus.COVERED else QueenStatus.AVAILABLE,
                                        size = 22.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "3. Who potted & covered the Queen?",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = when {
                                                selectedQueenTeamId == selectedWinningTeamId && selectedQueenPlayerId != null -> {
                                                    if (queenPointsAwarded > 0) "+$queenPointsAwarded pts Queen bonus to winner" else "Covered (24-pt cutoff: 0 bonus pts)"
                                                }
                                                selectedQueenTeamId != null && selectedQueenPlayerId != null -> {
                                                    val qTeam = if (selectedQueenTeamId == 1) config.team1Name else config.team2Name
                                                    "Covered by $qTeam (0 winner bonus pts, stats saved)"
                                                }
                                                else -> "Queen not covered / on board (0 pts)"
                                            },
                                            fontSize = 10.sp,
                                            color = if (queenPointsAwarded > 0) CarromQueenRed else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Team 1 / Team 2 / Not Covered Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Team 1 Option
                                FilterChip(
                                    selected = selectedQueenTeamId == 1,
                                    onClick = {
                                        selectedQueenTeamId = 1
                                        if (selectedQueenPlayerId == null || config.team1Players.none { it.id == selectedQueenPlayerId }) {
                                            selectedQueenPlayerId = config.team1Players.firstOrNull()?.id
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = config.team1Name,
                                            fontWeight = if (selectedQueenTeamId == 1) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    },
                                    leadingIcon = if (selectedQueenTeamId == 1) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("simplified_queen_team1_chip")
                                )

                                // Team 2 Option
                                FilterChip(
                                    selected = selectedQueenTeamId == 2,
                                    onClick = {
                                        selectedQueenTeamId = 2
                                        if (selectedQueenPlayerId == null || config.team2Players.none { it.id == selectedQueenPlayerId }) {
                                            selectedQueenPlayerId = config.team2Players.firstOrNull()?.id
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = config.team2Name,
                                            fontWeight = if (selectedQueenTeamId == 2) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    },
                                    leadingIcon = if (selectedQueenTeamId == 2) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("simplified_queen_team2_chip")
                                )

                                // None / Not Covered Option
                                FilterChip(
                                    selected = selectedQueenTeamId == null,
                                    onClick = {
                                        selectedQueenTeamId = null
                                        selectedQueenPlayerId = null
                                    },
                                    label = {
                                        Text(
                                            text = "None",
                                            fontWeight = if (selectedQueenTeamId == null) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    },
                                    leadingIcon = if (selectedQueenTeamId == null) {
                                        { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null,
                                    modifier = Modifier
                                        .weight(0.75f)
                                        .testTag("simplified_queen_none_chip")
                                )
                            }

                            // If a team is selected, show player picker and status notice
                            if (selectedQueenTeamId != null) {
                                val queenTeamPlayers = if (selectedQueenTeamId == 1) config.team1Players else config.team2Players
                                val isWinningTeamQueen = (selectedQueenTeamId == selectedWinningTeamId)

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Select player who pocketed the Queen:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        queenTeamPlayers.forEach { player ->
                                            val isSelected = selectedQueenPlayerId == player.id
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) CarromQueenRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(
                                                    if (isSelected) 1.5.dp else 1.dp,
                                                    if (isSelected) CarromQueenRed else MaterialTheme.colorScheme.outlineVariant
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { selectedQueenPlayerId = player.id }
                                                    .testTag("simplified_queen_player_${player.id}")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    PlayerAvatar(name = player.name, avatarColorIndex = player.avatarColorIndex, size = 22.dp)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = player.name,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 12.sp,
                                                        color = if (isSelected) CarromQueenRed else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Status note
                                if (isWinningTeamQueen) {
                                    if (isQueenStopApplied) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFFFF3E0),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = Color(0xFFE65100),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "24-Point Cutoff active: Winner score is already >= ${config.queenStopThreshold}. No Queen bonus pts added.",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFFE65100)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    val loserName = if (selectedQueenTeamId == 1) config.team1Name else config.team2Name
                                    val winnerName = if (selectedWinningTeamId == 1) config.team1Name else config.team2Name
                                    val qPlayerName = queenTeamPlayers.find { it.id == selectedQueenPlayerId }?.name ?: loserName

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Queen potted by $qPlayerName ($loserName). Since $winnerName won the board, no Queen bonus is awarded (+0 pts). Queen possession stat is credited to $qPlayerName 👑.",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // D. LIVE RESULT CALCULATION BANNER
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isPreviewNillMatchWin) Color(0xFFFCE4EC) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, if (isPreviewNillMatchWin) Color(0xFFF48FB1) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        val winningTeamName = if (selectedWinningTeamId == 1) config.team1Name else config.team2Name
                                        Text(
                                            text = "$winningTeamName gains: +$calculatedBoardScore pts",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = if (isPreviewNillMatchWin) Color(0xFFC2185B) else MaterialTheme.colorScheme.primary
                                        )
                                        val queenCalcNote = when {
                                            selectedQueenTeamId == selectedWinningTeamId && selectedQueenPlayerId != null -> "$queenPointsAwarded (Queen by winner)"
                                            selectedQueenTeamId != null && selectedQueenPlayerId != null -> "0 (Queen by opponent)"
                                            else -> "0 (Queen not covered)"
                                        }
                                        Text(
                                            text = "$opponentCoinsRemaining (coins) + $queenCalcNote",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isPreviewNillMatchWin) Color(0xFFC2185B) else MaterialTheme.colorScheme.surface
                                    ) {
                                        Text(
                                            text = "Match will be: $newPreviewT1Score - $newPreviewT2Score",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isPreviewNillMatchWin) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (isPreviewNillMatchWin) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFC2185B).copy(alpha = 0.12f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FlashOn,
                                                contentDescription = null,
                                                tint = Color(0xFFC2185B),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "NILL BOARD VICTORY: Reached 19+ pts while opponent < 7 pts (Match will end immediately)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF880E4F)
                                            )
                                        }
                                    }
                                } else if (isNillBoardPreview) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "⚠️ Nill Board triggered: Opponent score ($losingTeamScore pts) is under ${config.nillBoardThreshold} pts",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFE65100)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. COMPLETED BOARDS BREAKDOWN
                if (state.completedBoards.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Completed Boards (${state.completedBoards.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = onUndoLastBoard,
                                    modifier = Modifier.testTag("simplified_undo_last_board_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Undo Board", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            state.completedBoards.forEach { br ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "B${br.boardNumber}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${br.winningTeamName} (+${br.boardScore} pts)",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (br.isNillBoard) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFFFFEBEE)
                                                    ) {
                                                        Text(
                                                            text = "⚠️ Nill Board",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFC2185B),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = if (br.queenCoveredByPlayerName != null) {
                                                    val qTeamName = if (br.queenCoveredByTeamId == 1) config.team1Name else config.team2Name
                                                    if (br.queenCoveredByTeamId == br.winningTeamId) {
                                                        "Queen: ${br.queenCoveredByPlayerName} (+${br.queenPointsAwarded} pts) 👑"
                                                    } else {
                                                        "Queen: ${br.queenCoveredByPlayerName} ($qTeamName, 0 pts) 👑"
                                                    }
                                                } else "Queen not covered",
                                                fontSize = 10.sp,
                                                color = if (br.queenCoveredByPlayerName != null) CarromQueenRed else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "${br.team1ScoreAfterBoard} - ${br.team2ScoreAfterBoard}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (br != state.completedBoards.last()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 3.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Space for floating bottom action bar
                Spacer(modifier = Modifier.height(100.dp))
            }

            // Floating Frosted Glass Action Card ("Record Board Score")
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                    border = BorderStroke(
                        1.2.dp,
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.65f),
                                Color.White.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            )
                        )
                    ),
                    shadowElevation = 16.dp,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            ambientColor = Color.Black.copy(alpha = 0.25f)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.22f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Button(
                            onClick = {
                                onRecordBoardScore(
                                    selectedWinningTeamId,
                                    opponentCoinsRemaining,
                                    selectedQueenPlayerId,
                                    selectedQueenTeamId
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                                .testTag("simplified_record_board_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Record Board #$currentBoardNumber Score",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Abandon Dialog
    if (showAbandonDialog) {
        AlertDialog(
            onDismissRequest = { showAbandonDialog = false },
            title = { Text("Exit Current Match?") },
            text = { Text("You can save your match progress or abandon it. Your data won't be lost if you finish.") },
            confirmButton = {
                Button(
                    onClick = {
                        showAbandonDialog = false
                        onFinishAndSaveMatch()
                    },
                    modifier = Modifier.testTag("simplified_save_exit_button")
                ) {
                    Text("Save & Exit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAbandonDialog = false
                        onAbandonMatch()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Abandon")
                }
            }
        )
    }
}
