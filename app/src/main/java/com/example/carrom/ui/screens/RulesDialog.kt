package com.example.carrom.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun RulesDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .testTag("rules_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Carrom Match Rules",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    RuleSection(
                        title = "1. Break & Team Assignment",
                        content = "The team that breaks first plays White coins (9 White). The opposing team plays Black coins (9 Black). There is 1 Queen in the center. Total 19 coins."
                    )

                    RuleSection(
                        title = "2. Turn & Hand System",
                        content = "A Turn belongs to 1 player. During a single turn, the player can pocket multiple coins (+White, +Black), pocket the Queen, and score penalties.\nA Hand is completed when every player in the rotation has completed their turn."
                    )

                    RuleSection(
                        title = "3. Board Scoring",
                        content = "A board ends when one team clears all 9 of their colour coins. The winning team receives points equal to the opposing team's remaining coins on the board."
                    )

                    RuleSection(
                        title = "4. Queen & Cover Rules (5 Points)",
                        content = "The Queen is worth 5 points. When pocketed, it enters 'Pending Cover'. The player MUST pocket their team's colour coin during the SAME turn to cover it.\nIf the turn ends before covering, the Queen returns to the board as Available. The Queen can only be counted once per board."
                    )

                    RuleSection(
                        title = "5. 24+ Queen Rule",
                        content = "Once a team has reached 24 points or more in cumulative match score, Queen points are no longer added toward the 29-point target."
                    )

                    RuleSection(
                        title = "6. 29-Point Winning Target",
                        content = "A match is played up to 29 points. The first team to reach 29 points or higher (e.g. 30) wins the match."
                    )

                    RuleSection(
                        title = "7. Nill Board Rule (Threshold = 7)",
                        content = "If one team wins a board while the opposing team has not reached at least 7 points in total, the losing team is marked with a Nill Board result."
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got It")
                }
            }
        }
    }
}

@Composable
private fun RuleSection(title: String, content: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )
        }
    }
}
