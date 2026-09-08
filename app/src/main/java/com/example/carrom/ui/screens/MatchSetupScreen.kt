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
import androidx.compose.ui.draw.rotate
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carrom.data.local.entity.GroupEntity
import com.example.carrom.data.local.entity.PlayerEntity
import com.example.carrom.engine.Player
import com.example.carrom.ui.components.AvatarPalette
import com.example.carrom.ui.components.PlayerAvatar
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSetupScreen(
    savedPlayers: List<PlayerEntity>,
    savedGroups: List<GroupEntity> = emptyList(),
    onBack: () -> Unit,
    onSaveGroup: ((
        name: String,
        team1Player1: String,
        team1Player2: String,
        team2Player1: String,
        team2Player2: String,
        team1Name: String,
        team2Name: String,
        isDoubles: Boolean,
        colorIndex: Int,
        existingId: Long
    ) -> Unit)? = null,
    onDeleteGroup: ((groupId: Long) -> Unit)? = null,
    onAddNewPlayer: ((name: String, colorIndex: Int, nickname: String, notes: String, skillLevel: String, groupName: String, groupId: Long?) -> Unit)? = null,
    onAddNewGroup: ((name: String, description: String, colorIndex: Int) -> Unit)? = null,
    onManageGroups: (() -> Unit)? = null,
    onExportGroups: (() -> Unit)? = null,
    onImportGroups: (() -> Unit)? = null,
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
    val groups = savedGroups
    var isDoubles by rememberSaveable { mutableStateOf(true) }
    var team1Name by rememberSaveable { mutableStateOf("Team 1") }
    var team2Name by rememberSaveable { mutableStateOf("Team 2") }

    // Saved Group selection & management state
    var selectedSavedGroupName by rememberSaveable { mutableStateOf<String?>(null) }
    var groupDropdownExpanded by remember { mutableStateOf(false) }
    var groupToEditOrCreate by remember { mutableStateOf<GroupEntity?>(null) }
    var groupToDelete by remember { mutableStateOf<GroupEntity?>(null) }
    var showManageGroupsSheet by remember { mutableStateOf(false) }

    // Selected player names for each slot
    var t1p1Name by rememberSaveable { mutableStateOf(savedPlayers.getOrNull(0)?.name ?: "Player 1") }
    var t1p2Name by rememberSaveable { mutableStateOf(savedPlayers.getOrNull(2)?.name ?: "Player 3") }
    var t2p1Name by rememberSaveable { mutableStateOf(savedPlayers.getOrNull(1)?.name ?: "Player 2") }
    var t2p2Name by rememberSaveable { mutableStateOf(savedPlayers.getOrNull(3)?.name ?: "Player 4") }

    val loadGroupIntoMatch: (GroupEntity) -> Unit = { grp ->
        selectedSavedGroupName = grp.name
        if (grp.team1Player1.isNotBlank()) t1p1Name = grp.team1Player1
        if (grp.team1Player2.isNotBlank()) t1p2Name = grp.team1Player2
        if (grp.team2Player1.isNotBlank()) t2p1Name = grp.team2Player1
        if (grp.team2Player2.isNotBlank()) t2p2Name = grp.team2Player2
        if (grp.team1Name.isNotBlank()) team1Name = grp.team1Name
        if (grp.team2Name.isNotBlank()) team2Name = grp.team2Name
        isDoubles = grp.isDoubles
    }

    // Breaker Selection: 0 = T1P1, 1 = T2P1, 2 = T1P2, 3 = T2P2
    var selectedBreakerIndex by rememberSaveable { mutableIntStateOf(0) }

    // Swap Teams animation and state
    var swapRotation by remember { mutableFloatStateOf(0f) }
    val animatedSwapRotation by animateFloatAsState(
        targetValue = swapRotation,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swapRotation"
    )

    val isGroupPopulated = !selectedSavedGroupName.isNullOrBlank()

    val swapTeams: () -> Unit = {
        val tempTeamName = team1Name
        team1Name = team2Name
        team2Name = tempTeamName

        val tempP1 = t1p1Name
        t1p1Name = t2p1Name
        t2p1Name = tempP1

        val tempP2 = t1p2Name
        t1p2Name = t2p2Name
        t2p2Name = tempP2

        selectedBreakerIndex = when (selectedBreakerIndex) {
            0 -> 1
            1 -> 0
            2 -> 3
            3 -> 2
            else -> 0
        }
        swapRotation += 180f
    }

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

                // SAVED GROUPS SECTION (MINIMAL & COMPACT)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, if (selectedSavedGroupName != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("saved_groups_section")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Dropdown trigger
                            Box(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { groupDropdownExpanded = true }
                                        .padding(horizontal = 4.dp, vertical = 6.dp)
                                        .testTag("saved_group_dropdown_trigger"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = if (selectedSavedGroupName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (selectedSavedGroupName != null) "Group: $selectedSavedGroupName" else "Select Saved Group ▼",
                                            fontSize = 13.sp,
                                            fontWeight = if (selectedSavedGroupName != null) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selectedSavedGroupName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = if (groupDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown Menu",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = groupDropdownExpanded,
                                    onDismissRequest = { groupDropdownExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .testTag("saved_group_dropdown_menu")
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        },
                                        text = {
                                            Text("— Manual Entry (Custom) —", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        },
                                        onClick = {
                                            selectedSavedGroupName = null
                                            groupDropdownExpanded = false
                                        }
                                    )
                                    HorizontalDivider()

                                    if (savedGroups.isEmpty()) {
                                        DropdownMenuItem(
                                            text = {
                                                Text("No saved groups yet. Tap '+ Group' to save one.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            },
                                            onClick = {
                                                groupDropdownExpanded = false
                                                groupToEditOrCreate = GroupEntity(
                                                    id = 0L,
                                                    name = "",
                                                    team1Player1 = t1p1Name,
                                                    team1Player2 = if (isDoubles) t1p2Name else "",
                                                    team2Player1 = t2p1Name,
                                                    team2Player2 = if (isDoubles) t2p2Name else "",
                                                    team1Name = team1Name,
                                                    team2Name = team2Name,
                                                    isDoubles = isDoubles
                                                )
                                            }
                                        )
                                    } else {
                                        savedGroups.forEach { grp ->
                                            val isSelected = selectedSavedGroupName == grp.name
                                            val grpColor = AvatarPalette.getOrElse(grp.colorIndex) { MaterialTheme.colorScheme.primary }
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(grpColor)
                                                    )
                                                },
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(grp.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(if (grp.isDoubles) "2v2" else "1v1", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                },
                                                trailingIcon = if (isSelected) {
                                                    { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                                                } else null,
                                                onClick = {
                                                    groupDropdownExpanded = false
                                                    loadGroupIntoMatch(grp)
                                                },
                                                modifier = Modifier.testTag("group_dropdown_item_${grp.name.replace(" ", "_").lowercase()}")
                                            )
                                        }
                                    }
                                }
                            }

                            // Minimal Action icons: Edit / Delete / Clear / + Add
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val activeGroup = savedGroups.find { it.name == selectedSavedGroupName }
                                if (activeGroup != null) {
                                    IconButton(
                                        onClick = { groupToEditOrCreate = activeGroup },
                                        modifier = Modifier.size(28.dp).testTag("edit_active_group_button")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Group", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                    }
                                    IconButton(
                                        onClick = { groupToDelete = activeGroup },
                                        modifier = Modifier.size(28.dp).testTag("delete_active_group_button")
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Group", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                                    }
                                }
                                if (selectedSavedGroupName != null) {
                                    IconButton(
                                        onClick = swapTeams,
                                        modifier = Modifier.size(28.dp).testTag("swap_teams_header_button")
                                    ) {
                                        Icon(
                                            Icons.Default.SwapVert,
                                            contentDescription = "Swap Teams",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp).rotate(animatedSwapRotation)
                                        )
                                    }
                                    IconButton(
                                        onClick = { selectedSavedGroupName = null },
                                        modifier = Modifier.size(28.dp).testTag("clear_saved_group_button")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear Selection", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        groupToEditOrCreate = GroupEntity(
                                            id = 0L,
                                            name = "",
                                            team1Player1 = t1p1Name,
                                            team1Player2 = if (isDoubles) t1p2Name else "",
                                            team2Player1 = t2p1Name,
                                            team2Player2 = if (isDoubles) t2p2Name else "",
                                            team1Name = team1Name,
                                            team2Name = team2Name,
                                            isDoubles = isDoubles,
                                            colorIndex = 0
                                        )
                                    },
                                    modifier = Modifier.size(28.dp).testTag("create_group_button")
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Create Group", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                                if (savedGroups.isNotEmpty()) {
                                    IconButton(
                                        onClick = { showManageGroupsSheet = true },
                                        modifier = Modifier.size(28.dp).testTag("manage_saved_groups_button")
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = "Manage Groups", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }

                        // Compact 1-tap mini chips
                        if (savedGroups.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                savedGroups.forEach { grp ->
                                    val isSelected = selectedSavedGroupName == grp.name
                                    val grpColor = AvatarPalette.getOrElse(grp.colorIndex) { MaterialTheme.colorScheme.primary }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (isSelected) selectedSavedGroupName = null else loadGroupIntoMatch(grp)
                                            }
                                            .testTag("group_chip_${grp.name.replace(" ", "_").lowercase()}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(grpColor))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = grp.name,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
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
                            groups = groups,
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
                                    groups = groups,
                                    onSelectPlayer = { t1p2Name = it },
                                    onAddNew = { showAddPlayerDialogForSlot = 1 }
                                )
                            }
                        }
                    }
                }

                // Swap Teams Button (appears once a group is populated)
                AnimatedVisibility(
                    visible = isGroupPopulated,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FilledTonalButton(
                            onClick = swapTeams,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("swap_teams_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Swap Teams",
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(animatedSwapRotation),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Swap Teams",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "($team1Name ⇄ $team2Name)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
                            groups = groups,
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
                                    groups = groups,
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

    // Create or Edit Saved Group Dialog
    groupToEditOrCreate?.let { initialGrp ->
        val isEditing = initialGrp.id > 0L
        var groupNameInput by remember { mutableStateOf(initialGrp.name) }
        var groupDoubles by remember { mutableStateOf(if (isEditing) initialGrp.isDoubles else isDoubles) }
        var t1NameInput by remember { mutableStateOf(if (isEditing) initialGrp.team1Name else team1Name) }
        var t2NameInput by remember { mutableStateOf(if (isEditing) initialGrp.team2Name else team2Name) }
        var t1p1Input by remember { mutableStateOf(if (isEditing) initialGrp.team1Player1 else t1p1Name) }
        var t1p2Input by remember { mutableStateOf(if (isEditing) initialGrp.team1Player2 else (if (groupDoubles) t1p2Name else "")) }
        var t2p1Input by remember { mutableStateOf(if (isEditing) initialGrp.team2Player1 else t2p1Name) }
        var t2p2Input by remember { mutableStateOf(if (isEditing) initialGrp.team2Player2 else (if (groupDoubles) t2p2Name else "")) }
        var groupColorIdx by remember { mutableIntStateOf(initialGrp.colorIndex) }
        var nameError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { groupToEditOrCreate = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.GroupAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditing) "Edit Saved Group" else "Create Saved Group",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Save group names, players, and team assignments for 1-tap entry.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = groupNameInput,
                        onValueChange = {
                            groupNameInput = it
                            if (nameError && it.isNotBlank()) nameError = false
                        },
                        label = { Text("Group Name *") },
                        placeholder = { Text("e.g. Friday Team, College Group") },
                        singleLine = true,
                        isError = nameError,
                        supportingText = if (nameError) {
                            { Text("Group name cannot be blank", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("saved_group_name_input")
                    )

                    // Match Mode (Doubles vs Singles)
                    Text("Match Format", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = groupDoubles,
                            onClick = { groupDoubles = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Doubles (2v2)", fontSize = 12.sp)
                        }
                        SegmentedButton(
                            selected = !groupDoubles,
                            onClick = { groupDoubles = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Singles (1v1)", fontSize = 12.sp)
                        }
                    }

                    // TEAM 1 PLAYERS
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("TEAM 1", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            OutlinedTextField(
                                value = t1NameInput,
                                onValueChange = { t1NameInput = it },
                                label = { Text("Team 1 Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = t1p1Input,
                                onValueChange = { t1p1Input = it },
                                label = { Text("Player 1 *") },
                                placeholder = { Text("e.g. Rahul") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("group_t1p1_input")
                            )
                            if (groupDoubles) {
                                OutlinedTextField(
                                    value = t1p2Input,
                                    onValueChange = { t1p2Input = it },
                                    label = { Text("Player 2 *") },
                                    placeholder = { Text("e.g. Amit") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("group_t1p2_input")
                                )
                            }
                        }
                    }

                    // TEAM 2 PLAYERS
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("TEAM 2", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                            OutlinedTextField(
                                value = t2NameInput,
                                onValueChange = { t2NameInput = it },
                                label = { Text("Team 2 Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = t2p1Input,
                                onValueChange = { t2p1Input = it },
                                label = { Text("Player 1 *") },
                                placeholder = { Text("e.g. Suman") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("group_t2p1_input")
                            )
                            if (groupDoubles) {
                                OutlinedTextField(
                                    value = t2p2Input,
                                    onValueChange = { t2p2Input = it },
                                    label = { Text("Player 2 *") },
                                    placeholder = { Text("e.g. Raj") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("group_t2p2_input")
                                )
                            }
                        }
                    }

                    // Quick Suggestion Chips from Saved Players
                    if (savedPlayers.isNotEmpty()) {
                        Text("Quick Add from Saved Players", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            savedPlayers.forEach { sp ->
                                AssistChip(
                                    onClick = {
                                        // Fill first available or blank slot
                                        when {
                                            t1p1Input.isBlank() || t1p1Input.startsWith("Player ") -> t1p1Input = sp.name
                                            groupDoubles && (t1p2Input.isBlank() || t1p2Input.startsWith("Player ")) -> t1p2Input = sp.name
                                            t2p1Input.isBlank() || t2p1Input.startsWith("Player ") -> t2p1Input = sp.name
                                            groupDoubles && (t2p2Input.isBlank() || t2p2Input.startsWith("Player ")) -> t2p2Input = sp.name
                                            else -> t1p1Input = sp.name
                                        }
                                    },
                                    leadingIcon = {
                                        PlayerAvatar(name = sp.name, avatarColorIndex = sp.avatarColorIndex, size = 18.dp)
                                    },
                                    label = { Text(sp.name, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Color Badge
                    Text("Badge Color", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarPalette.forEachIndexed { idx, color ->
                            val isSel = groupColorIdx == idx
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { groupColorIdx = idx }
                                    .then(
                                        if (isSel) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = groupNameInput.trim()
                        if (trimmedName.isBlank()) {
                            nameError = true
                            return@Button
                        }

                        val p1 = t1p1Input.trim().ifBlank { "Player 1" }
                        val p2 = if (groupDoubles) t1p2Input.trim().ifBlank { "Player 3" } else ""
                        val p3 = t2p1Input.trim().ifBlank { "Player 2" }
                        val p4 = if (groupDoubles) t2p2Input.trim().ifBlank { "Player 4" } else ""
                        val tm1 = t1NameInput.trim().ifBlank { "Team 1" }
                        val tm2 = t2NameInput.trim().ifBlank { "Team 2" }

                        onSaveGroup?.invoke(
                            trimmedName,
                            p1,
                            p2,
                            p3,
                            p4,
                            tm1,
                            tm2,
                            groupDoubles,
                            groupColorIdx,
                            initialGrp.id
                        )

                        // Auto-load newly created or updated group into match setup fields
                        selectedSavedGroupName = trimmedName
                        t1p1Name = p1
                        t1p2Name = p2
                        t2p1Name = p3
                        t2p2Name = p4
                        team1Name = tm1
                        team2Name = tm2
                        isDoubles = groupDoubles

                        groupToEditOrCreate = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("save_group_submit_button")
                ) {
                    Text(if (isEditing) "Save Changes" else "Save Group", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToEditOrCreate = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Group Confirmation Dialog
    groupToDelete?.let { grp ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            icon = {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Delete Saved Group?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete '${grp.name}'? Existing match history and player statistics will NOT be affected.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGroup?.invoke(grp.id)
                        if (selectedSavedGroupName == grp.name) {
                            selectedSavedGroupName = null
                        }
                        groupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_delete_group_button")
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Manage All Saved Groups Dialog
    if (showManageGroupsSheet) {
        AlertDialog(
            onDismissRequest = { showManageGroupsSheet = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Manage Saved Groups", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showManageGroupsSheet = false
                                groupToEditOrCreate = GroupEntity(
                                    id = 0L,
                                    name = "",
                                    team1Player1 = t1p1Name,
                                    team1Player2 = if (isDoubles) t1p2Name else "",
                                    team2Player1 = t2p1Name,
                                    team2Player2 = if (isDoubles) t2p2Name else "",
                                    team1Name = team1Name,
                                    team2Name = team2Name,
                                    isDoubles = isDoubles
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("create_new_group_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Group", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        if (onExportGroups != null) {
                            OutlinedButton(
                                onClick = {
                                    showManageGroupsSheet = false
                                    onExportGroups()
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.testTag("export_groups_button")
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export Groups", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export", fontSize = 12.sp)
                            }
                        }

                        if (onImportGroups != null) {
                            OutlinedButton(
                                onClick = {
                                    showManageGroupsSheet = false
                                    onImportGroups()
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.testTag("import_groups_button")
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = "Import Groups", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import", fontSize = 12.sp)
                            }
                        }
                    }

                    if (savedGroups.isEmpty()) {
                        Text(
                            "No saved groups yet. Click '+ Create New Group' above to save commonly used player teams.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        savedGroups.forEach { grp ->
                            val grpColor = AvatarPalette.getOrElse(grp.colorIndex) { MaterialTheme.colorScheme.primary }
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(grpColor)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(grp.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (grp.isDoubles) "(2v2)" else "(1v1)",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    showManageGroupsSheet = false
                                                    groupToEditOrCreate = grp
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(
                                                onClick = {
                                                    showManageGroupsSheet = false
                                                    groupToDelete = grp
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (grp.isDoubles) {
                                            "Team 1: ${grp.team1Player1}, ${grp.team1Player2}\nTeam 2: ${grp.team2Player1}, ${grp.team2Player2}"
                                        } else {
                                            "Team 1: ${grp.team1Player1} • Team 2: ${grp.team2Player1}"
                                        },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    FilledTonalButton(
                                        onClick = {
                                            loadGroupIntoMatch(grp)
                                            showManageGroupsSheet = false
                                        },
                                        modifier = Modifier.fillMaxWidth().height(32.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Load into Match", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManageGroupsSheet = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Isolated Fast Dialog for adding new player into slot with pre-defined group selection
    if (showAddPlayerDialogForSlot != null) {
        SlotPlayerAddModal(
            slotIndex = showAddPlayerDialogForSlot!!,
            groups = groups,
            onDismiss = { showAddPlayerDialogForSlot = null },
            onAddNewGroup = onAddNewGroup,
            onSaveAndSelect = { name, nickname, colorIndex, groupName, groupId ->
                onAddNewPlayer?.invoke(name, colorIndex, nickname, "", "Intermediate", groupName, groupId)
                when (showAddPlayerDialogForSlot) {
                    0 -> t1p1Name = name
                    1 -> t1p2Name = name
                    2 -> t2p1Name = name
                    3 -> t2p2Name = name
                }
                showAddPlayerDialogForSlot = null
            }
        )
    }
}

/**
 * Isolated Modal for entering player name in match setup slot with pre-defined group selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotPlayerAddModal(
    slotIndex: Int,
    groups: List<GroupEntity>,
    onDismiss: () -> Unit,
    onAddNewGroup: ((name: String, description: String, colorIndex: Int) -> Unit)? = null,
    onSaveAndSelect: (name: String, nickname: String, colorIndex: Int, groupName: String, selectedGroupId: Long?) -> Unit
) {
    var playerNameInput by rememberSaveable { mutableStateOf("") }
    var playerNicknameInput by rememberSaveable { mutableStateOf("") }
    var selectedColorIndex by rememberSaveable { mutableIntStateOf(slotIndex % AvatarPalette.size) }
    var selectedGroupName by rememberSaveable { mutableStateOf(if (groups.isNotEmpty()) groups.first().name else "General") }
    var selectedGroupId by rememberSaveable { mutableStateOf<Long?>(if (groups.isNotEmpty()) groups.first().id else null) }
    var nameError by remember { mutableStateOf(false) }
    var showInlineCreateGroupDialog by remember { mutableStateOf(false) }

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
                    Text("Add Player to Match", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Assign player profile & pre-defined group", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                placeholder = { Text("e.g. Striker King") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Pre-defined Group Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Assign to Pre-defined Group",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = { showInlineCreateGroupDialog = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("+ New Group", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "General" option
                    val isGeneral = selectedGroupName.equals("General", ignoreCase = true)
                    FilterChip(
                        selected = isGeneral,
                        onClick = {
                            selectedGroupName = "General"
                            selectedGroupId = null
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outline)
                            )
                        },
                        label = { Text("General", fontSize = 12.sp, fontWeight = if (isGeneral) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(10.dp)
                    )

                    // All pre-defined groups
                    groups.forEach { grp ->
                        val isSelected = selectedGroupName.equals(grp.name, ignoreCase = true) || selectedGroupId == grp.id
                        val grpColor = AvatarPalette.getOrElse(grp.colorIndex) { MaterialTheme.colorScheme.primary }
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedGroupName = grp.name
                                selectedGroupId = grp.id
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(grpColor)
                                )
                            },
                            label = {
                                Text(
                                    grp.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

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
                            onSaveAndSelect(trimmed, playerNicknameInput.trim(), selectedColorIndex, selectedGroupName, selectedGroupId)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("confirm_add_player_button")
                ) {
                    Text("Save & Assign", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Inline dialog to pre-define group on the fly inside player add modal
    if (showInlineCreateGroupDialog) {
        var inlineGroupName by remember { mutableStateOf("") }
        var inlineGroupDesc by remember { mutableStateOf("") }
        var inlineGroupColor by remember { mutableIntStateOf(0) }
        var inlineError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showInlineCreateGroupDialog = false },
            title = { Text("Pre-define New Group", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Pre-define a group name beforehand to easily assign players:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = inlineGroupName,
                        onValueChange = {
                            inlineGroupName = it
                            if (inlineError && it.isNotBlank()) inlineError = false
                        },
                        label = { Text("Group Name *") },
                        placeholder = { Text("e.g. Club Alpha, Family") },
                        singleLine = true,
                        isError = inlineError,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inlineGroupDesc,
                        onValueChange = { inlineGroupDesc = it },
                        label = { Text("Description (Optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Badge Color", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AvatarPalette.forEachIndexed { idx, color ->
                            val isSel = inlineGroupColor == idx
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { inlineGroupColor = idx }
                                    .then(if (isSel) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = inlineGroupName.trim()
                        if (trimmed.isBlank()) {
                            inlineError = true
                        } else {
                            onAddNewGroup?.invoke(trimmed, inlineGroupDesc.trim(), inlineGroupColor)
                            selectedGroupName = trimmed
                            showInlineCreateGroupDialog = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add Group")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInlineCreateGroupDialog = false }) {
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
    groups: List<GroupEntity> = emptyList(),
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
                        val playerGroups = groups.filter { it.hasMember(player.id) || it.name.equals(player.groupName, true) }
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
                                        if (playerGroups.isNotEmpty()) {
                                            Text(
                                                playerGroups.joinToString(" • ") { it.name },
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
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
