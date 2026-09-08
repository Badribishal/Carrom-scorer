package com.example.carrom.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carrom.data.local.entity.PlayerEntity
import com.example.carrom.engine.PlayerLevelInfo
import com.example.ui.theme.CarromQueenRed
import kotlin.math.max

enum class TrajectoryMetric(val label: String) {
    XP_LEVEL("XP Progression"),
    COINS_POCKETED("Dots & Coins"),
    WIN_RECORD("Win Rate %")
}

data class ChartPoint(
    val label: String,
    val value: Float,
    val displayValue: String
)

/**
 * High-craft custom Canvas Line Graph for Player Statistics & Leveling Trajectory.
 */
@Composable
fun PlayerTrajectoryLineGraph(
    player: PlayerEntity,
    levelInfo: PlayerLevelInfo,
    modifier: Modifier = Modifier
) {
    var selectedMetric by remember { mutableStateOf(TrajectoryMetric.XP_LEVEL) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    // Generate meaningful milestone trajectory points based on cumulative player stats
    val points = remember(player, levelInfo, selectedMetric) {
        when (selectedMetric) {
            TrajectoryMetric.XP_LEVEL -> {
                val p0 = ChartPoint("Rookie", 0f, "0 XP (Lvl 1)")
                val debutXp = if (player.matchesPlayed > 0) 50f else 0f
                val p1 = ChartPoint("Debut", debutXp, "${debutXp.toInt()} XP")
                val midXp = (levelInfo.totalXp * 0.55f).coerceAtLeast(debutXp)
                val p2 = ChartPoint("Career", midXp, "${midXp.toInt()} XP")
                val currentXp = levelInfo.totalXp.toFloat()
                val p3 = ChartPoint("Current", currentXp, "${currentXp.toInt()} XP (Lvl ${levelInfo.level})")
                val nextTarget = levelInfo.nextLevelTargetXp.toFloat()
                val p4 = ChartPoint("Next", nextTarget, "${nextTarget.toInt()} XP (Lvl ${levelInfo.level + 1})")
                listOf(p0, p1, p2, p3, p4)
            }
            TrajectoryMetric.COINS_POCKETED -> {
                val total = player.totalCoinsPocketed.toFloat()
                val white = player.whitePocketed.toFloat()
                val black = player.blackPocketed.toFloat()
                val queens = (player.queensCovered * 3).toFloat()
                listOf(
                    ChartPoint("Start", 0f, "0 Coins"),
                    ChartPoint("White", white, "$white White"),
                    ChartPoint("Black", black, "$black Black"),
                    ChartPoint("Queen", queens, "${player.queensCovered} Queens"),
                    ChartPoint("Total", total, "$total Pocketed")
                )
            }
            TrajectoryMetric.WIN_RECORD -> {
                val winRate = player.winRate
                val boardsWinRate = if (player.boardsPlayed > 0) (player.boardsWon.toFloat() / player.boardsPlayed) * 100f else 0f
                val queenSuccess = player.queenSuccessRate
                listOf(
                    ChartPoint("Base", 0f, "0%"),
                    ChartPoint("Boards", boardsWinRate, "${"%.1f".format(boardsWinRate)}%"),
                    ChartPoint("Queens", queenSuccess, "${"%.1f".format(queenSuccess)}%"),
                    ChartPoint("Matches", winRate, "${"%.1f".format(winRate)}%"),
                    ChartPoint("Target", 100f, "100% Target")
                )
            }
        }
    }

    val primaryColor = when (selectedMetric) {
        TrajectoryMetric.XP_LEVEL -> levelInfo.tier.color
        TrajectoryMetric.COINS_POCKETED -> MaterialTheme.colorScheme.primary
        TrajectoryMetric.WIN_RECORD -> Color(0xFF2E7D32)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.2.dp, primaryColor.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("player_trajectory_line_graph")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Performance Trajectory",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Visual trend across carrom milestones",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Active level pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = levelInfo.tier.color.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, levelInfo.tier.color.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "Lvl ${levelInfo.level} • ${levelInfo.tier.title}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = levelInfo.tier.color,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metric Selector Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TrajectoryMetric.values().forEach { metric ->
                    val isSelected = selectedMetric == metric
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedMetric = metric
                            selectedPointIndex = null
                        },
                        label = {
                            Text(
                                text = metric.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Canvas Line Graph
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val paddingBottom = 24.dp.toPx()
                    val paddingTop = 16.dp.toPx()
                    val paddingHorizontal = 20.dp.toPx()

                    val chartWidth = width - (paddingHorizontal * 2)
                    val chartHeight = height - paddingTop - paddingBottom

                    val maxValue = max(points.maxOfOrNull { it.value } ?: 1f, 1f)
                    val minValue = 0f

                    // Draw 3 subtle horizontal guide lines
                    val gridSteps = 3
                    for (i in 0..gridSteps) {
                        val y = paddingTop + (chartHeight * (i.toFloat() / gridSteps))
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.15f),
                            start = Offset(paddingHorizontal, y),
                            end = Offset(width - paddingHorizontal, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    if (points.size >= 2) {
                        val stepX = chartWidth / (points.size - 1)

                        // Build curve path
                        val linePath = Path()
                        val fillPath = Path()

                        val coords = points.mapIndexed { index, point ->
                            val x = paddingHorizontal + (index * stepX)
                            val normalizedY = (point.value - minValue) / (maxValue - minValue)
                            val y = paddingTop + chartHeight * (1f - normalizedY.coerceIn(0f, 1f))
                            Offset(x, y)
                        }

                        linePath.moveTo(coords.first().x, coords.first().y)
                        fillPath.moveTo(coords.first().x, paddingTop + chartHeight)
                        fillPath.lineTo(coords.first().x, coords.first().y)

                        for (i in 0 until coords.size - 1) {
                            val current = coords[i]
                            val next = coords[i + 1]
                            val control1 = Offset(current.x + (next.x - current.x) / 2f, current.y)
                            val control2 = Offset(current.x + (next.x - current.x) / 2f, next.y)
                            linePath.cubicTo(control1.x, control1.y, control2.x, control2.y, next.x, next.y)
                            fillPath.cubicTo(control1.x, control1.y, control2.x, control2.y, next.x, next.y)
                        }

                        fillPath.lineTo(coords.last().x, paddingTop + chartHeight)
                        fillPath.close()

                        // Draw shaded gradient underneath
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.35f),
                                    primaryColor.copy(alpha = 0.02f)
                                ),
                                startY = paddingTop,
                                endY = paddingTop + chartHeight
                            )
                        )

                        // Draw bold smooth line
                        drawPath(
                            path = linePath,
                            color = primaryColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )

                        // Draw point circles and glow
                        coords.forEachIndexed { idx, offset ->
                            val isCurrent = (selectedMetric == TrajectoryMetric.XP_LEVEL && idx == 3) || selectedPointIndex == idx

                            // Glow ring
                            drawCircle(
                                color = if (isCurrent) primaryColor.copy(alpha = 0.35f) else primaryColor.copy(alpha = 0.2f),
                                radius = if (isCurrent) 8.dp.toPx() else 5.dp.toPx(),
                                center = offset
                            )
                            // Outer ring
                            drawCircle(
                                color = primaryColor,
                                radius = if (isCurrent) 5.dp.toPx() else 3.5.dp.toPx(),
                                center = offset
                            )
                            // Inner core
                            drawCircle(
                                color = Color.White,
                                radius = if (isCurrent) 2.5.dp.toPx() else 1.8.dp.toPx(),
                                center = offset
                            )
                        }
                    }
                }
            }

            // X-Axis Labels Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEachIndexed { index, point ->
                    val isCurrent = (selectedMetric == TrajectoryMetric.XP_LEVEL && index == 3)
                    Text(
                        text = point.label,
                        fontSize = 10.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Milestone Insight Card
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (selectedMetric) {
                            TrajectoryMetric.XP_LEVEL -> "Current Level ${levelInfo.level} (${levelInfo.title}): ${levelInfo.xpIntoCurrentLevel}/${levelInfo.xpRequiredForNextLevel} XP needed to unlock Level ${levelInfo.level + 1}."
                            TrajectoryMetric.COINS_POCKETED -> "Total Coins: ${player.totalCoinsPocketed} (${player.whitePocketed} White, ${player.blackPocketed} Black, ${player.queensCovered} Queens Covered)."
                            TrajectoryMetric.WIN_RECORD -> "Career Win Rate: ${"%.1f".format(player.winRate)}% across ${player.matchesPlayed} matches (${player.matchesWon} Won, ${player.matchesLost} Lost)."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
