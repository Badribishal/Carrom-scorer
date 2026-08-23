package com.example.carrom.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.carrom.data.local.CarromDatabase
import com.example.carrom.data.local.entity.PlayerEntity
import com.example.carrom.data.repository.CarromRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerStatsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CarromRepository

    init {
        val db = CarromDatabase.getDatabase(application)
        repository = CarromRepository(db)
    }

    val players: StateFlow<List<PlayerEntity>> = repository.allPlayers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPlayer = MutableStateFlow<PlayerEntity?>(null)
    val selectedPlayer: StateFlow<PlayerEntity?> = _selectedPlayer.asStateFlow()

    fun selectPlayer(player: PlayerEntity?) {
        _selectedPlayer.value = player
    }

    fun addPlayer(
        name: String,
        avatarColorIndex: Int = 0,
        nickname: String = "",
        notes: String = "",
        skillLevel: String = "Intermediate"
    ) {
        viewModelScope.launch {
            repository.insertPlayer(
                name = name,
                avatarColorIndex = avatarColorIndex,
                nickname = nickname,
                notes = notes,
                skillLevel = skillLevel
            )
        }
    }

    fun updatePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            repository.updatePlayer(player)
            if (_selectedPlayer.value?.id == player.id) {
                _selectedPlayer.value = player
            }
        }
    }

    fun deletePlayer(id: Long) {
        viewModelScope.launch {
            repository.deletePlayerById(id)
            if (_selectedPlayer.value?.id == id) {
                _selectedPlayer.value = null
            }
        }
    }
}
