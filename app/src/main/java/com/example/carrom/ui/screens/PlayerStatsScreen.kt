package com.example.carrom.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carrom.data.local.entity.GroupEntity
import com.example.carrom.data.local.entity.PlayerEntity
import com.example.carrom.engine.*
import com.example.carrom.ui.components.AvatarPalette
import com.example.carrom.ui.components.PlayerAvatar
import com.example.carrom.ui.components.PlayerTrajectoryLineGraph
import com.example.carrom.ui.components.QueenCoinBadge
import com.example.ui.theme.CarromQueenRed
import java.text.SimpleDateFormat
import java.util.*

enum class PlayerSortOption(val displayName: String) {
    LEVEL_XP("Level & XP"),
    WINS("Most Wins"),
    WIN_RATE("Win Rate %"),
    COINS("Total Coins"),
    QUEENS("Queens Covered"),
    NAME("Name (A-Z)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerStatsScreen(
    players: List<PlayerEntity>,
    groups: List<GroupEntity> = emptyList(),
    initialGroupFilter: String? = null,
    onAddNewPlayer: (name: String, colorIndex: Int, nickname: String, notes: String, skillLevel: String, groupName: String) -> Unit,
    onUpdatePlayer: (PlayerEntity) -> Unit = {},
    onDeletePlayer: (Long) -> Unit = {},
    onAddGroup: (name: String, description: String, colorIndex: Int) -> Unit = { _, _, _ -> },
    onExportPlayers: () -> Unit = {},
    onBack: () -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedGroupFilter by rememberSaveable { mutableStateOf(initialGroupFilter ?: "ALL") }
    var sortOption by rememberSaveable { mutableStateOf(PlayerSortOption.LEVEL_XP) }
    var selectedPlayerForDeepDive by remember { mutableStateOf<PlayerEntity?>(null) }
    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var playerToEdit by remember { mutableStateOf<PlayerEntity?>(null) }
    var playerToDelete by remember { mutableStateOf<PlayerEntity?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Extract all group names available
    val availableGroupNames = remember(groups, players) {
        val names = linkedSetOf<String>()
        groups.forEach { if (it.name.isNotBlank()) names.add(it.name) }
        players.forEach {
            if (it.groupName.isNotBlank() && !it.groupName.equals("General", ignoreCase = true)) {
                names.add(it.groupName)
            }
        }
        if (names.isEmpty()) {
            names.add("General")
        }
        names.toList()
    }

    // Filter and sort players
    val processedPlayers = remember(players, searchQuery, selectedGroupFilter, sortOption) {
        var list = players

        // Group filtering (supports both primary groupName and multi-group membership)
        if (selectedGroupFilter != "ALL") {
            list = list.filter {
                it.groupName.equals(selectedGroupFilter, ignoreCase = true) ||
                groups.find { g -> g.name.equals(selectedGroupFilter, ignoreCase = true) }?.hasMember(it.id) == true
            }
        }

        // Search filtering
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.nickname.contains(searchQuery, ignoreCase = true) ||
                it.groupName.contains(searchQuery, ignoreCase = true) ||
                it.skillLevel.contains(searchQuery, ignoreCase = true)
            }
        }

        // Sorting
        when (sortOption) {
            PlayerSortOption.LEVEL_XP -> list.sortedByDescending { PlayerLevelSystem.calculatePlayerXp(it).totalXp }
            PlayerSortOption.WINS -> list.sortedByDescending { it.matchesWon }
            PlayerSortOption.WIN_RATE -> list.sortedWith(compareByDescending<PlayerEntity> { it.winRate }.thenByDescending { it.matchesPlayed })
            PlayerSortOption.COINS -> list.sortedByDescending { it.totalCoinsPocketed }
            PlayerSortOption.QUEENS -> list.sortedByDescending { it.queensCovered }
            PlayerSortOption.NAME -> list.sortedBy { it.name.lowercase(Locale.ROOT) }
        }
    }

