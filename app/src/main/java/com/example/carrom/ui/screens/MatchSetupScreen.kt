package com.example.carrom.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carrom.data.local.entity.PlayerEntity
import com.example.carrom.engine.Player
import com.example.carrom.ui.components.AvatarPalette
import com.example.carrom.ui.components.PlayerAvatar
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSetupScreen(
    savedPlayers: List<PlayerEntity>,
    onBack: () -> Unit,
    onStartMatch: (
        team1Name: String,
        team2Name: String,
        team1Players: List<Player>,
        team2Players: List<Player>,
        firstBreakerPlayerId: Long,
        proMode: Boolean,
        targetPoints: Int,
        nillBoardThreshold: Int,
        queenPoints: Int,
        queenStopThreshold: Int,
        enableQueenStopRule: Boolean
    ) -> Unit
) {
    var isDoubles by rememberSaveable { mutableStateOf(true) }
    var team1Name by rememberSaveable { mutableStateOf("Team 1") }
    var team2Name by rememberSaveable { mutableStateOf("Team 2") }

    // Selected player names for each slot
    var t1p1Name by rememberSaveable { mutableStateOf(savedPlayers.getOrNull(0)?.name ?: "Player 1") }
    var t1p2Name by rememberSaveable { mutableStateOf(savedPlayers.getOrNull(2)?.name ?: "Player 3") }
    var t2p1Name by rememberSaveable { mutableStateOf(savedPlayers.getOrNull(1)?.name ?: "Player 2") }
    var t2p2Name by rememberSaveable { mutableStateOf(savedPlayers.getOrNull(3)?.name ?: "Player 4") }

    // Breaker Selection: 0 = T1P1, 1 = T2P1, 2 = T1P2, 3 = T2P2
    var selectedBreakerIndex by rememberSaveable { mutableIntStateOf(0) }

    var proMode by rememberSaveable { mutableStateOf(true) }
    var targetPoints by rememberSaveable { mutableIntStateOf(29) }
    var queenStopThreshold by rememberSaveable { mutableIntStateOf(24) }
    var enableQueenStopRule by rememberSaveable { mutableStateOf(true) }
    var showAdvancedRules by rememberSaveable { mutableStateOf(false) }

    var showAddPlayerDialogForSlot by remember { mutableStateOf<Int?>(null) } // 0, 1, 2, 3

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Match Setup", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("match_setup_back_button")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(2.dp))

                // Mode Selector: Singles vs Doubles
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = isDoubles,
                        onClick = {
                            isDoubles = true
                            if (selectedBreakerIndex > 3) selectedBreakerIndex = 0
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Doubles (2v2)", fontWeight = FontWeight.SemiBold)
                    }
                    SegmentedButton(
                        selected = !isDoubles,
                        onClick = {
                            isDoubles = false
                            if (selectedBreakerIndex != 0 && selectedBreakerIndex != 1) {
                                selectedBreakerIndex = 0
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Singles (1v1)", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Quick Saved Players Roster (if any saved players exist)
                if (savedPlayers.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.People,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Saved Players Roster (${savedPlayers.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Tap slot dropdown to assign",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                savedPlayers.forEach { sp ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            PlayerAvatar(name = sp.name, avatarColorIndex = sp.avatarColorIndex, size = 22.dp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = sp.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                if (sp.matchesPlayed > 0) {
                                                    Text(
                                                        text = "${sp.matchesWon}W/${sp.matchesLost}L • ${"%.0f".format(sp.winRate)}%",
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.primary
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

                // Team 1 Section
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TEAM 1",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = team1Name,
                            onValueChange = { team1Name = it },
                            label = { Text("Team 1 Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("team1_name_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PlayerSlotSelector(
                            label = "Player 1",
                            playerName = t1p1Name,
                            savedPlayers = savedPlayers,
                            onSelectPlayer = { t1p1Name = it },
                            onAddNew = { showAddPlayerDialogForSlot = 0 }
                        )

                        AnimatedVisibility(
                            visible = isDoubles,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                PlayerSlotSelector(
                                    label = "Player 3",
                                    playerName = t1p2Name,
                                    savedPlayers = savedPlayers,
                                    onSelectPlayer = { t1p2Name = it },
                                    onAddNew = { showAddPlayerDialogForSlot = 1 }
                                )
                            }
                        }
                    }
                }

                // Team 2 Section
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF57C00))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TEAM 2",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF57C00)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = team2Name,
                            onValueChange = { team2Name = it },
                            label = { Text("Team 2 Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("team2_name_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PlayerSlotSelector(
                            label = "Player 2",
                            playerName = t2p1Name,
                            savedPlayers = savedPlayers,
                            onSelectPlayer = { t2p1Name = it },
                            onAddNew = { showAddPlayerDialogForSlot = 2 }
                        )

                        AnimatedVisibility(
                            visible = isDoubles,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                PlayerSlotSelector(
                                    label = "Player 4",
                                    playerName = t2p2Name,
                                    savedPlayers = savedPlayers,
                                    onSelectPlayer = { t2p2Name = it },
                                    onAddNew = { showAddPlayerDialogForSlot = 3 }
                                )
                            }
                        }
                    }
                }

                // First Breaker Selection
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "FIRST BREAKER",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Breaking team plays White (9 coins), opponent plays Black (9 coins)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val breakerOptions = if (isDoubles) {
                            listOf(
                                Triple(0, t1p1Name, team1Name),
                                Triple(1, t2p1Name, team2Name),
                                Triple(2, t1p2Name, team1Name),
                                Triple(3, t2p2Name, team2Name)
                            )
                        } else {
                            listOf(
                                Triple(0, t1p1Name, team1Name),
                                Triple(1, t2p1Name, team2Name)
                            )
                        }

                        breakerOptions.forEach { (index, name, team) ->
                            val matchedPlayer = savedPlayers.find { it.name.equals(name, true) }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedBreakerIndex == index) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedBreakerIndex = index }
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedBreakerIndex == index,
                                        onClick = { selectedBreakerIndex = index }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    PlayerAvatar(
                                        name = name,
                                        avatarColorIndex = matchedPlayer?.avatarColorIndex ?: index,
                                        size = 28.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "$name ($team)",
                                        fontWeight = if (selectedBreakerIndex == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Match Mode & Rules Selection
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "MATCH RULES",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Pro Match Mode", fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (proMode) "29-pt Target • Unlimited Boards until Target" else "Standard Casual Match (25-pt Target)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = proMode,
                                onCheckedChange = {
                                    proMode = it
                                    targetPoints = if (it) 29 else 25
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Advanced Rules Accordion
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvancedRules = !showAdvancedRules }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Advanced Tournament Rules",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = if (showAdvancedRules) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        AnimatedVisibility(
                            visible = showAdvancedRules,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                // 19-Pt Queen Stop Rule Switch
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("24-Point Queen Cutoff Rule", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Text(
                                            "After reaching 24 points, Queen bonus (+5) is not counted",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = enableQueenStopRule,
                                        onCheckedChange = { enableQueenStopRule = it }
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Target Points Customization
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Target Points: $targetPoints pts", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(21, 25, 29, 33).forEach { pts ->
                                            FilterChip(
                                                selected = targetPoints == pts,
                                                onClick = { targetPoints = pts },
                                                label = { Text("$pts") }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // START MATCH BUTTON
                Button(
                    onClick = {
                        val t1p1 = t1p1Name.trim().ifBlank { "Player 1" }
                        val t1p2 = t1p2Name.trim().ifBlank { "Player 3" }
                        val t2p1 = t2p1Name.trim().ifBlank { "Player 2" }
                        val t2p2 = t2p2Name.trim().ifBlank { "Player 4" }

                        val t1List = if (isDoubles) {
                            listOf(
                                Player(id = savedPlayers.find { it.name.equals(t1p1, true) }?.id ?: 1L, name = t1p1, avatarColorIndex = savedPlayers.find { it.name.equals(t1p1, true) }?.avatarColorIndex ?: 0),
                                Player(id = savedPlayers.find { it.name.equals(t1p2, true) }?.id ?: 3L, name = t1p2, avatarColorIndex = savedPlayers.find { it.name.equals(t1p2, true) }?.avatarColorIndex ?: 2)
                            )
                        } else {
                            listOf(
                                Player(id = savedPlayers.find { it.name.equals(t1p1, true) }?.id ?: 1L, name = t1p1, avatarColorIndex = savedPlayers.find { it.name.equals(t1p1, true) }?.avatarColorIndex ?: 0)
                            )
                        }

                        val t2List = if (isDoubles) {
                            listOf(
                                Player(id = savedPlayers.find { it.name.equals(t2p1, true) }?.id ?: 2L, name = t2p1, avatarColorIndex = savedPlayers.find { it.name.equals(t2p1, true) }?.avatarColorIndex ?: 1),
                                Player(id = savedPlayers.find { it.name.equals(t2p2, true) }?.id ?: 4L, name = t2p2, avatarColorIndex = savedPlayers.find { it.name.equals(t2p2, true) }?.avatarColorIndex ?: 3)
                            )
                        } else {
                            listOf(
                                Player(id = savedPlayers.find { it.name.equals(t2p1, true) }?.id ?: 2L, name = t2p1, avatarColorIndex = savedPlayers.find { it.name.equals(t2p1, true) }?.avatarColorIndex ?: 1)
                            )
                        }

                        val breakerPlayerId = when (selectedBreakerIndex) {
                            0 -> t1List[0].id
                            1 -> t2List[0].id
                            2 -> if (isDoubles) t1List[1].id else t1List[0].id
                            3 -> if (isDoubles) t2List[1].id else t2List[0].id
                            else -> t1List[0].id
                        }

                        onStartMatch(
                            team1Name.ifBlank { "Team 1" },
                            team2Name.ifBlank { "Team 2" },
                            t1List,
                            t2List,
                            breakerPlayerId,
                            proMode,
                            targetPoints,
                            7,
                            5,
                            queenStopThreshold,
                            enableQueenStopRule
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(18.dp),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        .testTag("start_match_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Match", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 0.3.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Isolated Fast Dialog for adding new player into slot
    if (showAddPlayerDialogForSlot != null) {
        SlotPlayerAddModal(
            slotIndex = showAddPlayerDialogForSlot!!,
            onDismiss = { showAddPlayerDialogForSlot = null },
            onSelect = { assignedName ->
                when (showAddPlayerDialogForSlot) {
                    0 -> t1p1Name = assignedName
                    1 -> t1p2Name = assignedName
                    2 -> t2p1Name = assignedName
                    3 -> t2p2Name = assignedName
                }
                showAddPlayerDialogForSlot = null
            }
        )
    }
}

/**
 * Isolated Modal for entering player name in match setup slot without recomposing parent screen on typing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotPlayerAddModal(
    slotIndex: Int,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var playerNameInput by rememberSaveable { mutableStateOf("") }
    var playerNicknameInput by rememberSaveable { mutableStateOf("") }
    var selectedColorIndex by rememberSaveable { mutableIntStateOf(slotIndex % AvatarPalette.size) }
    var nameError by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(120)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                PlayerAvatar(
                    name = playerNameInput.ifBlank { "P" },
                    avatarColorIndex = selectedColorIndex,
                    size = 44.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Enter Player Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Assign player to match slot", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            OutlinedTextField(
                value = playerNameInput,
                onValueChange = {
                    playerNameInput = it
                    if (nameError && it.isNotBlank()) nameError = false
                },
                label = { Text("Player Name *") },
                placeholder = { Text("e.g. Anand Kumar") },
                singleLine = true,
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("Please enter a player name", color = MaterialTheme.colorScheme.error) }
                } else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("add_player_dialog_name_input")
            )

            OutlinedTextField(
                value = playerNicknameInput,
                onValueChange = { playerNicknameInput = it },
                label = { Text("Nickname (Optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Avatar Color Theme", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarPalette.forEachIndexed { index, color ->
                    val isSelected = selectedColorIndex == index
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1f,
                        animationSpec = spring(dampingRatio = 0.6f),
                        label = "slot_color_scale"
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { selectedColorIndex = index }
                            .then(
                                if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val trimmed = playerNameInput.trim()
                        if (trimmed.isBlank()) {
                            nameError = true
                        } else {
                            onSelect(trimmed)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("confirm_add_player_button")
                ) {
                    Text("Assign to Slot", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PlayerSlotSelector(
    label: String,
    playerName: String,
    savedPlayers: List<PlayerEntity>,
    onSelectPlayer: (String) -> Unit,
    onAddNew: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val matchedPlayer = savedPlayers.find { it.name.equals(playerName, true) }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    PlayerAvatar(
                        name = playerName,
                        avatarColorIndex = matchedPlayer?.avatarColorIndex ?: 0,
                        size = 28.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = playerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (matchedPlayer?.nickname?.isNotBlank() == true) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(${matchedPlayer.nickname})",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (matchedPlayer != null && matchedPlayer.matchesPlayed > 0) {
                            Text(
                                text = "Win Rate: ${"%.0f".format(matchedPlayer.winRate)}% (${matchedPlayer.matchesWon}W/${matchedPlayer.matchesLost}L)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "Assigned player",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (savedPlayers.isNotEmpty()) {
                    Text(
                        text = "  Select Saved Player",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    savedPlayers.forEach { player ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PlayerAvatar(name = player.name, avatarColorIndex = player.avatarColorIndex, size = 24.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(player.name, fontWeight = FontWeight.Bold)
                                        if (player.nickname.isNotBlank()) {
                                            Text(player.nickname, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            },
                            trailingIcon = {
                                if (player.matchesPlayed > 0) {
                                    Text(
                                        "${"%.0f".format(player.winRate)}% WR",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = {
                                onSelectPlayer(player.name)
                                expanded = false
                            }
                        )
                    }
                    HorizontalDivider()
                }

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enter / Add New Name", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    },
                    onClick = {
                        expanded = false
                        onAddNew()
                    }
                )
            }
        }
    }
}
