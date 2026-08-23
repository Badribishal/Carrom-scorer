package com.example.carrom.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
                            text = "Official Carrom Rules",
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
                        title = "1. Break & Team Colors",
                        content = "The player/team taking the opening break plays White coins (9 White). The opposing team plays Black coins (9 Black). There is 1 Queen in the center circle. Total 19 coins."
                    )

                    RuleSection(
                        title = "2. Turn & Hand System",
                        content = "A Turn belongs to one active player until their turn ends. A Hand is completed when every player in the rotation has completed their turn."
                    )

                    RuleSection(
                        title = "3. Board Scoring",
                        content = "A board ends when one team pockets all 9 of their assigned colour coins. The winning team receives 1 point per remaining opponent coin on the board."
                    )

                    RuleSection(
                        title = "4. Queen & Cover Rules (5 Points)",
                        content = "The Queen is worth 5 points. To successfully claim the Queen, a player must pocket the Queen and cover it with one of their team's own colour coins during the SAME turn. If the turn ends without covering, the Queen returns to the center."
                    )

                    RuleSection(
                        title = "5. 24-Point Queen Cutoff Rule",
                        content = "Once a team has reached 24 points or higher in cumulative match score, Queen bonus points (+5) are NO LONGER credited to that team's score upon winning a board. The team can only score points from opponent remaining coins on the board."
                    )

                    RuleSection(
                        title = "6. 29-Point Winning Target",
                        content = "A regulation match is played up to 29 points. The first team to reach or exceed 29 points wins the match."
                    )

                    RuleSection(
                        title = "7. Nill Board Match Win Rule",
                        content = "If a team reaches winning threshold (24+ points) while the opposing team has failed to score at least 7 points (score < 7), the leading team immediately wins the match by Nill Board victory."
                    )

                    RuleSection(
                        title = "8. Penalties & Fouls",
                        content = "Pocketing the striker or improper shots incur a penalty coin (Due), returning a coin to the center circle."
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got It")
                }
            }
        }
    }
}

@Composable
private fun RuleSection(
    title: String,
    content: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}