    // Top player (MVP)
    val topPlayer = remember(players) {
        players.maxByOrNull { PlayerLevelSystem.calculatePlayerXp(it).totalXp }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Player Statistics & Levels",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "${players.size} Players • Levels & Achievements",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                                contentDescription = "Export Stats",
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
            ExtendedFloatingActionButton(
                onClick = { showAddPlayerDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("add_player_fab")
            ) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Player", fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // OVERVIEW HERO METRICS (COMPACT & MINIMAL)
            if (players.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MilitaryTech,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (topPlayer != null) "MVP: ${topPlayer.name} (Lvl ${PlayerLevelSystem.calculatePlayerXp(topPlayer).level})" else "${players.size} Players",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${players.sumOf { it.matchesWon }}W • ${players.sumOf { it.totalCoinsPocketed }} Coins • ${players.sumOf { it.queensCovered }} ♛",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // SEARCH BAR AND SORT DROPDOWN
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search name, group, skill...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("player_search_field")
                )

                // SORT BUTTON & MENU
                Box {
                    FilledTonalIconButton(
                        onClick = { showSortMenu = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("sort_players_button")
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort Options")
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        Text(
                            text = "Sort Players By",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                        PlayerSortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (sortOption == option) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        } else {
                                            Spacer(modifier = Modifier.width(22.dp))
                                        }
                                        Text(option.displayName)
                                    }
                                },
                                onClick = {
                                    sortOption = option
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // GROUP FILTER CHIP ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "ALL" Chip
                FilterChip(
                    selected = selectedGroupFilter == "ALL",
                    onClick = { selectedGroupFilter = "ALL" },
                    label = {
                        Text(
                            "All Players (${players.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedGroupFilter == "ALL") FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(14.dp))
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("filter_all_groups")
                )

                // Chips for each group
                availableGroupNames.forEach { groupName ->
                    val isSelected = selectedGroupFilter.equals(groupName, ignoreCase = true)
                    val count = players.count { it.groupName.equals(groupName, ignoreCase = true) }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedGroupFilter = groupName },
                        label = {
                            Text(
                                "$groupName ($count)",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("filter_group_$groupName")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // PLAYERS ROSTER LIST OR EMPTY STATE
            if (processedPlayers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PersonSearch,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (players.isEmpty()) "No Players Registered Yet" else "No Players Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (players.isEmpty()) "Tap '+ Add Player' below to register players, assign groups, and track levels & achievements." else "Try clearing your search query or selecting 'All Players'.",
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(processedPlayers, key = { it.id }) { player ->
                        RedesignedPlayerCard(
                            player = player,
                            groups = groups,
                            onClick = { selectedPlayerForDeepDive = player },
                            onEdit = { playerToEdit = player },
                            onDelete = { playerToDelete = player }
                        )
                    }
                }
            }
        }
    }

    // ADD PLAYER DIALOG (PLAYER DATA ENTRY WITH GROUP SELECTION)
    if (showAddPlayerDialog) {
        AddEditPlayerBottomSheet(
            playerToEdit = null,
            availableGroups = availableGroupNames,
            defaultGroup = if (selectedGroupFilter != "ALL") selectedGroupFilter else "General",
            onDismiss = { showAddPlayerDialog = false },
            onAddGroup = onAddGroup,
            onConfirm = { name, colorIdx, nickname, notes, skillLevel, groupName ->
                onAddNewPlayer(name, colorIdx, nickname, notes, skillLevel, groupName)
                showAddPlayerDialog = false
            }
        )
    }

    // EDIT PLAYER DIALOG
    playerToEdit?.let { player ->
        AddEditPlayerBottomSheet(
            playerToEdit = player,
            availableGroups = availableGroupNames,
            defaultGroup = player.groupName,
            onDismiss = { playerToEdit = null },
            onAddGroup = onAddGroup,
            onConfirm = { name, colorIdx, nickname, notes, skillLevel, groupName ->
                onUpdatePlayer(
                    player.copy(
                        name = name,
                        avatarColorIndex = colorIdx,
                        nickname = nickname,
                        notes = notes,
                        skillLevel = skillLevel,
                        groupName = groupName
                    )
                )
                playerToEdit = null
            }
        )
    }

    // DELETE PLAYER DIALOG
    playerToDelete?.let { player ->
        AlertDialog(
            onDismissRequest = { playerToDelete = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Player Profile") },
            text = {
                Text("Are you sure you want to permanently delete \"${player.name}\"? Career stats, levels, and achievements for this player will be removed.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePlayer(player.id)
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

    // DETAILED PLAYER DEEP DIVE MODAL (LEVELS, ACHIEVEMENTS & STATS)
    selectedPlayerForDeepDive?.let { player ->
        PlayerDeepDiveSheet(
            player = player,
            groups = groups,
            onDismiss = { selectedPlayerForDeepDive = null },
            onEdit = {
                selectedPlayerForDeepDive = null
                playerToEdit = player
            }
        )
    }
}

@Composable
private fun StatMiniBadge(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Minimal & Compact Player Card showing Name, Level, Tier, and Key Stats in a space-saving layout.
 */
@Composable
private fun RedesignedPlayerCard(
    player: PlayerEntity,
    groups: List<GroupEntity> = emptyList(),
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val levelInfo = remember(player) { PlayerLevelSystem.calculatePlayerXp(player) }

    val playerGroups = remember(player, groups) {
        val list = mutableListOf<String>()
        groups.forEach { g ->
            if (g.hasMember(player.id) && !list.contains(g.name)) {
                list.add(g.name)
            }
        }
        if (player.groupName.isNotBlank() && !player.groupName.equals("General", true) && !list.contains(player.groupName)) {
            list.add(player.groupName)
        }
        list
    }

    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        shadowElevation = 0.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("player_card_${player.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Minimal Avatar with Level Badge
            Box {
                PlayerAvatar(
                    name = player.name,
                    avatarColorIndex = player.avatarColorIndex,
                    size = 38.dp
                )
                Surface(
                    shape = CircleShape,
                    color = levelInfo.tier.color,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${levelInfo.level}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Player Info (Compact 2-line layout)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = player.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Tier & Level Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = levelInfo.tier.color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Lvl ${levelInfo.level} • ${levelInfo.tier.title}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = levelInfo.tier.color,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    // Group Badge (if any)
                    if (playerGroups.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = playerGroups.first(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Single compact stats line: Wins/Played (Win%), Coins, Queens, Total XP
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${player.matchesWon}W/${player.matchesPlayed}M (${"%.0f".format(player.winRate)}%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (player.matchesWon > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = "${player.totalCoinsPocketed} Coins",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        QueenCoinBadge(size = 11.dp, isCovered = player.queensCovered > 0)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${player.queensCovered}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (player.queensCovered > 0) CarromQueenRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = "${levelInfo.totalXp} XP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = levelInfo.tier.color
                    )
                }
            }

            // More Options Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("View Full Deep-Dive", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            showMenu = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Player Profile", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Player", color = MaterialTheme.colorScheme.error, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Redesigned Add/Edit Player Modal Bottom Sheet with First-Class Group Selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditPlayerBottomSheet(
    playerToEdit: PlayerEntity?,
    availableGroups: List<String>,
    defaultGroup: String,
    onDismiss: () -> Unit,
    onAddGroup: (name: String, description: String, colorIndex: Int) -> Unit,
    onConfirm: (name: String, colorIndex: Int, nickname: String, notes: String, skillLevel: String, groupName: String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(playerToEdit?.name ?: "") }
    var nickname by rememberSaveable { mutableStateOf(playerToEdit?.nickname ?: "") }
    var notes by rememberSaveable { mutableStateOf(playerToEdit?.notes ?: "") }
    var skillLevel by rememberSaveable { mutableStateOf(playerToEdit?.skillLevel ?: "Intermediate") }
    var selectedColorIndex by rememberSaveable { mutableStateOf(playerToEdit?.avatarColorIndex ?: 0) }
    var selectedGroupName by rememberSaveable { mutableStateOf(playerToEdit?.groupName ?: defaultGroup) }
    var nameError by rememberSaveable { mutableStateOf(false) }
    var showQuickNewGroupDialog by remember { mutableStateOf(false) }

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
                        text = if (playerToEdit == null) "Add New Player" else "Edit Player Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Assign player to a group for quick match entry & stats tracking",
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

            // GROUP SELECTION SECTION
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Assign to Group *",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(
                        onClick = { showQuickNewGroupDialog = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Group", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Group Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableGroups.forEach { groupName ->
                        val isSelected = selectedGroupName.equals(groupName, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedGroupName = groupName },
                            label = { Text(groupName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = {
                                Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

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
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColorIndex = index }
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Notes / Biography
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Tournament History") },
                placeholder = { Text("e.g. Right hand break specialist, club captain") },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isBlank()) {
                        nameError = true
                    } else {
                        onConfirm(
                            trimmedName,
                            selectedColorIndex,
                            nickname.trim(),
                            notes.trim(),
                            skillLevel,
                            selectedGroupName.ifBlank { "General" }
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_add_player_button")
            ) {
                Text(
                    text = if (playerToEdit == null) "Save Player to Group" else "Update Player Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }

    // Quick Dialog to create new group on the fly inside player data entry
    if (showQuickNewGroupDialog) {
        var newGroupNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showQuickNewGroupDialog = false },
            title = { Text("Create New Group") },
            text = {
                OutlinedTextField(
                    value = newGroupNameInput,
                    onValueChange = { newGroupNameInput = it },
                    label = { Text("Group Name") },
                    placeholder = { Text("e.g. Club Alpha, Family") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newGroupNameInput.trim()
                        if (trimmed.isNotBlank()) {
                            onAddGroup(trimmed, "", 0)
                            selectedGroupName = trimmed
                            showQuickNewGroupDialog = false
                        }
                    }
                ) {
                    Text("Add Group")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickNewGroupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Detailed Player Deep-Dive Modal Bottom Sheet:
 * Shows Level XP Breakdown, Unlockable Achievements, and Match Performance Metrics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerDeepDiveSheet(
    player: PlayerEntity,
    groups: List<GroupEntity> = emptyList(),
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val levelInfo = remember(player) { PlayerLevelSystem.calculatePlayerXp(player) }
    val achievements = remember(player) { PlayerLevelSystem.getPlayerAchievements(player) }

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var selectedAchievementCategory by rememberSaveable { mutableStateOf(AchievementCategory.ALL) }

    val filteredAchievements = remember(achievements, selectedAchievementCategory) {
        if (selectedAchievementCategory == AchievementCategory.ALL) achievements
        else achievements.filter { it.category == selectedAchievementCategory }
    }

    val unlockedCount = remember(achievements) { achievements.count { it.isUnlocked } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // HERO HEADER
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box {
                    PlayerAvatar(name = player.name, avatarColorIndex = player.avatarColorIndex, size = 56.dp)
                    Surface(
                        shape = CircleShape,
                        color = levelInfo.tier.color,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${levelInfo.level}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = levelInfo.tier.color.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Lvl ${levelInfo.level} • ${levelInfo.title}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = levelInfo.tier.color,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (player.groupName.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = player.groupName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // TAB ROW
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Levels & Badges", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Performance", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Profile & Notes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // TAB CONTENT
            when (selectedTab) {
                0 -> {
                    // LEVELS & ACHIEVEMENTS TAB
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // COMPACT & MINIMAL HERO LEVEL CARD
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = levelInfo.tier.color.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, levelInfo.tier.color.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = levelInfo.tier.color,
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${levelInfo.level}",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Level ${levelInfo.level} • ${levelInfo.title}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${levelInfo.tier.title} Striker • ${levelInfo.xpIntoCurrentLevel}/${levelInfo.xpRequiredForNextLevel} XP",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = levelInfo.tier.color
                                    ) {
                                        Text(
                                            text = "${levelInfo.totalXp} XP",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // XP Bar
                                LinearProgressIndicator(
                                    progress = { levelInfo.progressPercent },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = levelInfo.tier.color,
                                    trackColor = levelInfo.tier.color.copy(alpha = 0.2f)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // XP Sources breakdown row (compact scroll)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    XpSourceChip("Matches (${player.matchesPlayed})", "+${levelInfo.matchesPlayedXp}")
                                    XpSourceChip("Wins (${player.matchesWon})", "+${levelInfo.matchesWonXp}")
                                    XpSourceChip("Coins (${player.totalCoinsPocketed})", "+${levelInfo.coinsPocketedXp}")
                                    XpSourceChip("Queens (${player.queensCovered})", "+${levelInfo.queenCoveredXp}")
                                    if (levelInfo.achievementBonusXp > 0) {
                                        XpSourceChip("Badges (${levelInfo.unlockedAchievementsCount})", "+${levelInfo.achievementBonusXp}")
                                    }
                                }
                            }
                        }

                        // ACHIEVEMENTS HEADER & CATEGORIES
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Milestone Achievements ($unlockedCount/${achievements.size} Unlocked)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Category chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AchievementCategory.values().forEach { cat ->
                                val isSelected = selectedAchievementCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedAchievementCategory = cat },
                                    label = { Text(cat.displayName, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        // List of achievements
                        filteredAchievements.forEach { achievement ->
                            AchievementItemCard(achievement = achievement)
                        }
                    }
                }
                1 -> {
                    // PERFORMANCE & METRICS TAB (WITH TRAJECTORY LINE GRAPH)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // INTERACTIVE TRAJECTORY LINE GRAPH
                        PlayerTrajectoryLineGraph(
                            player = player,
                            levelInfo = levelInfo
                        )

                        // MATCH PERFORMANCE SUMMARY
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Match Win/Loss Record", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MetricBox("Played", "${player.matchesPlayed}")
                                    MetricBox("Won", "${player.matchesWon}", Color(0xFF2E7D32))
                                    MetricBox("Lost", "${player.matchesLost}", Color(0xFFC62828))
                                    MetricBox("Win Rate", "${"%.1f".format(player.winRate)}%", MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        // COINS / DOTS POCKETED
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Coins / Dots Pocketed", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MetricBox("Total Coins", "${player.totalCoinsPocketed}")
                                    MetricBox("White Coins", "${player.whitePocketed}")
                                    MetricBox("Black Coins", "${player.blackPocketed}")
                                    val whitePct = if (player.totalCoinsPocketed > 0) (player.whitePocketed.toFloat() / player.totalCoinsPocketed) * 100f else 0f
                                    MetricBox("White Ratio", "${"%.0f".format(whitePct)}%")
                                }
                            }
                        }

                        // QUEEN STATS
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    QueenCoinBadge(size = 18.dp, isCovered = true)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Queen Mastery", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MetricBox("Attempts", "${player.queenAttempts}")
                                    MetricBox("Covered", "${player.queensCovered}", CarromQueenRed)
                                    MetricBox("Success %", "${"%.1f".format(player.queenSuccessRate)}%")
                                    MetricBox("Queen Pts", "${player.queenPointsScored}")
                                }
                            }
                        }

                        // ADVANCED METRICS
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Advanced Match Telemetry", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MetricBox("Boards Won", "${player.boardsWon}/${player.boardsPlayed}")
                                    MetricBox("Nill Wins", "${player.nillBoardWins}")
                                    MetricBox("Penalties", "${player.penalties}")
                                    MetricBox("Total Pts", "${player.totalPointsContributed}")
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // PROFILE & NOTES TAB
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ProfileInfoCard(label = "Registered Full Name", value = player.name)
                        ProfileInfoCard(label = "Nickname / Title", value = player.nickname.ifBlank { "None assigned" })
                        ProfileInfoCard(label = "Skill Level", value = player.skillLevel)

                        // GROUP MEMBERSHIPS & REGULAR STATUS
                        val affiliatedGroups = remember(player, groups) {
                            val list = mutableListOf<GroupEntity>()
                            groups.forEach { g ->
                                if (g.hasMember(player.id) || g.name.equals(player.groupName, true)) {
                                    if (list.none { it.id == g.id }) list.add(g)
                                }
                            }
                            list
                        }
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Group Memberships & Match Regulars", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Players can belong to multiple groups or change groups on any match. They appear in regular player quick entry during match setup.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                if (affiliatedGroups.isEmpty()) {
                                    Text(
                                        text = "Currently in General pool. Add this player to groups in the Groups tab for 1-tap regular match selection.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        affiliatedGroups.forEach { grp ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        text = grp.name,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val dateFormat = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }
                        ProfileInfoCard(label = "Player Since", value = dateFormat.format(Date(player.createdAt)))

                        if (player.notes.isNotBlank()) {
                            ProfileInfoCard(label = "Player Notes", value = player.notes)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onEdit,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Profile & Group")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun XpSourceChip(label: String, xp: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = xp, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
        Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MetricBox(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = valueColor
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileInfoCard(label: String, value: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AchievementItemCard(achievement: PlayerAchievement) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (achievement.isUnlocked) {
            achievement.tier.color.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (achievement.isUnlocked) achievement.tier.color.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (achievement.isUnlocked) achievement.tier.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, if (achievement.isUnlocked) achievement.tier.color else Color.Transparent),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (achievement.isUnlocked) achievement.icon else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (achievement.isUnlocked) achievement.tier.color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = achievement.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (achievement.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (achievement.isUnlocked) achievement.tier.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (achievement.isUnlocked) "UNLOCKED" else "${achievement.currentProgress}/${achievement.target}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (achievement.isUnlocked) achievement.tier.color else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = achievement.description,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!achievement.isUnlocked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { achievement.progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = achievement.tier.color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}
