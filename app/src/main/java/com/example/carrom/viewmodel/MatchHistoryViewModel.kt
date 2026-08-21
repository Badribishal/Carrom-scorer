package com.example.carrom.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.carrom.data.local.CarromDatabase
import com.example.carrom.data.local.CarromJsonParser
import com.example.carrom.data.local.entity.MatchEntity
import com.example.carrom.data.repository.CarromRepository
import com.example.carrom.engine.BoardRecord
import com.example.carrom.engine.TurnRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MatchDetailData(
    val match: MatchEntity,
    val boards: List<BoardRecord>,
    val turnLogs: List<TurnRecord>
)

class MatchHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CarromRepository

    init {
        val db = CarromDatabase.getDatabase(application)
        repository = CarromRepository(db)
    }

    val matches: StateFlow<List<MatchEntity>> = repository.allMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMatch = MutableStateFlow<MatchDetailData?>(null)
    val selectedMatch: StateFlow<MatchDetailData?> = _selectedMatch.asStateFlow()

    fun selectMatch(match: MatchEntity?) {
        if (match == null) {
            _selectedMatch.value = null
            return
        }
        val boards = CarromJsonParser.deserializeBoardRecords(match.boardDetailsJson)
        val turns = CarromJsonParser.deserializeTurnRecords(match.turnLogsJson)
        _selectedMatch.value = MatchDetailData(match, boards, turns)
    }

    fun deleteMatch(id: Long) {
        viewModelScope.launch {
            repository.deleteMatchById(id)
            if (_selectedMatch.value?.match?.id == id) {
                _selectedMatch.value = null
            }
        }
    }
}
