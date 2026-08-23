package com.example.carrom.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carrom.data.local.entity.PlayerEntity
import com.example.carrom.ui.components.AvatarPalette
import com.example.carrom.ui.components.PlayerAvatar
import com.example.carrom.ui.components.QueenCoinBadge
import com.example.ui.theme.CarromQueenRed
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerStatsScreen(
    players: List<PlayerEntity>,
    onAddNewPlayer: (name: String, colorIndex: Int, nickname: String, notes: String, skillLevel: String) -> Unit,
    onUpdatePlayer: (PlayerEntity) -> Unit = {},
    onDeletePlayer: (Long) -> Unit = {},
    onExportPlayers: () -> Unit = {},
    onBack: () -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedPlayer by remember { mutableStateOf<PlayerEntity?>(null) }
    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var playerToEdit by remember { mutableStateOf<PlayerEntity?>(null) }
    var playerToDelete by remember { mutableStateOf<PlayerEntity?>(null) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val filteredPlayers = remember(players, searchQuery) {
        if (searchQuery.isBlank()) players
        else players.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.nickname.contains(searchQuery, ignoreCase = true) ||
            it.skillLevel.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Player Profiles & Stats",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("player_stats_back_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (players.isNotEmpty()) {
                        IconButton(
                            onClick = onExportPlayers,
                            modifier = Modifier.testTag("export_player_stats_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export Player Stats",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = { showAddPlayerDialog = true },
                        modifier = Modifier.testTag("add_player_top_button")
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Player")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPlayerDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .testTag("add_player_fab")
                    .animateContentSize()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Player", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, nickname, or rank...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = searchQuery.isNotEmpty(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_search_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(
                targetState = filteredPlayers.isEmpty(),
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "players_list_content"
            ) { isEmpty ->
                if (isEmpty) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.75f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PeopleOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (players.isEmpty()) "No saved players yet" else "No players match your search",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (players.isEmpty()) "Tap '+ Add Player' below to register player profiles with custom colors and stats." else "Try searching with a different name or keyword.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (players.isEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showAddPlayerDialog = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add First Player")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredPlayers, key = { it.id }) { player ->
                            PlayerCard(
                                player = player,
                                onClick = { selectedPlayer = player }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(84.dp)) }
                    }
                }
            }
        }
    }

    // Separate Add Player Dialog (Fully encapsulated state for zero typing lag)
    if (showAddPlayerDialog) {
        AddPlayerFastModal(
            onDismiss = { showAddPlayerDialog = false },
            onConfirm = { name, colorIdx, nickname, notes, skill ->
                onAddNewPlayer(name, colorIdx, nickname, notes, skill)
                showAddPlayerDialog = false
            }
        )
    }

    // Player Detail Sheet
    if (selectedPlayer != null) {
        PlayerDetailBottomSheet(
            player = selectedPlayer!!,
            onDismiss = { selectedPlayer = null },
            onEdit = {
                playerToEdit = it
                selectedPlayer = null
            },
            onDelete = {
                playerToDelete = it
                selectedPlayer = null
            }
        )
    }

    // Edit Player Dialog (Encapsulated state)
    if (playerToEdit != null) {
        EditPlayerFastModal(
            player = playerToEdit!!,
            onDismiss = { playerToEdit = null },
            onConfirm = { updated ->
                onUpdatePlayer(updated)
                playerToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (playerToDelete != null) {
        AlertDialog(
            onDismissRequest = { playerToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Player Profile") },
            text = {
                Text("Are you sure you want to delete ${playerToDelete?.name}? Match records and stats history will be retained.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        playerToDelete?.let { onDeletePlayer(it.id) }
                        playerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { playerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Encapsulated fast modal for adding a player.
 * Keystrokes are isolated to this sub-tree to ensure zero frame-drops.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPlayerFastModal(
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorIndex: Int, nickname: String, notes: String, skillLevel: String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var skillLevel by rememberSaveable { mutableStateOf("Intermediate") }
    var selectedColorIndex by rememberSaveable { mutableIntStateOf(0) }
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
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
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
                    name = name.ifBlank { "P" },
                    avatarColorIndex = selectedColorIndex,
                    size = 48.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Add New Player",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Save player for instant match selection & stats tracking",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Player Full Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (nameError && it.isNotBlank()) nameError = false
                },
                label = { Text("Player Full Name *") },
                placeholder = { Text("e.g. Rahul Sharma") },
                singleLine = true,
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("Player name cannot be blank", color = MaterialTheme.colorScheme.error) }
                } else null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("add_player_dialog_name_input")
            )

            // Nickname / Title
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Nickname / Title (Optional)") },
                placeholder = { Text("e.g. Ace Striker, The Finisher") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Skill Level Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Skill Level",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Beginner", "Intermediate", "Advanced", "Master").forEach { skill ->
                        val isSelected = skillLevel == skill
                        FilterChip(
                            selected = isSelected,
                            onClick = { skillLevel = skill },
                            label = { Text(skill, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Avatar Color Palette
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Avatar Color Theme",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                            label = "color_scale"
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
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
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Notes / Style
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Playing Style / Notes (Optional)") },
                placeholder = { Text("e.g. Aggressive thumb flick, White specialist") },
                maxLines = 2,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isBlank()) {
                            nameError = true
                        } else {
                            onConfirm(trimmed, selectedColorIndex, nickname.trim(), notes.trim(), skillLevel)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("confirm_add_player_button")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Player", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Encapsulated fast modal for editing a player.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPlayerFastModal(
    player: PlayerEntity,
    onDismiss: () -> Unit,
    onConfirm: (PlayerEntity) -> Unit
) {
    var editName by rememberSaveable { mutableStateOf(player.name) }
    var editNickname by rememberSaveable { mutableStateOf(player.nickname) }
    var editSkill by rememberSaveable { mutableStateOf(player.skillLevel) }
    var editNotes by rememberSaveable { mutableStateOf(player.notes) }
    var editColorIndex by rememberSaveable { mutableIntStateOf(player.avatarColorIndex) }
    var nameError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
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
                    name = editName.ifBlank { "P" },
                    avatarColorIndex = editColorIndex,
                    size = 48.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text("Edit Player Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Update player details and visual theme", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            OutlinedTextField(
                value = editName,
                onValueChange = {
                    editName = it
                    if (nameError && it.isNotBlank()) nameError = false
                },
                label = { Text("Player Name *") },
                singleLine = true,
                isError = nameError,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = editNickname,
                onValueChange = { editNickname = it },
                label = { Text("Nickname / Title") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Skill Level
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Skill Level", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Beginner", "Intermediate", "Advanced", "Master").forEach { skill ->
                        val isSelected = editSkill == skill
                        FilterChip(
                            selected = isSelected,
                            onClick = { editSkill = skill },
                            label = { Text(skill, fontSize = 11.sp) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Colors
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Avatar Color Theme", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarPalette.forEachIndexed { index, color ->
                        val isSelected = editColorIndex == index
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { editColorIndex = index }
                                .then(
                                    if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = editNotes,
                onValueChange = { editNotes = it },
                label = { Text("Notes / Style") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val name = editName.trim()
                        if (name.isBlank()) {
                            nameError = true
                        } else {
                            onConfirm(
                                player.copy(
                                    name = name,
                                    nickname = editNickname.trim(),
                                    skillLevel = editSkill,
                                    notes = editNotes.trim(),
                                    avatarColorIndex = editColorIndex
                                )
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                ) {
                    Text("Update Profile", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerDetailBottomSheet(
    player: PlayerEntity,
    onDismiss: () -> Unit,
    onEdit: (PlayerEntity) -> Unit,
    onDelete: (PlayerEntity) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                PlayerAvatar(name = player.name, avatarColorIndex = player.avatarColorIndex, size = 56.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                        if (player.nickname.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = player.nickname,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Win Rate: ${"%.1f".format(player.winRate)}% (${player.matchesWon}W / ${player.matchesLost}L)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Skill Level: ${player.skillLevel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { onEdit(player) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Player")
                }
                IconButton(onClick = { onDelete(player) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Player", tint = MaterialTheme.colorScheme.error)
                }
            }

            if (player.notes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Notes,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = player.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Performance Gauges
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Performance Gauges", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    // Win Rate Bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Match Win Rate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${"%.1f".format(player.winRate)}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (player.winRate / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }

                    // Queen Conversion Bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Queen Cover Success", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${player.queensCovered} covered", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CarromQueenRed)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (player.queenSuccessRate / 100f).coerceIn(0f, 1f) },
                            color = CarromQueenRed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }

            Text(
                text = "Career Performance Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Stats Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailStatBox("Matches", "${player.matchesPlayed}", Icons.Default.SportsEsports, Modifier.weight(1f))
                    DetailStatBox("Boards Won", "${player.boardsWon}/${player.boardsPlayed}", Icons.Default.Dashboard, Modifier.weight(1f))
                    DetailStatBox("Hands", "${player.handsPlayed}", Icons.Default.PanTool, Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailStatBox("White Coins", "${player.whitePocketed}", Icons.Default.Circle, Modifier.weight(1f))
                    DetailStatBox("Black Coins", "${player.blackPocketed}", Icons.Default.Circle, Modifier.weight(1f))
                    DetailStatBox("Total Coins", "${player.totalCoinsPocketed}", Icons.Default.Savings, Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailStatBox("Queens Covered", "${player.queensCovered}", Icons.Default.Stars, Modifier.weight(1f), isQueen = true)
                    DetailStatBox("Queen Points", "+${player.queenPointsScored}", Icons.Default.AddCircle, Modifier.weight(1f))
                    DetailStatBox("Penalties", "${player.penalties}", Icons.Default.Warning, Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailStatBox("Nill Wins", "${player.nillBoardWins}", Icons.Default.CheckCircle, Modifier.weight(1f))
                    DetailStatBox("Nill Losses", "${player.nillBoardLosses}", Icons.Default.Cancel, Modifier.weight(1f))
                    DetailStatBox("Total Points", "${player.totalPointsContributed}", Icons.Default.EmojiEvents, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PlayerCard(
    player: PlayerEntity,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("player_card_${player.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerAvatar(name = player.name, avatarColorIndex = player.avatarColorIndex, size = 44.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = player.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (player.nickname.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${player.nickname})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${player.matchesPlayed} Matches • ${player.matchesWon}W / ${player.matchesLost}L (${"%.0f".format(player.winRate)}%) • ${player.skillLevel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (player.queensCovered > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFEBEE)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            QueenCoinBadge(size = 14.dp, isCovered = true)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${player.queensCovered}",
                                fontWeight = FontWeight.Bold,
                                color = CarromQueenRed,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetailStatBox(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isQueen: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isQueen) CarromQueenRed else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
