package com.example.carrom.export

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.example.carrom.data.local.CarromJsonParser
import com.example.carrom.data.local.entity.MatchEntity
import com.example.carrom.data.local.entity.PlayerEntity
import com.example.carrom.engine.BoardRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object CarromPdfExporter {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * Generates a single Match Official Scorecard PDF file.
     */
    fun generateMatchScorecardPdf(
        context: Context,
        match: MatchEntity
    ): File {
        val boards = CarromJsonParser.deserializeBoardRecords(match.boardDetailsJson)
        val pdfDocument = PdfDocument()

        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawMatchScorecardPage(canvas, match, boards)
        pdfDocument.finishPage(page)

        val outputDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val fileName = "Carrom_Match_${match.id}_${fileDateFormat.format(Date(match.timestamp))}.pdf"
        val outputFile = File(outputDir, fileName)

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }

    /**
     * Generates a Tournament & Player Leaderboard Summary PDF file.
     */
    fun generateTournamentReportPdf(
        context: Context,
        matches: List<MatchEntity>,
        players: List<PlayerEntity>
    ): File {
        val pdfDocument = PdfDocument()

        // Page 1: Leaderboard & Overview
        val pageInfo1 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page1 = pdfDocument.startPage(pageInfo1)
        drawTournamentOverviewPage(page1.canvas, matches, players, 1)
        pdfDocument.finishPage(page1)

        // If there are many matches, generate Page 2 for Match Log History
        if (matches.isNotEmpty()) {
            val pageInfo2 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
            val page2 = pdfDocument.startPage(pageInfo2)
            drawMatchHistoryPage(page2.canvas, matches, 2)
            pdfDocument.finishPage(page2)
        }

        val outputDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val fileName = "Carrom_Tournament_Report_${fileDateFormat.format(Date())}.pdf"
        val outputFile = File(outputDir, fileName)

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }

    // =========================================================================
    // DRAWING IMPLEMENTATIONS
    // =========================================================================

    private fun drawMatchScorecardPage(
        canvas: Canvas,
        match: MatchEntity,
        boards: List<BoardRecord>
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Background
        canvas.drawColor(Color.WHITE)

        // 2. Header Banner (Navy #0A192F)
        paint.color = Color.rgb(10, 25, 47)
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 75f, paint)

        // Header accent gold line
        paint.color = Color.rgb(255, 179, 0)
        canvas.drawRect(0f, 75f, PAGE_WIDTH.toFloat(), 78f, paint)

        // Red Queen Emblem in Header
        paint.color = Color.rgb(255, 23, 68)
        canvas.drawCircle(36f, 37f, 14f, paint)
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawCircle(36f, 37f, 10f, paint)
        paint.style = Paint.Style.FILL

        // Header Title
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f
        canvas.drawText("CARROM SCOREKEEPER", 60f, 34f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawText("OFFICIAL MATCH SCORECARD & PERFORMANCE REPORT", 60f, 52f, paint)

        // Date on top right
        val dateStr = dateFormat.format(Date(match.timestamp))
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 9f
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawText(dateStr, (PAGE_WIDTH - 24).toFloat(), 34f, paint)
        canvas.drawText("Match ID: #${match.id}", (PAGE_WIDTH - 24).toFloat(), 50f, paint)
        paint.textAlign = Paint.Align.LEFT

        var curY = 95f

        // 3. Match Overview Meta Chips
        paint.color = Color.rgb(241, 245, 249)
        val metaRect = RectF(24f, curY, (PAGE_WIDTH - 24).toFloat(), curY + 28f)
        canvas.drawRoundRect(metaRect, 6f, 6f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FORMAT: ${if (match.proMode) "PRO MODE" else "STANDARD"}", 36f, curY + 18f, paint)
        canvas.drawText("TARGET: ${match.targetPoints} PTS", 180f, curY + 18f, paint)
        canvas.drawText("FIRST BREAKER: ${match.firstBreakerPlayerName}", 290f, curY + 18f, paint)
        canvas.drawText("TOTAL BOARDS: ${match.boardsCount}", 450f, curY + 18f, paint)

        curY += 40f

        // 4. Team Scores Comparison Card
        paint.color = Color.rgb(248, 250, 252)
        val scoreCardRect = RectF(24f, curY, (PAGE_WIDTH - 24).toFloat(), curY + 92f)
        canvas.drawRoundRect(scoreCardRect, 10f, 10f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRoundRect(scoreCardRect, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        // Team 1 Left Box
        paint.color = Color.rgb(30, 41, 59)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        canvas.drawText(match.team1Name, 44f, curY + 28f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9.5f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText(match.team1PlayerNames, 44f, curY + 44f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 34f
        paint.color = if (match.winnerTeamId == 1) Color.rgb(16, 185, 129) else Color.rgb(15, 23, 42)
        canvas.drawText("${match.team1FinalScore}", 44f, curY + 80f, paint)

        // VS Center
        paint.textSize = 14f
        paint.color = Color.rgb(148, 163, 184)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("VS", (PAGE_WIDTH / 2).toFloat(), curY + 50f, paint)
        paint.textAlign = Paint.Align.LEFT

        // Team 2 Right Box
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(match.team2Name, (PAGE_WIDTH - 44).toFloat(), curY + 28f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9.5f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText(match.team2PlayerNames, (PAGE_WIDTH - 44).toFloat(), curY + 44f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 34f
        paint.color = if (match.winnerTeamId == 2) Color.rgb(16, 185, 129) else Color.rgb(15, 23, 42)
        canvas.drawText("${match.team2FinalScore}", (PAGE_WIDTH - 44).toFloat(), curY + 80f, paint)
        paint.textAlign = Paint.Align.LEFT

        // Winner Banner in center bottom
        val winnerText = if (match.winnerTeamName != null) {
            "🏆 ${match.winnerTeamName.uppercase()} WON THE MATCH"
        } else {
            "DRAW MATCH"
        }
        paint.color = Color.rgb(240, 253, 244)
        val bannerRect = RectF((PAGE_WIDTH / 2 - 110).toFloat(), curY + 66f, (PAGE_WIDTH / 2 + 110).toFloat(), curY + 86f)
        canvas.drawRoundRect(bannerRect, 4f, 4f, paint)

        paint.color = Color.rgb(22, 101, 52)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(winnerText, (PAGE_WIDTH / 2).toFloat(), curY + 80f, paint)
        paint.textAlign = Paint.Align.LEFT

        curY += 110f

        // 5. Board-by-Board Breakdown Table
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("BOARD-BY-BOARD BREAKDOWN", 24f, curY, paint)

        curY += 12f

        // Table Header
        val colWidths = floatArrayOf(45f, 95f, 100f, 65f, 115f, 65f, 62f) // total = 547 (PAGE_WIDTH - 48 = 547)
        val colX = FloatArray(colWidths.size)
        var runningX = 24f
        for (i in colWidths.indices) {
            colX[i] = runningX
            runningX += colWidths[i]
        }

        paint.color = Color.rgb(30, 41, 59)
        canvas.drawRect(24f, curY, (PAGE_WIDTH - 24).toFloat(), curY + 22f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BOARD", colX[0] + 6f, curY + 15f, paint)
        canvas.drawText("BREAKER", colX[1] + 6f, curY + 15f, paint)
        canvas.drawText("WINNER", colX[2] + 6f, curY + 15f, paint)
        canvas.drawText("PTS WON", colX[3] + 6f, curY + 15f, paint)
        canvas.drawText("QUEEN WINNER", colX[4] + 6f, curY + 15f, paint)
        canvas.drawText("T1 SCORE", colX[5] + 6f, curY + 15f, paint)
        canvas.drawText("T2 SCORE", colX[6] + 6f, curY + 15f, paint)

        curY += 22f

        // Table Rows
        var rowAlt = false
        val rowHeight = 22f

        for (b in boards) {
            paint.color = if (rowAlt) Color.rgb(248, 250, 252) else Color.WHITE
            canvas.drawRect(24f, curY, (PAGE_WIDTH - 24).toFloat(), curY + rowHeight, paint)

            // Grid bottom line
            paint.color = Color.rgb(226, 232, 240)
            paint.strokeWidth = 0.5f
            canvas.drawLine(24f, curY + rowHeight, (PAGE_WIDTH - 24).toFloat(), curY + rowHeight, paint)

            // Cell values
            paint.color = Color.rgb(30, 41, 59)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8.5f

            canvas.drawText("#${b.boardNumber}", colX[0] + 6f, curY + 15f, paint)
            canvas.drawText(truncate(b.breakerPlayerName, 14), colX[1] + 6f, curY + 15f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(truncate(b.winningTeamName, 15), colX[2] + 6f, curY + 15f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            paint.color = Color.rgb(16, 185, 129)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("+${b.boardScore} pts", colX[3] + 6f, curY + 15f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            if (b.queenCoveredByPlayerName != null) {
                paint.color = Color.rgb(225, 29, 72)
                val ptsText = if (b.queenPointsAwarded > 0) "(+${b.queenPointsAwarded})" else "(0)"
                paint.typeface = Typeface.create(Typeface.DEFAULT, if (b.queenPointsAwarded > 0) Typeface.BOLD else Typeface.NORMAL)
                canvas.drawText("${truncate(b.queenCoveredByPlayerName, 11)} $ptsText", colX[4] + 6f, curY + 15f, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            } else {
                paint.color = Color.rgb(148, 163, 184)
                canvas.drawText("None (0)", colX[4] + 6f, curY + 15f, paint)
            }

            paint.color = Color.rgb(15, 23, 42)
            canvas.drawText("${b.team1ScoreAfterBoard}", colX[5] + 12f, curY + 15f, paint)
            canvas.drawText("${b.team2ScoreAfterBoard}", colX[6] + 12f, curY + 15f, paint)

            curY += rowHeight
            rowAlt = !rowAlt

            // Prevent overflow
            if (curY > PAGE_HEIGHT - 120) break
        }

        curY += 20f

        // 6. Match Highlights & Official ICF Verification Box
        paint.color = Color.rgb(241, 245, 249)
        val highlightsRect = RectF(24f, curY, (PAGE_WIDTH - 24).toFloat(), curY + 70f)
        canvas.drawRoundRect(highlightsRect, 8f, 8f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10.5f
        canvas.drawText("MATCH NOTES & HIGHLIGHTS", 36f, curY + 20f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        paint.color = Color.rgb(71, 85, 105)

        val nillStatus = if (match.nillBoardOccurred) "⚠️ Nill Board occurred during this match." else "✓ No Nill Board recorded."
        val queensCoveredCount = boards.count { it.queenPointsAwarded > 0 }
        canvas.drawText("• $nillStatus", 36f, curY + 36f, paint)
        canvas.drawText("• Total Queen covers in match: $queensCoveredCount boards.", 36f, curY + 50f, paint)
        canvas.drawText("• Total Hands played across all boards: ${match.handsCount} hands.", 290f, curY + 36f, paint)
        canvas.drawText("• Scored under ICF 29-Point standard rules.", 290f, curY + 50f, paint)

        // 7. Footer
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Generated by Carrom Scorekeeper App • Official Tournament Record • Page 1 of 1", (PAGE_WIDTH / 2).toFloat(), (PAGE_HEIGHT - 24).toFloat(), paint)
    }

    private fun drawTournamentOverviewPage(
        canvas: Canvas,
        matches: List<MatchEntity>,
        players: List<PlayerEntity>,
        pageNumber: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.WHITE)

        // Header Banner
        paint.color = Color.rgb(10, 25, 47)
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 75f, paint)

        paint.color = Color.rgb(255, 179, 0)
        canvas.drawRect(0f, 75f, PAGE_WIDTH.toFloat(), 78f, paint)

        // Queen Emblem
        paint.color = Color.rgb(255, 23, 68)
        canvas.drawCircle(36f, 37f, 14f, paint)
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawCircle(36f, 37f, 10f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f
        canvas.drawText("CARROM PERFORMANCE REPORT", 60f, 34f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawText("CAREER STATISTICS & PLAYER LEADERBOARD", 60f, 52f, paint)

        val dateStr = dateFormat.format(Date())
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 9f
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawText(dateStr, (PAGE_WIDTH - 24).toFloat(), 34f, paint)
        canvas.drawText("Total Players: ${players.size} | Matches: ${matches.size}", (PAGE_WIDTH - 24).toFloat(), 50f, paint)
        paint.textAlign = Paint.Align.LEFT

        var curY = 95f

        // Stats Summary 4-card Grid
        val totalMatchesPlayed = matches.size
        val totalPointsScored = matches.sumOf { it.team1FinalScore + it.team2FinalScore }
        val totalQueensScored = players.sumOf { it.queensCovered }
        val totalCoinsPocketed = players.sumOf { it.totalCoinsPocketed }

        val cardWidth = (PAGE_WIDTH - 48 - 36) / 4f
        val statTitles = arrayOf("MATCHES", "TOTAL POINTS", "QUEENS COVERED", "COINS POCKETED")
        val statValues = arrayOf("$totalMatchesPlayed", "$totalPointsScored", "$totalQueensScored", "$totalCoinsPocketed")

        for (i in 0 until 4) {
            val cx = 24f + i * (cardWidth + 12f)
            paint.color = Color.rgb(248, 250, 252)
            val cRect = RectF(cx, curY, cx + cardWidth, curY + 54f)
            canvas.drawRoundRect(cRect, 6f, 6f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = Color.rgb(226, 232, 240)
            canvas.drawRoundRect(cRect, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.rgb(100, 116, 139)
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(statTitles[i], cx + 8f, curY + 18f, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 18f
            canvas.drawText(statValues[i], cx + 8f, curY + 42f, paint)
        }

        curY += 72f

        // Player Leaderboard Table
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("PLAYER RANKINGS & PERFORMANCE LEADERBOARD", 24f, curY, paint)

        curY += 12f

        // Leaderboard Table Columns
        val colWidths = floatArrayOf(35f, 110f, 65f, 55f, 55f, 65f, 80f, 82f) // total = 547
        val colX = FloatArray(colWidths.size)
        var runningX = 24f
        for (i in colWidths.indices) {
            colX[i] = runningX
            runningX += colWidths[i]
        }

        paint.color = Color.rgb(30, 41, 59)
        canvas.drawRect(24f, curY, (PAGE_WIDTH - 24).toFloat(), curY + 22f, paint)

        paint.color = Color.WHITE
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("RANK", colX[0] + 4f, curY + 15f, paint)
        canvas.drawText("PLAYER", colX[1] + 6f, curY + 15f, paint)
        canvas.drawText("SKILL", colX[2] + 6f, curY + 15f, paint)
        canvas.drawText("PLAYED", colX[3] + 6f, curY + 15f, paint)
        canvas.drawText("WON", colX[4] + 6f, curY + 15f, paint)
        canvas.drawText("WIN %", colX[5] + 6f, curY + 15f, paint)
        canvas.drawText("QUEEN COVERS", colX[6] + 6f, curY + 15f, paint)
        canvas.drawText("TOTAL PTS", colX[7] + 6f, curY + 15f, paint)

        curY += 22f

        // Sorted Players
        val sortedPlayers = players.sortedWith(
            compareByDescending<PlayerEntity> { it.matchesWon }
                .thenByDescending { it.winRate }
                .thenByDescending { it.queensCovered }
        )

        var rowAlt = false
        val rowHeight = 22f

        sortedPlayers.take(24).forEachIndexed { index, p ->
            paint.color = if (rowAlt) Color.rgb(248, 250, 252) else Color.WHITE
            canvas.drawRect(24f, curY, (PAGE_WIDTH - 24).toFloat(), curY + rowHeight, paint)

            paint.color = Color.rgb(226, 232, 240)
            paint.strokeWidth = 0.5f
            canvas.drawLine(24f, curY + rowHeight, (PAGE_WIDTH - 24).toFloat(), curY + rowHeight, paint)

            paint.color = Color.rgb(30, 41, 59)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8.5f

            // Rank #1, #2, #3 badge
            if (index < 3) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = when (index) {
                    0 -> Color.rgb(217, 119, 6) // Gold
                    1 -> Color.rgb(100, 116, 139) // Silver
                    else -> Color.rgb(180, 83, 9) // Bronze
                }
                canvas.drawText("#${index + 1}", colX[0] + 6f, curY + 15f, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            } else {
                paint.color = Color.rgb(71, 85, 105)
                canvas.drawText("#${index + 1}", colX[0] + 6f, curY + 15f, paint)
            }

            paint.color = Color.rgb(15, 23, 42)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(truncate(p.name, 16), colX[1] + 6f, curY + 15f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            paint.color = Color.rgb(100, 116, 139)
            canvas.drawText(truncate(p.skillLevel, 10), colX[2] + 6f, curY + 15f, paint)

            paint.color = Color.rgb(30, 41, 59)
            canvas.drawText("${p.matchesPlayed}", colX[3] + 12f, curY + 15f, paint)
            canvas.drawText("${p.matchesWon}", colX[4] + 12f, curY + 15f, paint)

            paint.color = Color.rgb(16, 185, 129)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${String.format(Locale.US, "%.0f", p.winRate)}%", colX[5] + 10f, curY + 15f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            paint.color = Color.rgb(225, 29, 72)
            canvas.drawText("${p.queensCovered}", colX[6] + 18f, curY + 15f, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${p.totalPointsContributed} pts", colX[7] + 10f, curY + 15f, paint)

            curY += rowHeight
            rowAlt = !rowAlt
        }

        // Footer
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Generated by Carrom Scorekeeper • Page $pageNumber of 2", (PAGE_WIDTH / 2).toFloat(), (PAGE_HEIGHT - 24).toFloat(), paint)
    }

    private fun drawMatchHistoryPage(
        canvas: Canvas,
        matches: List<MatchEntity>,
        pageNumber: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.WHITE)

        // Header
        paint.color = Color.rgb(10, 25, 47)
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 60f, paint)

        paint.color = Color.rgb(255, 179, 0)
        canvas.drawRect(0f, 60f, PAGE_WIDTH.toFloat(), 63f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 15f
        canvas.drawText("MATCH RESULTS LOG", 24f, 38f, paint)

        var curY = 85f

        // Match table columns
        val colWidths = floatArrayOf(80f, 150f, 75f, 110f, 65f, 67f) // total = 547
        val colX = FloatArray(colWidths.size)
        var runningX = 24f
        for (i in colWidths.indices) {
            colX[i] = runningX
            runningX += colWidths[i]
        }

        paint.color = Color.rgb(30, 41, 59)
        canvas.drawRect(24f, curY, (PAGE_WIDTH - 24).toFloat(), curY + 22f, paint)

        paint.color = Color.WHITE
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DATE", colX[0] + 6f, curY + 15f, paint)
        canvas.drawText("MATCH", colX[1] + 6f, curY + 15f, paint)
        canvas.drawText("SCORE", colX[2] + 6f, curY + 15f, paint)
        canvas.drawText("WINNER", colX[3] + 6f, curY + 15f, paint)
        canvas.drawText("BOARDS", colX[4] + 6f, curY + 15f, paint)
        canvas.drawText("MODE", colX[5] + 6f, curY + 15f, paint)

        curY += 22f

        var rowAlt = false
        val rowHeight = 22f
        val simpleDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        matches.take(30).forEach { m ->
            paint.color = if (rowAlt) Color.rgb(248, 250, 252) else Color.WHITE
            canvas.drawRect(24f, curY, (PAGE_WIDTH - 24).toFloat(), curY + rowHeight, paint)

            paint.color = Color.rgb(226, 232, 240)
            paint.strokeWidth = 0.5f
            canvas.drawLine(24f, curY + rowHeight, (PAGE_WIDTH - 24).toFloat(), curY + rowHeight, paint)

            paint.color = Color.rgb(71, 85, 105)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8.5f
            canvas.drawText(simpleDate.format(Date(m.timestamp)), colX[0] + 6f, curY + 15f, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${truncate(m.team1Name, 10)} vs ${truncate(m.team2Name, 10)}", colX[1] + 6f, curY + 15f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            paint.color = Color.rgb(16, 185, 129)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${m.team1FinalScore} - ${m.team2FinalScore}", colX[2] + 6f, curY + 15f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            paint.color = Color.rgb(30, 41, 59)
            canvas.drawText(truncate(m.winnerTeamName ?: "Draw", 15), colX[3] + 6f, curY + 15f, paint)

            paint.color = Color.rgb(100, 116, 139)
            canvas.drawText("${m.boardsCount} bds", colX[4] + 6f, curY + 15f, paint)
            canvas.drawText(if (m.proMode) "Pro" else "Std", colX[5] + 6f, curY + 15f, paint)

            curY += rowHeight
            rowAlt = !rowAlt
        }

        // Footer
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Generated by Carrom Scorekeeper • Page $pageNumber of 2", (PAGE_WIDTH / 2).toFloat(), (PAGE_HEIGHT - 24).toFloat(), paint)
    }

    private fun truncate(text: String, maxLength: Int): String {
        return if (text.length <= maxLength) text else text.substring(0, maxLength - 1) + "…"
    }
}
