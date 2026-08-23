package com.example.carrom.ui.components

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carrom.data.local.entity.MatchEntity
import com.example.carrom.export.CarromExportManager
import com.example.carrom.export.ImportType
import com.example.carrom.export.ParsedImportResult
import com.example.carrom.viewmodel.DataExportImportViewModel
import com.example.ui.theme.CarromQueenRed
import java.text.SimpleDateFormat
import java.util.*

enum class ExportOptionType(
    val title: String,
    val subtitle: String,
    val fileExtension: String,
    val mimeType: String,
    val icon: ImageVector,
    val badge: String
) {
    PDF_TOURNAMENT_REPORT(
        title = "Tournament Performance Report",
        subtitle = "Official multi-page vector PDF with leaderboards, rankings & match history",
        fileExtension = "pdf",
        mimeType = "application/pdf",
        icon = Icons.Default.PictureAsPdf,
        badge = "PDF"
    ),
    CSV_MATCH_HISTORY(
        title = "Match Results Summary (CSV)",
        subtitle = "Spreadsheet table of all matches, scores, winner teams & game formats",
        fileExtension = "csv",
        mimeType = "text/csv",
        icon = Icons.Default.TableChart,
        badge = "CSV"
    ),
    CSV_BOARDS_BREAKDOWN(
        title = "Board-by-Board Breakdown (CSV)",
        subtitle = "Detailed board logs with breaker, queen winner, coins left & score progression",
        fileExtension = "csv",
        mimeType = "text/csv",
        icon = Icons.Default.ViewList,
        badge = "CSV"
    ),
    CSV_PLAYER_STATS(
        title = "Player Career Statistics (CSV)",
        subtitle = "Win rates, queen covers, coins pocketed & points for all players",
        fileExtension = "csv",
        mimeType = "text/csv",
        icon = Icons.Default.Leaderboard,
        badge = "CSV"
    ),
    FULL_BACKUP_JSON(
        title = "Complete App Backup (JSON)",
        subtitle = "Full portable snapshot to restore matches and players on any device or version",
        fileExtension = "json",
        mimeType = "application/json",
        icon = Icons.Default.CloudSync,
        badge = "JSON"
    )
}

