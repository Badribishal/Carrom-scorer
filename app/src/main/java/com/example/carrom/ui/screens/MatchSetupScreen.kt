package com.example.carrom.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carrom.data.local.entity.PlayerEntity
import com.example.carrom.engine.Player
import com.example.carrom.ui.components.AvatarPalette
import com.example.carrom.ui.components.PlayerAvatar
import com.example.ui.theme.CarromWhiteCoin
import com.example.ui.theme.WoodMedium

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
    var isDoubles by remember { mutableStateOf(true) }
    var team1Name by remember { mutableStateOf("Team 1") }
    var team2Name by remember { mutableStateOf("Team 2") }

    var t1p1Name by remember { mutableStateOf(savedPlayers.getOrNull(0)?.name ?: "Player 1") }
    var t1p2Name by remember { mutableStateOf(savedPlayers.getOrNull(2)?.name ?: "Player 3") }
    var t2p1Name by remember { mutableStateOf(savedPlayers.getOrNull(1)?.name ?: "Player 2") }
    var t2p2Name by remember { mutableStateOf(savedPlayers.getOrNull(3)?.name ?: "Player 4") }

    // Breaker Selection: index 0 = T1P1 (Player 1), 1 = T2P1 (Player 2), 2 = T1P2 (Player 3), 3 = T2P2 (Player 4)
    var selectedBreakerIndex by remember { mutableIntStateOf(0) }

    var proMode by remember { mutableStateOf(true) }
    var targetPoints by remember { mutableIntStateOf(29) }
    var queenStopThreshold by remember { mutableIntStateOf(19) }
    var enableQueenStopRule by remember { mutableStateOf(true) }
    var showAdvancedRules by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var showAddPlayerDialogForSlot by remember { mutableStateOf<Int?>(null) } // 0, 1, 2, 3
    var newPlayerInputName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Match Setup", fontWeight = FontWeight.Bold) },
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
            Spacer(modifier = Modifier.height(4.dp))

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

            // Team 1 Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TEAM 1",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = team1Name,
                        onValueChange = { team1Name = it },
                        label = { Text("Team 1 Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
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
                        onAddNew = {
                            showAddPlayerDialogForSlot = 0
                            newPlayerInputName = ""
                        }
                    )

                    if (isDoubles) {
                        Spacer(modifier = Modifier.height(10.dp))
                        PlayerSlotSelector(
                            label = "Player 3",
                            playerName = t1p2Name,
                            savedPlayers = savedPlayers,
                            onSelectPlayer = { t1p2Name = it },
                            onAddNew = {
                                showAddPlayerDialogForSlot = 1
                                newPlayerInputName = ""
                            }
                        )
                    }
                }
            }

            // Team 2 Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TEAM 2",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = team2Name,
                        onValueChange = { team2Name = it },
                        label = { Text("Team 2 Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
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
                        onAddNew = {
                            showAddPlayerDialogForSlot = 2
                            newPlayerInputName = ""
                        }
                    )

                    if (isDoubles) {
                        Spacer(modifier = Modifier.height(10.dp))
                        PlayerSlotSelector(
                            label = "Player 4",
                            playerName = t2p2Name,
                            savedPlayers = savedPlayers,
                            onSelectPlayer = { t2p2Name = it },
                            onAddNew = {
                                showAddPlayerDialogForSlot = 3
                                newPlayerInputName = ""
                            }
                        )
                    }
                }
            }

            // First Breaker Selection
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedBreakerIndex = index }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedBreakerIndex == index,
                                onClick = { selectedBreakerIndex = index }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            PlayerAvatar(name = name, avatarColorIndex = index, size = 28.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "$name ($team)",
                                fontWeight = if (selectedBreakerIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Pro Mode & Rule Toggles
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PRO MODE",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Detailed turn-by-turn logs, coin stats & break analytics",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = proMode,
                            onCheckedChange = { proMode = it },
                            modifier = Modifier.testTag("pro_mode_switch")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvancedRules = !showAdvancedRules },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Match Rules & Target",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = if (showAdvancedRules) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    if (showAdvancedRules) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Winning Target Points")
                            Text(
                                text = "$targetPoints pts",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 19-Point Rule (Standard Carrom Regulation)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("19-Point Queen Rule (Official ICF)", fontWeight = FontWeight.Medium)
                                Text(
                                    "No Queen bonus points (+5) awarded once team score reaches >= 19 points",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = enableQueenStopRule,
                                onCheckedChange = { enableQueenStopRule = it },
                                modifier = Modifier.testTag("queen_stop_rule_switch")
                            )
                        }

                        if (enableQueenStopRule) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Queen Stop Threshold", fontSize = 12.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = queenStopThreshold == 19,
                                        onClick = { queenStopThreshold = 19 },
                                        label = { Text("19 pts (Official)") }
                                    )
                                    FilterChip(
                                        selected = queenStopThreshold == 24,
                                        onClick = { queenStopThreshold = 24 },
                                        label = { Text("24 pts") }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom scroll margin so content is fully accessible above floating frosted bar
            Spacer(modifier = Modifier.height(110.dp))
        }

        // Floating Frosted Glass Card for Start Match Action
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
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
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        Button(
                            onClick = {
                                val t1p1 = t1p1Name.trim()
                                val t1p2 = t1p2Name.trim()
                                val t2p1 = t2p1Name.trim()
                                val t2p2 = t2p2Name.trim()

                                if (t1p1.isBlank() || t2p1.isBlank() || (isDoubles && (t1p2.isBlank() || t2p2.isBlank()))) {
                                    errorMessage = "Please enter valid names for all players."
                                    return@Button
                                }

                                val t1List = if (isDoubles) {
                                    listOf(
                                        Player(id = savedPlayers.find { it.name.equals(t1p1, true) }?.id ?: 1L, name = t1p1, avatarColorIndex = 0),
                                        Player(id = savedPlayers.find { it.name.equals(t1p2, true) }?.id ?: 3L, name = t1p2, avatarColorIndex = 2)
                                    )
                                } else {
                                    listOf(
                                        Player(id = savedPlayers.find { it.name.equals(t1p1, true) }?.id ?: 1L, name = t1p1, avatarColorIndex = 0)
                                    )
                                }

                                val t2List = if (isDoubles) {
                                    listOf(
                                        Player(id = savedPlayers.find { it.name.equals(t2p1, true) }?.id ?: 2L, name = t2p1, avatarColorIndex = 1),
                                        Player(id = savedPlayers.find { it.name.equals(t2p2, true) }?.id ?: 4L, name = t2p2, avatarColorIndex = 3)
                                    )
                                } else {
                                    listOf(
                                        Player(id = savedPlayers.find { it.name.equals(t2p1, true) }?.id ?: 2L, name = t2p1, avatarColorIndex = 1)
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
                    }
                }
            }
        }
    }
}

    // Add Player Dialog
    if (showAddPlayerDialogForSlot != null) {
        AlertDialog(
            onDismissRequest = {
                keyboardController?.hide()
                focusManager.clearFocus()
                showAddPlayerDialogForSlot = null
            },
            title = { Text("Add New Player") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPlayerInputName,
                        onValueChange = { newPlayerInputName = it },
                        label = { Text("Player Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newPlayerInputName.isNotBlank()) {
                                when (showAddPlayerDialogForSlot) {
                                    0 -> t1p1Name = newPlayerInputName.trim()
                                    1 -> t1p2Name = newPlayerInputName.trim()
                                    2 -> t2p1Name = newPlayerInputName.trim()
                                    3 -> t2p2Name = newPlayerInputName.trim()
                                }
                                showAddPlayerDialogForSlot = null
                            }
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_player_dialog_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlayerInputName.isNotBlank()) {
                            when (showAddPlayerDialogForSlot) {
                                0 -> t1p1Name = newPlayerInputName.trim()
                                1 -> t1p2Name = newPlayerInputName.trim()
                                2 -> t2p1Name = newPlayerInputName.trim()
                                3 -> t2p2Name = newPlayerInputName.trim()
                            }
                            showAddPlayerDialogForSlot = null
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_player_button")
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlayerDialogForSlot = null }) {
                    Text("Cancel")
                }
            }
        )
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

    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .weight(1f)
                    .clickable { expanded = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlayerAvatar(name = playerName, size = 26.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = playerName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    savedPlayers.forEach { player ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PlayerAvatar(name = player.name, avatarColorIndex = player.avatarColorIndex, size = 22.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(player.name)
                                }
                            },
                            onClick = {
                                onSelectPlayer(player.name)
                                expanded = false
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add New Player", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
}
