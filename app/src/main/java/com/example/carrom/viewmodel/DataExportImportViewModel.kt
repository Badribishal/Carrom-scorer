package com.example.carrom.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.carrom.data.local.CarromDatabase
import com.example.carrom.data.local.entity.MatchEntity
import com.example.carrom.data.local.entity.PlayerEntity
import com.example.carrom.data.repository.CarromRepository
import com.example.carrom.export.CarromCsvExporter
import com.example.carrom.export.CarromExportManager
import com.example.carrom.export.CarromPdfExporter
import com.example.carrom.export.ParsedImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ExportImportUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val pendingImportPreview: ParsedImportResult? = null,
    val lastExportedFile: File? = null,
    val userMessage: String? = null,
    val isError: Boolean = false
)

class DataExportImportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CarromRepository

    init {
        val database = CarromDatabase.getDatabase(application)
        repository = CarromRepository(database)
    }

    private val _uiState = MutableStateFlow(ExportImportUiState())
    val uiState: StateFlow<ExportImportUiState> = _uiState.asStateFlow()

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null, isError = false)
    }

    fun dismissImportPreview() {
        _uiState.value = _uiState.value.copy(pendingImportPreview = null)
    }

    // =========================================================================
    // EXPORT ACTIONS
    // =========================================================================

    /**
     * Export all matches as CSV (and optionally shares via Android Sharesheet)
     */
    fun exportMatchesCsv(context: Context, share: Boolean = true, onFileReady: ((File, String) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            try {
                val matches = repository.getAllMatchesList()
                if (matches.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        userMessage = "No match history available to export.",
                        isError = true
                    )
                    return@launch
                }

                val csv = withContext(Dispatchers.Default) {
                    CarromCsvExporter.exportMatchesToCsv(matches)
                }
                val file = withContext(Dispatchers.IO) {
                    CarromExportManager.createTempCsvFile(context, csv, "Carrom_Matches")
                }

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    lastExportedFile = file,
                    userMessage = "Matches exported successfully (${matches.size} matches)."
                )

                if (share) {
                    CarromExportManager.shareFile(context, file, "text/csv", "Share Carrom Matches CSV")
                }
                onFileReady?.invoke(file, csv)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    userMessage = "Export failed: ${e.localizedMessage}",
                    isError = true
                )
            }
        }
    }

    /**
     * Export detailed board breakdowns as CSV
     */
    fun exportBoardsCsv(context: Context, share: Boolean = true, onFileReady: ((File, String) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            try {
                val matches = repository.getAllMatchesList()
                if (matches.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        userMessage = "No matches available for board export.",
                        isError = true
                    )
                    return@launch
                }

                val csv = withContext(Dispatchers.Default) {
                    CarromCsvExporter.exportBoardsBreakdownToCsv(matches)
                }
                val file = withContext(Dispatchers.IO) {
                    CarromExportManager.createTempCsvFile(context, csv, "Carrom_Boards_Breakdown")
                }

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    lastExportedFile = file,
                    userMessage = "Board breakdown CSV exported successfully."
                )

                if (share) {
                    CarromExportManager.shareFile(context, file, "text/csv", "Share Board Breakdown CSV")
                }
                onFileReady?.invoke(file, csv)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    userMessage = "Export failed: ${e.localizedMessage}",
                    isError = true
                )
            }
        }
    }

    /**
     * Export players career statistics as CSV
     */
    fun exportPlayersCsv(context: Context, share: Boolean = true, onFileReady: ((File, String) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            try {
                val players = repository.getAllPlayersList()
                if (players.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        userMessage = "No players found to export.",
                        isError = true
                    )
                    return@launch
                }

                val csv = withContext(Dispatchers.Default) {
                    CarromCsvExporter.exportPlayersToCsv(players)
                }
                val file = withContext(Dispatchers.IO) {
                    CarromExportManager.createTempCsvFile(context, csv, "Carrom_Players_Stats")
                }

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    lastExportedFile = file,
                    userMessage = "Player career stats exported successfully (${players.size} players)."
                )

                if (share) {
                    CarromExportManager.shareFile(context, file, "text/csv", "Share Player Stats CSV")
                }
                onFileReady?.invoke(file, csv)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    userMessage = "Export failed: ${e.localizedMessage}",
                    isError = true
                )
            }
        }
    }

    /**
     * Export complete database backup as JSON
     */
    fun exportFullBackup(context: Context, share: Boolean = true, onFileReady: ((File, String) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            try {
                val players = repository.getAllPlayersList()
                val matches = repository.getAllMatchesList()

                val json = withContext(Dispatchers.Default) {
                    CarromCsvExporter.exportFullBackupJson(players, matches)
                }
                val file = withContext(Dispatchers.IO) {
                    CarromExportManager.createTempJsonFile(context, json, "Carrom_Backup")
                }

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    lastExportedFile = file,
                    userMessage = "Full backup created (${players.size} players, ${matches.size} matches)."
                )

                if (share) {
                    CarromExportManager.shareFile(context, file, "application/json", "Share Carrom Full Backup")
                }
                onFileReady?.invoke(file, json)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    userMessage = "Backup failed: ${e.localizedMessage}",
                    isError = true
                )
            }
        }
    }

    /**
     * Generate and share a single match PDF scorecard
     */
    fun exportSingleMatchPdf(
        context: Context,
        match: MatchEntity,
        share: Boolean = true,
        onFileReady: ((File) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            try {
                val file = withContext(Dispatchers.IO) {
                    CarromPdfExporter.generateMatchScorecardPdf(context, match)
                }

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    lastExportedFile = file,
                    userMessage = "Official Match PDF Scorecard generated!"
                )

                if (share) {
                    CarromExportManager.shareFile(context, file, "application/pdf", "Share Match Scorecard PDF")
                }
                onFileReady?.invoke(file)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    userMessage = "PDF generation failed: ${e.localizedMessage}",
                    isError = true
                )
            }
        }
    }

    /**
     * Generate and share full tournament and player leaderboard report PDF
     */
    fun exportTournamentReportPdf(
        context: Context,
        share: Boolean = true,
        onFileReady: ((File) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            try {
                val matches = repository.getAllMatchesList()
                val players = repository.getAllPlayersList()

                val file = withContext(Dispatchers.IO) {
                    CarromPdfExporter.generateTournamentReportPdf(context, matches, players)
                }

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    lastExportedFile = file,
                    userMessage = "Tournament Performance Report PDF generated!"
                )

                if (share) {
                    CarromExportManager.shareFile(context, file, "application/pdf", "Share Tournament Report PDF")
                }
                onFileReady?.invoke(file)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    userMessage = "Report generation failed: ${e.localizedMessage}",
                    isError = true
                )
            }
        }
    }

    // =========================================================================
    // IMPORT ACTIONS
    // =========================================================================

    /**
     * Reads and inspects selected file Uri, preparing the import preview dialog
     */
    fun processSelectedImportUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true)
            try {
                val rawText = withContext(Dispatchers.IO) {
                    CarromExportManager.readTextFromUri(context, uri)
                }

                if (rawText.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        userMessage = "Could not read the selected file or file is empty.",
                        isError = true
                    )
                    return@launch
                }

                val parseResult = withContext(Dispatchers.Default) {
                    CarromCsvExporter.parseImportData(rawText)
                }

                if (parseResult.players.isEmpty() && parseResult.matches.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        userMessage = "No valid match or player data found in file. Supported: Carrom CSV exports or JSON backup files.",
                        isError = true
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    pendingImportPreview = parseResult
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    userMessage = "Failed to parse import file: ${e.localizedMessage}",
                    isError = true
                )
            }
        }
    }

    /**
     * Confirms and persists the parsed import data
     */
    fun executeImport(replaceAll: Boolean, onComplete: ((Int, Int) -> Unit)? = null) {
        val preview = _uiState.value.pendingImportPreview ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true)
            try {
                val result = repository.importFullBackup(
                    players = preview.players,
                    matches = preview.matches,
                    replaceAll = replaceAll
                )

                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    pendingImportPreview = null,
                    userMessage = "Successfully imported ${result.first} player profiles and ${result.second} match records!"
                )
                onComplete?.invoke(result.first, result.second)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    userMessage = "Import failed: ${e.localizedMessage}",
                    isError = true
                )
            }
        }
    }
}
