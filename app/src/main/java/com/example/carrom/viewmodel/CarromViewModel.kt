package com.example.carrom.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.carrom.data.local.CarromDatabase
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

    val activeSavedMatch: StateFlow<GameState?> = repository.activeMatchFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _liveGameState = MutableStateFlow<GameState?>(null)
    val liveGameState: StateFlow<GameState?> = _liveGameState.asStateFlow()

    private var engine: CarromGameEngine? = null

    init {
        // Automatically check if there is an active match
        viewModelScope.launch {
            val existing = repository.getActiveMatch()
            if (existing != null && !existing.isMatchOver) {
                // Keep it ready for resume
            }
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
        queenPoints: Int = 5,
        enable24PlusQueenRule: Boolean = true
    ) {
        viewModelScope.launch {
            // Ensure players exist in DB with unique entities
            val finalT1 = team1Players.mapIndexed { idx, p ->
                val fallbackName = if (p.name.isNotBlank()) p.name else "Player ${idx + 1}"
                val entity = repository.getOrCreatePlayer(fallbackName, p.avatarColorIndex)
                Player(id = entity.id, name = entity.name, avatarColorIndex = entity.avatarColorIndex)
            }
            val finalT2 = team2Players.mapIndexed { idx, p ->
                val fallbackName = if (p.name.isNotBlank()) p.name else "Player ${idx + 3}"
                val entity = repository.getOrCreatePlayer(fallbackName, p.avatarColorIndex)
                Player(id = entity.id, name = entity.name, avatarColorIndex = entity.avatarColorIndex)
            }

            // Find matching breaker ID
            val allOriginal = team1Players + team2Players
            val allFinal = finalT1 + finalT2
            val originalBreaker = allOriginal.find { it.id == firstBreakerPlayerId }
            val finalBreakerId = if (originalBreaker != null) {
                allFinal.find { it.name.equals(originalBreaker.name, ignoreCase = true) }?.id
                    ?: allFinal.find { it.id == firstBreakerPlayerId }?.id
                    ?: finalT1.firstOrNull()?.id ?: 1L
            } else {
                allFinal.find { it.id == firstBreakerPlayerId }?.id ?: finalT1.firstOrNull()?.id ?: 1L
            }

            val config = MatchConfig(
                team1Name = team1Name.ifBlank { "Team 1" },
                team2Name = team2Name.ifBlank { "Team 2" },
                team1Players = finalT1,
                team2Players = finalT2,
                firstBreakerPlayerId = finalBreakerId,
                proMode = proMode,
                targetPoints = targetPoints,
                nillBoardThreshold = nillBoardThreshold,
                queenPoints = queenPoints,
                enable24PlusQueenRule = enable24PlusQueenRule
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

            val newEngine = CarromGameEngine(initialState)
            engine = newEngine
            _liveGameState.value = initialState
            repository.saveActiveMatch(initialState)
        }
    }

    fun resumeMatch(state: GameState) {
        val newEngine = CarromGameEngine(state)
        engine = newEngine
        _liveGameState.value = state
    }

    fun pocketWhite() {
        val currentEngine = engine ?: return
        val updated = currentEngine.pocketWhite()
        _liveGameState.value = updated
        persistState(updated)
    }

    fun pocketBlack() {
        val currentEngine = engine ?: return
        val updated = currentEngine.pocketBlack()
        _liveGameState.value = updated
        persistState(updated)
    }

    fun pocketQueen() {
        val currentEngine = engine ?: return
        val updated = currentEngine.pocketQueen()
        _liveGameState.value = updated
        persistState(updated)
    }

    fun recordPenalty() {
        val currentEngine = engine ?: return
        val updated = currentEngine.recordPenalty()
        _liveGameState.value = updated
        persistState(updated)
    }

    fun undo() {
        val currentEngine = engine ?: return
        val updated = currentEngine.undo()
        _liveGameState.value = updated
        persistState(updated)
    }

    fun endTurn() {
        val currentEngine = engine ?: return
        val updated = currentEngine.endTurn()
        _liveGameState.value = updated
        persistState(updated)
    }

    fun dismissBoardResultDialog() {
        val currentEngine = engine ?: return
        val updated = currentEngine.dismissBoardResultDialog()
        _liveGameState.value = updated
        persistState(updated)
    }

    fun startNextBoard() {
        val currentEngine = engine ?: return
        val updated = currentEngine.startNextBoard()
        _liveGameState.value = updated
        persistState(updated)
    }

    fun finishAndSaveMatch() {
        val state = _liveGameState.value ?: return
        viewModelScope.launch {
            repository.finalizeAndSaveMatch(state)
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

    private fun persistState(state: GameState) {
        viewModelScope.launch {
            if (state.isMatchOver) {
                repository.saveActiveMatch(state)
            } else {
                repository.saveActiveMatch(state)
            }
        }
    }

    fun addNewPlayer(name: String, avatarColorIndex: Int) {
        viewModelScope.launch {
            repository.insertPlayer(name, avatarColorIndex)
        }
    }
}
