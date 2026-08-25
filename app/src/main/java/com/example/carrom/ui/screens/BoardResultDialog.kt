package com.example.carrom.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.carrom.engine.BoardRecord
import com.example.carrom.engine.TeamColor
import com.example.carrom.ui.components.CarromCoinBadge
import com.example.carrom.ui.components.QueenCoinBadge
import com.example.ui.theme.CarromQueenRed

@Composable
fun BoardResultDialog(
    board: BoardRecord,
    team1Name: String,
    team2Name: String,
    nextBoardNumber: Int? = null,
    nextBreakerName: String? = null,
    nextBreakerTeamName: String? = null,
    onDismiss: () -> Unit,
    onStartNextBoard: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("board_result_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF8F00)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFF3E2723),
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (board.isNillMatchWin) "NILL BOARD MATCH WIN" else "BOARD ${board.boardNumber} FINISHED",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (board.isNillMatchWin) Color(0xFFC2185B) else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = if (board.isNillMatchWin) "${board.winningTeamName} Wins Match!" else "${board.winningTeamName} Wins Board!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    if (board.isNillMatchWin) {
                        Text(
                            text = "Nill Board Rule: 19+ points vs <7 points",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF880E4F),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Points Breakdown Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Opponent Coins Left", fontSize = 13.sp)
                            Text("+${board.opponentRemainingCoins}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                QueenCoinBadge(size = 16.dp, isCovered = board.queenCoveredByPlayerId != null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when {
                                        board.queenCoveredByTeamId == board.winningTeamId -> {
                                            if (board.queenPointsAwarded > 0) {
                                                "Queen Bonus (${board.queenCoveredByPlayerName ?: "Winner"})"
                                            } else {
                                                "Queen Covered by ${board.queenCoveredByPlayerName ?: "Winner"} (24+ Rule: 0 pts)"
                                            }
                                        }
                                        board.queenCoveredByPlayerName != null -> {
                                            "Queen covered by ${board.queenCoveredByPlayerName} (No winner bonus)"
                                        }
                                        else -> "Queen (Not Covered)"
                                    },
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = "+${board.queenPointsAwarded}",
                                fontWeight = FontWeight.Bold,
                                color = if (board.queenPointsAwarded > 0) CarromQueenRed else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Board Total", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "+${board.boardScore} pts",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Cumulative Score Row
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(team1Name, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(
                                text = "${board.team1ScoreAfterBoard}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            text = "VS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(team2Name, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(
                                text = "${board.team2ScoreAfterBoard}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Next Breaker Notice
                if (!board.isNillMatchWin && nextBreakerName != null && nextBoardNumber != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Next Break (B$nextBoardNumber): $nextBreakerName ($nextBreakerTeamName)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onStartNextBoard,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_next_board_button")
                    ) {
                        Icon(imageVector = if (board.isNillMatchWin) Icons.Default.EmojiEvents else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (board.isNillMatchWin) "View Match Summary" else if (nextBoardNumber != null) "Start Board $nextBoardNumber" else "Next Board",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("dismiss_board_dialog_button")
                    ) {
                        Text("View Board")
                    }
                }
            }
        }
    }
}
