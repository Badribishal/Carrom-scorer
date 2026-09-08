package com.example.carrom.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.carrom.data.local.CarromDatabase
import com.example.carrom.data.local.entity.GroupEntity
import com.example.carrom.data.local.entity.PlayerEntity
import com.example.carrom.data.repository.CarromRepository
import com.example.carrom.engine.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CarromViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CarromRepository

    init {
        val db = CarromDatabase.getDatabase(application)
        repository = CarromRepository(db)
    }

    val allPlayers: StateFlow<List<PlayerEntity>> = repository.allPlayers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGroups: StateFlow<List<GroupEntity>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSavedMatch: StateFlow<GameState?> = repository.activeMatchFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _liveGameState = MutableStateFlow<GameState?>(null)
    val liveGameState: StateFlow<GameState?> = _liveGameState.asStateFlow()

    private var engine: CarromGameEngine? = null

    init {
        // Seed default groups if empty and check active match
        viewModelScope.launch {
            repository.seedDefaultGroupsIfEmpty()
            val existing = repository.getActiveMatch()
            if (existing != null && !existing.isMatchOver) {
                // Keep it ready for resume
            }
        }
    }

    fun addNewPlayerWithGroup(
        name: String,
        avatarColorIndex: Int = 0,
        nickname: String = "",
        notes: String = "",
        skillLevel: String = "Intermediate",
        groupName: String = "General",
        groupId: Long? = null
    ) {
        viewModelScope.launch {
            val playerId = repository.insertPlayer(
                name = name,
                avatarColorIndex = avatarColorIndex,
                nickname = nickname,
                notes = notes,
                skillLevel = skillLevel,
                groupName = groupName
            )
            if (groupId != null && groupId > 0) {
                val group = repository.getGroupById(groupId)
                if (group != null) {
                    repository.updateGroup(group.withPlayerAdded(playerId))
                }
            }
        }
    }

    fun addNewGroup(name: String, description: String = "", colorIndex: Int = 0) {
        viewModelScope.launch {
            repository.insertGroup(name, description, colorIndex)
        }
    }

    fun saveQuickGroup(
        name: String,
        team1Player1: String,
        team1Player2: String,
        team2Player1: String,
        team2Player2: String,
        team1Name: String = "Team 1",
        team2Name: String = "Team 2",
        isDoubles: Boolean = true,
        colorIndex: Int = 0,
        existingId: Long = 0L
    ) {
        viewModelScope.launch {
            // Ensure all player records exist in DB for stats tracking without duplication
            val names = listOf(team1Player1, team1Player2, team2Player1, team2Player2).filter { it.isNotBlank() }
            val memberIds = mutableListOf<Long>()
            for (pName in names) {
                val p = repository.getOrCreatePlayer(name = pName, groupName = name)
                memberIds.add(p.id)
            }
            val group = GroupEntity(
                id = existingId,
                name = name.trim(),
                team1Name = team1Name.trim().ifBlank { "Team 1" },
                team2Name = team2Name.trim().ifBlank { "Team 2" },
                team1Player1 = team1Player1.trim(),
                team1Player2 = team1Player2.trim(),
                team2Player1 = team2Player1.trim(),
                team2Player2 = team2Player2.trim(),
                isDoubles = isDoubles,
                colorIndex = colorIndex,
                memberPlayerIds = memberIds.joinToString(",")
            )
            repository.saveGroup(group)
        }
    }

    fun deleteGroup(id: Long) {
        viewModelScope.launch {
            repository.deleteGroupById(id)
        }
    }

    fun startNewMatch(
        team1Name: String,
        team2Name: String,
        team1Players: List<Player>,
        team2Players: List<Player>,
        firstBreakerPlayerId: Long,
        proMode: Boolean,
        targetPoints: Int = 29,
        nillBoardThreshold: Int = 7,
        nillWinThreshold: Int = 19,
        queenPoints: Int = 5,
        queenStopThreshold: Int = 24,
        enableQueenStopRule: Boolean = true
    ) {
        val config = MatchConfig(
            team1Name = team1Name.ifBlank { "Team 1" },
            team2Name = team2Name.ifBlank { "Team 2" },
            team1Players = team1Players,
            team2Players = team2Players,
            firstBreakerPlayerId = firstBreakerPlayerId,
            proMode = proMode,
            targetPoints = targetPoints,
            nillBoardThreshold = nillBoardThreshold,
            nillWinThreshold = nillWinThreshold,
            queenPoints = queenPoints,
            queenStopThreshold = queenStopThreshold,
            enableQueenStopRule = enableQueenStopRule
        )

        val initialState = GameState(
            matchId = System.currentTimeMillis(),
            config = config,
            team1Score = 0,
            team2Score = 0,
            currentBoardNumber = 1,
            boardState = BoardLiveState(boardNumber = 1),
            turnState = TurnLiveState(
                currentHand = 1,
                currentTurnIndexInRotation = 0,
                currentOverallTurnNumber = 1
            ),
            startTime = System.currentTimeMillis()
        )

        // Set engine and live state synchronously so navigation opens the scoreboard immediately
        engine = CarromGameEngine(initialState)
        _liveGameState.value = initialState

        viewModelScope.launch {
            // Ensure players exist in DB with unique entities
            val finalT1 = team1Players.mapIndexed { idx, p ->
                val entity = repository.getOrCreatePlayer(p.name, idx)
                Player(id = entity.id, name = entity.name, avatarColorIndex = entity.avatarColorIndex)
            }

            val finalT2 = team2Players.mapIndexed { idx, p ->
                val entity = repository.getOrCreatePlayer(p.name, idx + 2)
                Player(id = entity.id, name = entity.name, avatarColorIndex = entity.avatarColorIndex)
            }

            val allFinal = finalT1 + finalT2
            val finalBreakerId = if (firstBreakerPlayerId > 0) {
                allFinal.find { it.id == firstBreakerPlayerId }?.id ?: finalT1.firstOrNull()?.id ?: 1L
            } else {
                allFinal.find { it.id == firstBreakerPlayerId }?.id ?: finalT1.firstOrNull()?.id ?: 1L
            }

            val persistedConfig = config.copy(
                team1Players = finalT1,
                team2Players = finalT2,
                firstBreakerPlayerId = finalBreakerId
            )
            val persistedState = initialState.copy(config = persistedConfig)

            // Update with persisted player IDs if engine is still on initial board
            if (_liveGameState.value?.matchId == initialState.matchId && _liveGameState.value?.turnState?.currentOverallTurnNumber == 1) {
                engine = CarromGameEngine(persistedState)
                _liveGameState.value = persistedState
            }
            repository.saveActiveMatch(persistedState)
        }
    }

    fun resumeMatch(match: GameState) {
        engine = CarromGameEngine(match)
        _liveGameState.value = match
    }

    fun pocketWhite() {
        val eng = engine ?: return
        val newState = eng.pocketWhite()
        _liveGameState.value = newState
        persistLiveState(newState)
    }

    fun pocketBlack() {
        val eng = engine ?: return
        val newState = eng.pocketBlack()
        _liveGameState.value = newState
        persistLiveState(newState)
    }

    fun pocketQueen() {
        val eng = engine ?: return
        val newState = eng.pocketQueen()
        _liveGameState.value = newState
        persistLiveState(newState)
    }

    fun recordPenalty() {
        val eng = engine ?: return
        val newState = eng.recordPenalty()
        _liveGameState.value = newState
        persistLiveState(newState)
    }

    fun undo() {
        val eng = engine ?: return
        val newState = eng.undo()
        _liveGameState.value = newState
        persistLiveState(newState)
    }

    fun endTurn() {
        val eng = engine ?: return
        val newState = eng.endTurn()
        _liveGameState.value = newState
        persistLiveState(newState)
    }

    fun dismissBoardResultDialog() {
        val eng = engine ?: return
        val newState = eng.dismissBoardResultDialog()
        _liveGameState.value = newState
        persistLiveState(newState)
    }

    fun dismissBoardDialog() {
        dismissBoardResultDialog()
    }

    fun startNextBoard() {
        val eng = engine ?: return
        val newState = eng.startNextBoard()
        _liveGameState.value = newState
        persistLiveState(newState)
    }

    fun recordSimplifiedBoard(
        winningTeamId: Int,
        opponentRemainingCoins: Int,
        queenCoveredByPlayerId: Long?,
        queenCoveredByTeamId: Int?
    ) {
        val eng = engine ?: return
        val newState = eng.recordSimplifiedBoard(
            winningTeamId = winningTeamId,
            opponentRemainingCoins = opponentRemainingCoins,
            queenCoveredByPlayerId = queenCoveredByPlayerId,
            queenCoveredByTeamId = queenCoveredByTeamId
        )
        _liveGameState.value = newState
        persistLiveState(newState)
    }

    fun undoLastBoard() {
        val eng = engine ?: return
        val newState = eng.undoLastBoard()
        _liveGameState.value = newState
        persistLiveState(newState)
    }

    fun finishAndSaveMatch() {
        val current = _liveGameState.value ?: return
        viewModelScope.launch {
            repository.finalizeAndSaveMatch(current)
            repository.clearActiveMatch()
            _liveGameState.value = null
            engine = null
        }
    }

    fun abandonMatch() {
        viewModelScope.launch {
            repository.clearActiveMatch()
            _liveGameState.value = null
            engine = null
        }
    }

    fun clearLiveState() {
        _liveGameState.value = null
        engine = null
    }

    private fun persistLiveState(state: GameState) {
        viewModelScope.launch {
            if (state.isMatchOver) {
                repository.finalizeAndSaveMatch(state)
                repository.clearActiveMatch()
            } else {
                repository.saveActiveMatch(state)
            }
        }
    }
}
