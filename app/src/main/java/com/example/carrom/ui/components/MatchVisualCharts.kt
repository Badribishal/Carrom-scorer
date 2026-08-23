package com.example.carrom.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carrom.engine.BoardRecord
import com.example.ui.theme.CarromQueenRed
import kotlin.math.max

enum class ChartTab {
    SCORE_PROGRESSION,
    BOARD_COMPARISON,
    POINT_SOURCES
}

@Composable
fun MatchVisualSummaryCard(
    team1Name: String,
    team2Name: String,
    team1FinalScore: Int,
    team2FinalScore: Int,
    targetPoints: Int,
    completedBoards: List<BoardRecord>,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ChartTab.SCORE_PROGRESSION) }

    val t1Color = MaterialTheme.colorScheme.primary
    val t2Color = Color(0xFFF57C00) // Amber / Orange for Team 2

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("match_visual_summary_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with title and tab selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Score Breakdown Chart",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Visual match progression & analysis",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Tabs Switcher
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == ChartTab.SCORE_PROGRESSION,
                    onClick = { selectedTab = ChartTab.SCORE_PROGRESSION },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Text("Trajectory", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                SegmentedButton(
                    selected = selectedTab == ChartTab.BOARD_COMPARISON,
                    onClick = { selectedTab = ChartTab.BOARD_COMPARISON },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Text("Boards", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                SegmentedButton(
                    selected = selectedTab == ChartTab.POINT_SOURCES,
                    onClick = { selectedTab = ChartTab.POINT_SOURCES },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) {
                    Text("Sources", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(t1Color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$team1Name ($team1FinalScore pts)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(t2Color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$team2Name ($team2FinalScore pts)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (completedBoards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Score progression chart appears as boards are completed.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                when (selectedTab) {
                    ChartTab.SCORE_PROGRESSION -> {
                        MatchScoreProgressionChart(
                            team1Name = team1Name,
                            team2Name = team2Name,
                            targetPoints = targetPoints,
                            completedBoards = completedBoards,
                            t1Color = t1Color,
                            t2Color = t2Color,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                        )
                    }
                    ChartTab.BOARD_COMPARISON -> {
                        BoardScoreComparisonBarChart(
                            team1Name = team1Name,
                            team2Name = team2Name,
                            completedBoards = completedBoards,
                            t1Color = t1Color,
                            t2Color = t2Color,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                        )
                    }
                    ChartTab.POINT_SOURCES -> {
                        ScoreSourceBreakdownChart(
                            team1Name = team1Name,
                            team2Name = team2Name,
                            team1FinalScore = team1FinalScore,
                            team2FinalScore = team2FinalScore,
                            completedBoards = completedBoards,
                            t1Color = t1Color,
                            t2Color = t2Color,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MatchScoreProgressionChart(
    team1Name: String,
    team2Name: String,
    targetPoints: Int,
    completedBoards: List<BoardRecord>,
    t1Color: Color,
    t2Color: Color,
    modifier: Modifier = Modifier
) {
    // Points history points: B0 (0,0), then each board's team scores
    val t1Points = remember(completedBoards) {
        listOf(0) + completedBoards.map { it.team1ScoreAfterBoard }
    }
    val t2Points = remember(completedBoards) {
        listOf(0) + completedBoards.map { it.team2ScoreAfterBoard }
    }

    val maxScoreInGame = max(targetPoints, max(t1Points.maxOrNull() ?: 0, t2Points.maxOrNull() ?: 0) + 3)
    val totalSteps = t1Points.size // number of points including B0

    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 9.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Canvas(
        modifier = modifier
            .padding(top = 8.dp, bottom = 4.dp, start = 4.dp, end = 4.dp)
            .testTag("score_progression_canvas")
    ) {
        val width = size.width
        val height = size.height
        val paddingLeft = 32.dp.toPx()
        val paddingBottom = 26.dp.toPx()
        val paddingTop = 14.dp.toPx()
        val paddingRight = 14.dp.toPx()

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        if (chartWidth <= 0 || chartHeight <= 0 || totalSteps <= 1) return@Canvas

        // Draw horizontal grid lines and Y-axis score labels
        val yIntervals = listOf(0, targetPoints / 2, 19, targetPoints, maxScoreInGame).distinct().sorted()
        yIntervals.forEach { scoreVal ->
            val yPos = paddingTop + chartHeight - (scoreVal.toFloat() / maxScoreInGame.toFloat()) * chartHeight
            drawLine(
                color = if (scoreVal == targetPoints) t1Color.copy(alpha = 0.5f) else if (scoreVal == 19) CarromQueenRed.copy(alpha = 0.4f) else gridLineColor,
                start = Offset(paddingLeft, yPos),
                end = Offset(width - paddingRight, yPos),
                strokeWidth = if (scoreVal == targetPoints || scoreVal == 19) 1.5.dp.toPx() else 1.dp.toPx(),
                pathEffect = if (scoreVal == targetPoints || scoreVal == 19) PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) else null
            )

            val textLayout = textMeasurer.measure(
                text = "$scoreVal",
                style = labelStyle
            )
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(paddingLeft - textLayout.size.width - 6.dp.toPx(), yPos - textLayout.size.height / 2f)
            )
        }

        // Draw X-axis board steps (B0, B1, B2...)
        val xStep = chartWidth / (totalSteps - 1).coerceAtLeast(1)
        for (i in 0 until totalSteps) {
            val xPos = paddingLeft + i * xStep
            val label = if (i == 0) "Start" else "B$i"
            val textLayout = textMeasurer.measure(
                text = label,
                style = labelStyle
            )
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(xPos - textLayout.size.width / 2f, height - paddingBottom + 6.dp.toPx())
            )
        }

        // Draw Team 1 Path & Filled Area
        val t1Path = Path()
        val t1AreaPath = Path()
        for (i in 0 until totalSteps) {
            val xPos = paddingLeft + i * xStep
            val yPos = paddingTop + chartHeight - (t1Points[i].toFloat() / maxScoreInGame.toFloat()) * chartHeight
            if (i == 0) {
                t1Path.moveTo(xPos, yPos)
                t1AreaPath.moveTo(xPos, paddingTop + chartHeight)
                t1AreaPath.lineTo(xPos, yPos)
            } else {
                t1Path.lineTo(xPos, yPos)
                t1AreaPath.lineTo(xPos, yPos)
            }
        }
        t1AreaPath.lineTo(paddingLeft + (totalSteps - 1) * xStep, paddingTop + chartHeight)
        t1AreaPath.close()

        drawPath(
            path = t1AreaPath,
            brush = Brush.verticalGradient(
                listOf(t1Color.copy(alpha = 0.25f), t1Color.copy(alpha = 0.02f)),
                startY = paddingTop,
                endY = paddingTop + chartHeight
            )
        )

        drawPath(
            path = t1Path,
            color = t1Color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw Team 2 Path & Filled Area
        val t2Path = Path()
        val t2AreaPath = Path()
        for (i in 0 until totalSteps) {
            val xPos = paddingLeft + i * xStep
            val yPos = paddingTop + chartHeight - (t2Points[i].toFloat() / maxScoreInGame.toFloat()) * chartHeight
            if (i == 0) {
                t2Path.moveTo(xPos, yPos)
                t2AreaPath.moveTo(xPos, paddingTop + chartHeight)
                t2AreaPath.lineTo(xPos, yPos)
            } else {
                t2Path.lineTo(xPos, yPos)
                t2AreaPath.lineTo(xPos, yPos)
            }
        }
        t2AreaPath.lineTo(paddingLeft + (totalSteps - 1) * xStep, paddingTop + chartHeight)
        t2AreaPath.close()

        drawPath(
            path = t2AreaPath,
            brush = Brush.verticalGradient(
                listOf(t2Color.copy(alpha = 0.20f), t2Color.copy(alpha = 0.01f)),
                startY = paddingTop,
                endY = paddingTop + chartHeight
            )
        )

        drawPath(
            path = t2Path,
            color = t2Color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw Point Markers with Queen Crown Badges
        for (i in 0 until totalSteps) {
            val xPos = paddingLeft + i * xStep

            // T1 Dot
            val yPos1 = paddingTop + chartHeight - (t1Points[i].toFloat() / maxScoreInGame.toFloat()) * chartHeight
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = Offset(xPos, yPos1)
            )
            drawCircle(
                color = t1Color,
                radius = 3.5.dp.toPx(),
                center = Offset(xPos, yPos1)
            )

            // T2 Dot
            val yPos2 = paddingTop + chartHeight - (t2Points[i].toFloat() / maxScoreInGame.toFloat()) * chartHeight
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = Offset(xPos, yPos2)
            )
            drawCircle(
                color = t2Color,
                radius = 3.5.dp.toPx(),
                center = Offset(xPos, yPos2)
            )

            // Queen Badge indicator if queen was covered in board i
            if (i > 0) {
                val board = completedBoards.getOrNull(i - 1)
                if (board != null && board.queenPointsAwarded > 0) {
                    val queenWinnerY = if (board.winningTeamId == 1) yPos1 else yPos2
                    val badgeColor = CarromQueenRed
                    drawCircle(
                        color = badgeColor,
                        radius = 3.dp.toPx(),
                        center = Offset(xPos, queenWinnerY - 9.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun BoardScoreComparisonBarChart(
    team1Name: String,
    team2Name: String,
    completedBoards: List<BoardRecord>,
    t1Color: Color,
    t2Color: Color,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val maxBoardScore = remember(completedBoards) {
        (completedBoards.maxOfOrNull { it.boardScore } ?: 5).coerceAtLeast(6)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            completedBoards.forEach { board ->
                val isT1 = board.winningTeamId == 1
                val score = board.boardScore
                val heightFraction = (score.toFloat() / maxBoardScore.toFloat()).coerceIn(0.12f, 1f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(52.dp)
                ) {
                    // Score + Queen Indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "+$score",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = if (isT1) t1Color else t2Color
                        )
                        if (board.queenPointsAwarded > 0) {
                            Text(
                                text = "👑",
                                fontSize = 9.sp,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bar
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .fillMaxHeight(heightFraction * 0.72f)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        if (isT1) t1Color else t2Color,
                                        if (isT1) t1Color.copy(alpha = 0.65f) else t2Color.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Board Label
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "B${board.boardNumber}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = if (isT1) "T1" else "T2",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isT1) t1Color else t2Color
                    )
                }
            }
        }
    }
}

@Composable
fun ScoreSourceBreakdownChart(
    team1Name: String,
    team2Name: String,
    team1FinalScore: Int,
    team2FinalScore: Int,
    completedBoards: List<BoardRecord>,
    t1Color: Color,
    t2Color: Color,
    modifier: Modifier = Modifier
) {
    val t1QueenPts = completedBoards.filter { it.winningTeamId == 1 }.sumOf { it.queenPointsAwarded }
    val t1CoinPts = (team1FinalScore - t1QueenPts).coerceAtLeast(0)

    val t2QueenPts = completedBoards.filter { it.winningTeamId == 2 }.sumOf { it.queenPointsAwarded }
    val t2CoinPts = (team2FinalScore - t2QueenPts).coerceAtLeast(0)

    val t1BoardsWon = completedBoards.count { it.winningTeamId == 1 }
    val t2BoardsWon = completedBoards.count { it.winningTeamId == 2 }

    Column(
        modifier = modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Team 1 Breakdown Bar
        TeamPointsSourceBar(
            teamName = team1Name,
            totalPoints = team1FinalScore,
            coinPoints = t1CoinPts,
            queenPoints = t1QueenPts,
            boardsWon = t1BoardsWon,
            primaryColor = t1Color
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Team 2 Breakdown Bar
        TeamPointsSourceBar(
            teamName = team2Name,
            totalPoints = team2FinalScore,
            coinPoints = t2CoinPts,
            queenPoints = t2QueenPts,
            boardsWon = t2BoardsWon,
            primaryColor = t2Color
        )
    }
}

@Composable
private fun TeamPointsSourceBar(
    teamName: String,
    totalPoints: Int,
    coinPoints: Int,
    queenPoints: Int,
    boardsWon: Int,
    primaryColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = teamName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = primaryColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$boardsWon boards won",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = "$totalPoints pts",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = primaryColor
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Stacked Progress Bar
        val coinFraction = if (totalPoints > 0) coinPoints.toFloat() / totalPoints.toFloat() else 0f
        val queenFraction = if (totalPoints > 0) queenPoints.toFloat() / totalPoints.toFloat() else 0f

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (coinFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(coinFraction.coerceAtLeast(0.01f))
                        .background(primaryColor)
                )
            }
            if (queenFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(queenFraction.coerceAtLeast(0.01f))
                        .background(CarromQueenRed)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(primaryColor))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Carrom Coins: $coinPoints pts", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CarromQueenRed))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Queen Bonuses: $queenPoints pts", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