/**
 * Bottom sheet modal for selecting export format and sharing/saving.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDataBottomSheet(
    exportViewModel: DataExportImportViewModel,
    singleMatch: MatchEntity? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uiState by exportViewModel.uiState.collectAsState()

    var pendingSaveOption by remember { mutableStateOf<ExportOptionType?>(null) }
    var pendingFileToSave by remember { mutableStateOf<java.io.File?>(null) }
    var pendingContentToSave by remember { mutableStateOf<String?>(null) }

    // SAF Save Document Launcher
    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            val success = if (pendingFileToSave != null) {
                CarromExportManager.copyFileToUri(context, pendingFileToSave!!, uri)
            } else if (pendingContentToSave != null) {
                CarromExportManager.writeStringToUri(context, uri, pendingContentToSave!!)
            } else {
                false
            }

            if (success) {
                Toast.makeText(context, "Saved successfully to your device!", Toast.LENGTH_SHORT).show()
                onDismiss()
            } else {
                Toast.makeText(context, "Failed to save file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (singleMatch != null) "Export Match Scorecard" else "Export Performance & Data",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (singleMatch != null) "Share official scorecard or save spreadsheet CSV" else "Export reports as PDF, CSV spreadsheets, or full JSON backups",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }

            // Single Match specific Quick Cards
            if (singleMatch != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = CarromQueenRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Official Match PDF Scorecard",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = "Vector PDF scorecard containing team rosters, winner banner, and board-by-board score progressions.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    exportViewModel.exportSingleMatchPdf(context, singleMatch, share = true) {
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_single_match_pdf_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share PDF")
                            }

                            OutlinedButton(
                                onClick = {
                                    exportViewModel.exportSingleMatchPdf(context, singleMatch, share = false) { file ->
                                        pendingFileToSave = file
                                        saveDocumentLauncher.launch(file.name)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_single_match_pdf_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save PDF")
                            }
                        }
                    }
                }
            }

            Text(
                text = "Export Formats",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            ExportOptionType.values().forEach { option ->
                ExportOptionCard(
                    option = option,
                    onShare = {
                        when (option) {
                            ExportOptionType.PDF_TOURNAMENT_REPORT -> exportViewModel.exportTournamentReportPdf(context, share = true) { onDismiss() }
                            ExportOptionType.CSV_MATCH_HISTORY -> exportViewModel.exportMatchesCsv(context, share = true) { _, _ -> onDismiss() }
                            ExportOptionType.CSV_BOARDS_BREAKDOWN -> exportViewModel.exportBoardsCsv(context, share = true) { _, _ -> onDismiss() }
                            ExportOptionType.CSV_PLAYER_STATS -> exportViewModel.exportPlayersCsv(context, share = true) { _, _ -> onDismiss() }
                            ExportOptionType.FULL_BACKUP_JSON -> exportViewModel.exportFullBackup(context, share = true) { _, _ -> onDismiss() }
                        }
                    },
                    onSave = {
                        val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                        val defaultFileName = "Carrom_${option.name.lowercase(Locale.ROOT)}_${fileDateFormat.format(Date())}.${option.fileExtension}"

                        when (option) {
                            ExportOptionType.PDF_TOURNAMENT_REPORT -> {
                                exportViewModel.exportTournamentReportPdf(context, share = false) { file ->
                                    pendingFileToSave = file
                                    pendingContentToSave = null
                                    saveDocumentLauncher.launch(file.name)
                                }
                            }
                            ExportOptionType.CSV_MATCH_HISTORY -> {
                                exportViewModel.exportMatchesCsv(context, share = false) { file, content ->
                                    pendingFileToSave = file
                                    pendingContentToSave = content
                                    saveDocumentLauncher.launch(file.name)
                                }
                            }
                            ExportOptionType.CSV_BOARDS_BREAKDOWN -> {
                                exportViewModel.exportBoardsCsv(context, share = false) { file, content ->
                                    pendingFileToSave = file
                                    pendingContentToSave = content
                                    saveDocumentLauncher.launch(file.name)
                                }
                            }
                            ExportOptionType.CSV_PLAYER_STATS -> {
                                exportViewModel.exportPlayersCsv(context, share = false) { file, content ->
                                    pendingFileToSave = file
                                    pendingContentToSave = content
                                    saveDocumentLauncher.launch(file.name)
                                }
                            }
                            ExportOptionType.FULL_BACKUP_JSON -> {
                                exportViewModel.exportFullBackup(context, share = false) { file, content ->
                                    pendingFileToSave = file
                                    pendingContentToSave = content
                                    saveDocumentLauncher.launch(file.name)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ExportOptionCard(
    option: ExportOptionType,
    onShare: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (option == ExportOptionType.PDF_TOURNAMENT_REPORT) CarromQueenRed.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = if (option == ExportOptionType.PDF_TOURNAMENT_REPORT) CarromQueenRed else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = option.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (option == ExportOptionType.PDF_TOURNAMENT_REPORT) CarromQueenRed.copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = option.badge,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (option == ExportOptionType.PDF_TOURNAMENT_REPORT) CarromQueenRed else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = option.subtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSave,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save to Files", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onShare,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", fontSize = 11.sp)
                }
            }
        }
    }
}

/**
 * Confirmation and strategy selection modal for importing files.
 */
@Composable
fun ImportPreviewDialog(
    preview: ParsedImportResult,
    isImporting: Boolean,
    onConfirm: (replaceAll: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var replaceAll by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FileOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Import Data Preview", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (preview.importType) {
                                ImportType.FULL_JSON_BACKUP -> Icons.Default.CloudSync
                                ImportType.MATCHES_CSV -> Icons.Default.TableChart
                                ImportType.PLAYERS_CSV -> Icons.Default.Leaderboard
                                ImportType.UNKNOWN -> Icons.Default.HelpOutline
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = when (preview.importType) {
                                    ImportType.FULL_JSON_BACKUP -> "Carrom Full Backup (JSON)"
                                    ImportType.MATCHES_CSV -> "Matches History (CSV)"
                                    ImportType.PLAYERS_CSV -> "Players Statistics (CSV)"
                                    ImportType.UNKNOWN -> "Import File"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = preview.summary,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Stats breakdown cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Matches Found", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${preview.matches.size}",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Players Found", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${preview.players.size}",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text(
                    text = "Import Strategy",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                // Strategy Option 1: Merge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (!replaceAll) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                    border = BorderStroke(1.dp, if (!replaceAll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { replaceAll = false }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = !replaceAll, onClick = { replaceAll = false })
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Merge with Existing Data (Recommended)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Combines imported matches and players with your existing records.", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Strategy Option 2: Replace All
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (replaceAll) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f) else Color.Transparent,
                    border = BorderStroke(1.dp, if (replaceAll) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { replaceAll = true }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = replaceAll, onClick = { replaceAll = true })
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Replace / Clean Restore", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (replaceAll) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            Text("Clears current records and restores data strictly from this file.", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(replaceAll) },
                enabled = !isImporting,
                modifier = Modifier.testTag("confirm_import_button")
            ) {
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Importing...")
                } else {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import Data")
                }
            }
        },
        dismissButton = {
            if (!isImporting) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
