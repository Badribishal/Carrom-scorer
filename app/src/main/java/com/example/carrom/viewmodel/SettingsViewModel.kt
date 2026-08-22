package com.example.carrom.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.carrom.data.local.CarromDatabase
import com.example.carrom.data.repository.CarromRepository
import com.example.ui.theme.MinimalThemePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class MatchRuleDefaults(
    val targetPoints: Int = 29,
    val nillBoardThreshold: Int = 7,
    val queenPoints: Int = 5,
    val queenStopThreshold: Int = 19,
    val enableQueenStopRule: Boolean = true,
    val defaultProMode: Boolean = true
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("carrom_settings_prefs", Context.MODE_PRIVATE)
    private val repository: CarromRepository

    init {
        val db = CarromDatabase.getDatabase(application)
        repository = CarromRepository(db)
    }

    private val _themeMode = MutableStateFlow(
        AppThemeMode.valueOf(prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name)
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _themePreset = MutableStateFlow(
        try {
            MinimalThemePreset.valueOf(prefs.getString("theme_preset", MinimalThemePreset.SLATE.name) ?: MinimalThemePreset.SLATE.name)
        } catch (e: Exception) {
            MinimalThemePreset.SLATE
        }
    )
    val themePreset: StateFlow<MinimalThemePreset> = _themePreset.asStateFlow()

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean("sound_enabled", true))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(prefs.getBoolean("vibration_enabled", true))
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _ruleDefaults = MutableStateFlow(
        MatchRuleDefaults(
            targetPoints = prefs.getInt("target_points", 29),
            nillBoardThreshold = prefs.getInt("nill_board_threshold", 7),
            queenPoints = prefs.getInt("queen_points", 5),
            queenStopThreshold = prefs.getInt("queen_stop_threshold", 19),
            enableQueenStopRule = prefs.getBoolean("enable_queen_stop_rule", true),
            defaultProMode = prefs.getBoolean("default_pro_mode", true)
        )
    )
    val ruleDefaults: StateFlow<MatchRuleDefaults> = _ruleDefaults.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setThemePreset(preset: MinimalThemePreset) {
        _themePreset.value = preset
        prefs.edit().putString("theme_preset", preset.name).apply()
    }

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _vibrationEnabled.value = enabled
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()
    }

    fun updateRuleDefaults(newRules: MatchRuleDefaults) {
        _ruleDefaults.value = newRules
        prefs.edit()
            .putInt("target_points", newRules.targetPoints)
            .putInt("nill_board_threshold", newRules.nillBoardThreshold)
            .putInt("queen_points", newRules.queenPoints)
            .putInt("queen_stop_threshold", newRules.queenStopThreshold)
            .putBoolean("enable_queen_stop_rule", newRules.enableQueenStopRule)
            .putBoolean("default_pro_mode", newRules.defaultProMode)
            .apply()
    }

    fun resetAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.resetAllData()
            onComplete()
        }
    }
}
