package com.example.carrom.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalFocusManager
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
import com.example.carrom.engine.PlayerLevelSystem
import com.example.carrom.ui.components.AvatarPalette
import com.example.carrom.ui.components.PlayerAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    groups: List<GroupEntity>,
    players: List<PlayerEntity>,
    onAddGroup: (name: String, description: String, colorIndex: Int) -> Unit,
    onUpdateGroup: (GroupEntity) -> Unit,
    onDeleteGroup: (Long) -> Unit,
    onAddNewPlayerWithGroup: (name: String, colorIndex: Int, nickname: String, notes: String, skillLevel: String, groupName: String) -> Unit,
    onQuickAddPlayerToGroup: (groupId: Long, playerName: String) -> Unit = { _, _ -> },
    onTogglePlayerInGroup: (groupId: Long, playerId: Long, isMember: Boolean) -> Unit = { _, _, _ -> },
    onNavigateToPlayerStats: (groupFilter: String?) -> Unit,
    onBack: () -> Unit
) {
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<GroupEntity?>(null) }
    var groupToDelete by remember { mutableStateOf<GroupEntity?>(null) }
    var groupForNewPlayer by remember { mutableStateOf<String?>(null) }
    var expandedGroupId by remember { mutableStateOf<Long?>(null) }

    // Combine custom groups and any group names existing on players
    val allGroupNames = remember(groups, players) {
        val set = linkedSetOf<String>()
        groups.forEach { set.add(it.name) }
        players.forEach {
            if (it.groupName.isNotBlank() && !it.groupName.equals("General", ignoreCase = true)) {
                set.add(it.groupName)
            }
        }
        if (set.isEmpty()) {
            set.add("General")
        }
        set.toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Player Groups",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("groups_back_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCreateGroupDialog = true },
                        modifier = Modifier.testTag("add_group_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = "Create Group",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateGroupDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("create_group_fab")
            ) {
                Icon(imageVector = Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Group", fontWeight = FontWeight.Bold)
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

            // OVERVIEW HERO CARD
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Flexible Groups & Regular Rosters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Players are not bound to a single group and can switch groups for any match. Use groups to quickly select regular players during match setup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        GroupSummaryStatItem(
                            label = "Total Groups",
                            value = "${groups.size.coerceAtLeast(allGroupNames.size)}",
                            icon = Icons.Default.Category
                        )
                        GroupSummaryStatItem(
                            label = "Total Players",
                            value = "${players.size}",
                            icon = Icons.Default.People
                        )
                        val groupedPlayersCount = players.count { p ->
                            groups.any { g -> g.hasMember(p.id) || g.name.equals(p.groupName, ignoreCase = true) }
                        }
                        GroupSummaryStatItem(
                            label = "In Groups",
                            value = "$groupedPlayersCount",
                            icon = Icons.Default.CheckCircle
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (groups.isEmpty() && players.isEmpty()) {
                // EMPTY STATE WITH QUICK PRESETS
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
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Groups Created Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Create group names beforehand so you can assign players with a single tap during data entry.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Quick Starter Groups:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("Family", "Club Alpha", "Weekend Strikers").forEachIndexed { idx, presetName ->
                                OutlinedButton(
                                    onClick = {
                                        onAddGroup(presetName, "Default $presetName group", idx)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                                ) {
                                    Text("+ $presetName", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Active Groups (${groups.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(groups, key = { it.id }) { group ->
                        val groupPlayers = players.filter { group.hasMember(it.id) || it.groupName.equals(group.name, ignoreCase = true) }
                        val isExpanded = expandedGroupId == group.id

                        GroupCardItem(
                            group = group,
                            groupPlayers = groupPlayers,
                            allPlayers = players,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedGroupId = if (isExpanded) null else group.id
                            },
                            onQuickAddPlayer = { name ->
                                onQuickAddPlayerToGroup(group.id, name)
                            },
                            onToggleMember = { playerId, isMember ->
                                onTogglePlayerInGroup(group.id, playerId, isMember)
                            },
                            onAddPlayer = {
                                groupForNewPlayer = group.name
                            },
                            onViewStats = {
                                onNavigateToPlayerStats(group.name)
                            },
                            onEdit = {
                                groupToEdit = group
                            },
                            onDelete = {
                                groupToDelete = group
                            }
                        )
                    }

                    // Also show any unassigned / General players group if there are players without an explicit GroupEntity
                    val unmanagedPlayers = players.filter { p ->
                        groups.none { it.hasMember(p.id) || it.name.equals(p.groupName, ignoreCase = true) }
                    }
                    if (unmanagedPlayers.isNotEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.FolderOpen,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "General / Ungrouped",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "${unmanagedPlayers.size} players in this group",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { onNavigateToPlayerStats("General") },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text("View", fontSize = 12.sp)
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

    // CREATE GROUP DIALOG
    if (showCreateGroupDialog) {
        CreateOrEditGroupDialog(
            groupToEdit = null,
            onDismiss = { showCreateGroupDialog = false },
            onConfirm = { name, desc, colorIdx ->
                onAddGroup(name, desc, colorIdx)
                showCreateGroupDialog = false
            }
        )
    }

    // EDIT GROUP DIALOG
    groupToEdit?.let { group ->
        CreateOrEditGroupDialog(
            groupToEdit = group,
            onDismiss = { groupToEdit = null },
            onConfirm = { name, desc, colorIdx ->
                onUpdateGroup(group.copy(name = name, description = desc, colorIndex = colorIdx))
                groupToEdit = null
            }
        )
    }

    // DELETE GROUP CONFIRMATION DIALOG
    groupToDelete?.let { group ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Group") },
            text = {
                Text("Are you sure you want to delete the group \"${group.name}\"? Players belonging to this group will remain saved under 'General'.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGroup(group.id)
                        groupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ADD PLAYER DIRECTLY TO THIS GROUP DIALOG
    groupForNewPlayer?.let { targetGroupName ->
        AddPlayerToGroupModal(
            preselectedGroupName = targetGroupName,
            onDismiss = { groupForNewPlayer = null },
            onConfirm = { name, colorIdx, nickname, notes, skillLevel ->
                onAddNewPlayerWithGroup(name, colorIdx, nickname, notes, skillLevel, targetGroupName)
                groupForNewPlayer = null
            }
        )
    }
}

@Composable
private fun GroupSummaryStatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GroupCardItem(
    group: GroupEntity,
    groupPlayers: List<PlayerEntity>,
    allPlayers: List<PlayerEntity>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onQuickAddPlayer: (String) -> Unit,
    onToggleMember: (Long, Boolean) -> Unit,
    onAddPlayer: () -> Unit,
    onViewStats: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val groupColor = AvatarPalette.getOrElse(group.colorIndex) { MaterialTheme.colorScheme.primary }
    var showMenu by remember { mutableStateOf(false) }
    var quickPlayerName by remember { mutableStateOf("") }

    val otherPlayersToAdd = remember(groupPlayers, allPlayers) {
        val memberIds = groupPlayers.map { it.id }.toSet()
        allPlayers.filter { it.id !in memberIds }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, groupColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("group_card_${group.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = groupColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.5.dp, groupColor),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = groupColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = group.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = groupColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${groupPlayers.size} ${if (groupPlayers.size == 1) "Regular" else "Regulars"}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = groupColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (group.description.isNotBlank()) {
                        Text(
                            text = group.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add Player to ${group.name}") },
                            leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onAddPlayer()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Filter Players") },
                            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onViewStats()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Group") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Group", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // STATS & MEMBER PREVIEW ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (groupPlayers.isEmpty()) {
                        Text(
                            text = "No regular players added yet",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Stacked avatar preview
                        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                            groupPlayers.take(4).forEach { p ->
                                PlayerAvatar(
                                    name = p.name,
                                    avatarColorIndex = p.avatarColorIndex,
                                    size = 22.dp
                                )
                            }
                        }
                        if (groupPlayers.size > 4) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+${groupPlayers.size - 4}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                val totalGroupWins = groupPlayers.sumOf { it.matchesWon }
                val totalGroupMatches = groupPlayers.sumOf { it.matchesPlayed }
                Text(
                    text = "$totalGroupMatches matches • $totalGroupWins wins",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ACTION BUTTONS ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onAddPlayer,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Player", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onToggleExpand,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Close Regulars" else "Manage Regulars (${groupPlayers.size})",
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // EXPANDED REGULARS & QUICK ENTRY SECTION
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // QUICK ADD REGULAR PLAYER BAR
                    Text(
                        text = "Quick Entry Regular Player:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = quickPlayerName,
                            onValueChange = { quickPlayerName = it },
                            placeholder = { Text("Player name (e.g. Ramesh)", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val trimmed = quickPlayerName.trim()
                                    if (trimmed.isNotBlank()) {
                                        onQuickAddPlayer(trimmed)
                                        quickPlayerName = ""
                                    }
                                }
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val trimmed = quickPlayerName.trim()
                                if (trimmed.isNotBlank()) {
                                    onQuickAddPlayer(trimmed)
                                    quickPlayerName = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // ADD FROM EXISTING PLAYERS CHIPS (if any exist not yet in this group)
                    if (otherPlayersToAdd.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Add Existing Regulars to this Group:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            otherPlayersToAdd.forEach { p ->
                                AssistChip(
                                    onClick = { onToggleMember(p.id, true) },
                                    label = { Text("+ ${p.name}", fontSize = 11.sp) },
                                    leadingIcon = {
                                        PlayerAvatar(
                                            name = p.name,
                                            avatarColorIndex = p.avatarColorIndex,
                                            size = 18.dp
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    // CURRENT GROUP ROSTER
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Group Roster (${groupPlayers.size} regular players):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (groupPlayers.isEmpty()) {
                        Text(
                            text = "No regular players yet. Type a name above or tap an existing player to add.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        groupPlayers.forEach { p ->
                            val lvl = PlayerLevelSystem.calculatePlayerXp(p)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlayerAvatar(name = p.name, avatarColorIndex = p.avatarColorIndex, size = 26.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Lvl ${lvl.level} • ${lvl.title}", fontSize = 10.sp, color = lvl.tier.color)
                                }
                                Text(
                                    "${p.matchesWon}W / ${p.matchesPlayed}M",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { onToggleMember(p.id, false) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove from group",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
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

@Composable
private fun CreateOrEditGroupDialog(
    groupToEdit: GroupEntity?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, colorIndex: Int) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(groupToEdit?.name ?: "") }
    var description by rememberSaveable { mutableStateOf(groupToEdit?.description ?: "") }
    var selectedColorIndex by rememberSaveable { mutableStateOf(groupToEdit?.colorIndex ?: 0) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (groupToEdit == null) "Create Group" else "Edit Group",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter group name beforehand to easily assign players to this group in player data entry.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (errorText != null && it.isNotBlank()) errorText = null
                    },
                    label = { Text("Group Name *") },
                    placeholder = { Text("e.g. Club Alpha, Family, Office") },
                    singleLine = true,
                    isError = errorText != null,
                    supportingText = errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_name_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("e.g. Weekend tournament squad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Group Color Badge",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
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
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isBlank()) {
                        errorText = "Group name cannot be blank"
                    } else {
                        onConfirm(trimmed, description.trim(), selectedColorIndex)
                    }
                },
                modifier = Modifier.testTag("save_group_button")
            ) {
                Text(if (groupToEdit == null) "Create Group" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPlayerToGroupModal(
    preselectedGroupName: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorIdx: Int, nickname: String, notes: String, skillLevel: String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var skillLevel by rememberSaveable { mutableStateOf("Intermediate") }
    var selectedColorIndex by rememberSaveable { mutableStateOf(0) }
    var nameError by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                PlayerAvatar(name = name.ifBlank { "P" }, avatarColorIndex = selectedColorIndex, size = 44.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Add Player to $preselectedGroupName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Player will be assigned to group '$preselectedGroupName'",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (nameError && it.isNotBlank()) nameError = false
                },
                label = { Text("Player Full Name *") },
                placeholder = { Text("e.g. Amit Kumar") },
                singleLine = true,
                isError = nameError,
                supportingText = if (nameError) { { Text("Name is required", color = MaterialTheme.colorScheme.error) } } else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Nickname / Title (Optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Skill Level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Beginner", "Intermediate", "Advanced", "Master").forEach { skill ->
                    val isSelected = skillLevel == skill
                    FilterChip(
                        selected = isSelected,
                        onClick = { skillLevel = skill },
                        label = { Text(skill, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Avatar Color Palette
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarPalette.forEachIndexed { index, color ->
                    val isSelected = selectedColorIndex == index
                    Box(
                        modifier = Modifier
                            .size(32.dp)
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

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isBlank()) {
                        nameError = true
                    } else {
                        onConfirm(trimmed, selectedColorIndex, nickname.trim(), notes.trim(), skillLevel)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Player to $preselectedGroupName", fontWeight = FontWeight.Bold)
            }
        }
    }
}
