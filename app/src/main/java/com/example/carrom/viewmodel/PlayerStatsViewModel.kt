package com.example.carrom.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.carrom.data.local.CarromDatabase
import com.example.carrom.data.local.entity.GroupEntity
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

    val groups: StateFlow<List<GroupEntity>> = repository.allGroups
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
        skillLevel: String = "Intermediate",
        groupName: String = "General"
    ) {
        viewModelScope.launch {
            repository.insertPlayer(
                name = name,
                avatarColorIndex = avatarColorIndex,
                nickname = nickname,
                notes = notes,
                skillLevel = skillLevel,
                groupName = groupName
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

    fun addGroup(
        name: String,
        description: String = "",
        colorIndex: Int = 0
    ) {
        viewModelScope.launch {
            repository.insertGroup(
                name = name,
                description = description,
                colorIndex = colorIndex
            )
        }
    }

    fun updateGroup(group: GroupEntity) {
        viewModelScope.launch {
            repository.updateGroup(group)
        }
    }

    fun deleteGroup(id: Long) {
        viewModelScope.launch {
            repository.deleteGroupById(id)
        }
    }

    fun addPlayerToGroup(groupId: Long, playerId: Long) {
        viewModelScope.launch {
            repository.addPlayerToGroup(groupId, playerId)
        }
    }

    fun removePlayerFromGroup(groupId: Long, playerId: Long) {
        viewModelScope.launch {
            repository.removePlayerFromGroup(groupId, playerId)
        }
    }

    fun setGroupMembers(groupId: Long, playerIds: Set<Long>) {
        viewModelScope.launch {
            repository.setGroupMembers(groupId, playerIds)
        }
    }

    fun quickAddPlayerToGroup(
        name: String,
        groupId: Long,
        colorIndex: Int = 0,
        nickname: String = "",
        notes: String = "",
        skillLevel: String = "Intermediate"
    ) {
        viewModelScope.launch {
            val playerId = repository.insertPlayer(
                name = name,
                avatarColorIndex = colorIndex,
                nickname = nickname,
                notes = notes,
                skillLevel = skillLevel,
                groupName = "General"
            )
            repository.addPlayerToGroup(groupId, playerId)
        }
    }
}
