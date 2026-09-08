package com.example

import com.example.carrom.data.local.entity.GroupEntity
import com.example.carrom.data.local.entity.PlayerEntity
import com.example.carrom.export.CarromCsvExporter
import com.example.carrom.export.ImportType
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GroupExportImportTest {

    @Test
    fun testExportAndParseGroupsJson() {
        val groups = listOf(
            GroupEntity(
                id = 1L,
                name = "Friday Team",
                description = "Weekend Champions",
                colorIndex = 2,
                team1Name = "Strikers",
                team2Name = "Defenders",
                team1Player1 = "Rahul",
                team1Player2 = "Amit",
                team2Player1 = "Suman",
                team2Player2 = "Raj",
                isDoubles = true,
                memberPlayerIds = "1,2,3,4"
            ),
            GroupEntity(
                id = 2L,
                name = "Singles Duel",
                description = "Head to Head",
                colorIndex = 4,
                team1Name = "Player A",
                team2Name = "Player B",
                team1Player1 = "Player A",
                team1Player2 = "",
                team2Player1 = "Player B",
                team2Player2 = "",
                isDoubles = false,
                memberPlayerIds = "5,6"
            )
        )

        val json = CarromCsvExporter.exportGroupsJson(groups)
        assertTrue(json.contains("Friday Team"))
        assertTrue(json.contains("Rahul"))
        assertTrue(json.contains("Singles Duel"))

        val result = CarromCsvExporter.parseImportData(json)
        assertEquals(ImportType.GROUPS_JSON, result.importType)
        assertEquals(2, result.groups.size)

        val group1 = result.groups.find { it.name == "Friday Team" }
        assertNotNull(group1)
        assertEquals("Rahul", group1?.team1Player1)
        assertEquals("Amit", group1?.team1Player2)
        assertEquals("Suman", group1?.team2Player1)
        assertEquals("Raj", group1?.team2Player2)
        assertTrue(group1?.isDoubles == true)

        val group2 = result.groups.find { it.name == "Singles Duel" }
        assertNotNull(group2)
        assertFalse(group2?.isDoubles == true)
    }

    @Test
    fun testExportAndParseGroupsCsv() {
        val groups = listOf(
            GroupEntity(
                id = 10L,
                name = "College Group",
                description = "Campus Legends",
                colorIndex = 3,
                team1Name = "Alpha",
                team2Name = "Beta",
                team1Player1 = "Player A",
                team1Player2 = "Player B",
                team2Player1 = "Player C",
                team2Player2 = "Player D",
                isDoubles = true,
                memberPlayerIds = "10,11,12,13"
            )
        )

        val csv = CarromCsvExporter.exportGroupsToCsv(groups)
        assertTrue(csv.contains("College Group"))
        assertTrue(csv.contains("Campus Legends"))
        assertTrue(csv.contains("Player A"))

        val result = CarromCsvExporter.parseImportData(csv)
        assertEquals(ImportType.GROUPS_CSV, result.importType)
        assertEquals(1, result.groups.size)

        val parsed = result.groups.first()
        assertEquals("College Group", parsed.name)
        assertEquals("Player A", parsed.team1Player1)
        assertEquals("Player B", parsed.team1Player2)
        assertEquals("Player C", parsed.team2Player1)
        assertEquals("Player D", parsed.team2Player2)
        assertTrue(parsed.isDoubles)
    }

    @Test
    fun testFullBackupJsonWithGroups() {
        val players = listOf(
            PlayerEntity(id = 1L, name = "Rahul", skillLevel = "Advanced")
        )
        val groups = listOf(
            GroupEntity(
                id = 1L,
                name = "Friday Team",
                team1Player1 = "Rahul",
                team1Player2 = "Amit",
                team2Player1 = "Suman",
                team2Player2 = "Raj",
                isDoubles = true
            )
        )

        val backupJson = CarromCsvExporter.exportFullBackupJson(
            players = players,
            matches = emptyList(),
            groups = groups
        )

        val result = CarromCsvExporter.parseImportData(backupJson)
        assertEquals(ImportType.FULL_JSON_BACKUP, result.importType)
        assertEquals(1, result.players.size)
        assertEquals(1, result.groups.size)
        assertEquals("Friday Team", result.groups[0].name)
    }
}
