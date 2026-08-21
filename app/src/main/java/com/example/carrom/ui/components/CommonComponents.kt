package com.example.carrom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carrom.engine.QueenStatus
import com.example.carrom.engine.TeamColor
import com.example.ui.theme.*

val AvatarPalette = listOf(
    Color(0xFFE53935),
    Color(0xFF1E88E5),
    Color(0xFF43A047),
    Color(0xFFFB8C00),
    Color(0xFF8E24AA),
    Color(0xFF00ACC1),
    Color(0xFFD81B60),
    Color(0xFF5E35B1)
)

@Composable
fun PlayerAvatar(
    name: String,
    avatarColorIndex: Int = 0,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val color = AvatarPalette.getOrElse(avatarColorIndex % AvatarPalette.size) { AvatarPalette[0] }
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.45f).sp
        )
    }
}

@Composable
fun CarromCoinBadge(
    color: TeamColor,
    size: Dp = 24.dp,
    count: Int? = null,
    modifier: Modifier = Modifier
) {
    val bgBrush = if (color == TeamColor.WHITE) {
        Brush.radialGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFE0D8CB), Color(0xFFC7BBAA))
        )
    } else {
        Brush.radialGradient(
            listOf(Color(0xFF424242), Color(0xFF212121), Color(0xFF111111))
        )
    }

    val borderCol = if (color == TeamColor.WHITE) Color(0xFFBCAAA4) else Color(0xFF616161)
    val textCol = if (color == TeamColor.WHITE) Color(0xFF3E2723) else Color(0xFFFAFAFA)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(bgBrush)
            .border(1.dp, borderCol, CircleShape)
    ) {
        if (count != null) {
            Text(
                text = count.toString(),
                color = textCol,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.48f).sp
            )
        }
    }
}

@Composable
fun NavyRedDotLogo(
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val navyBg = Brush.radialGradient(
        listOf(
            Color(0xFF0F1F47),
            Color(0xFF0A1430),
            Color(0xFF060B1C)
        )
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(navyBg)
            .border(1.5.dp, Color(0xFF1E3A8A), CircleShape)
    ) {
        // Outer concentric accent ring
        Box(
            modifier = Modifier
                .size(size * 0.65f)
                .clip(CircleShape)
                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), CircleShape)
        )

        // Center Red Dot
        Box(
            modifier = Modifier
                .size(size * 0.28f)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFFF5252),
                            Color(0xFFE53935),
                            Color(0xFFB71C1C)
                        )
                    )
                )
                .border(0.75.dp, Color(0xFFFFCDD2), CircleShape)
        )
    }
}

@Composable
fun QueenCoinBadge(
    size: Dp = 24.dp,
    isCovered: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bgBrush = Brush.radialGradient(
        listOf(CarromQueenGold, CarromQueenRed, Color(0xFF8B0000))
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .shadow(3.dp, CircleShape)
            .clip(CircleShape)
            .background(bgBrush)
            .border(1.5.dp, if (isCovered) Color(0xFFFFD700) else Color(0xFFFFCDD2), CircleShape)
    ) {
        Text(
            text = "Q",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.5f).sp
        )
    }
}

@Composable
fun QueenStatusIndicator(
    status: QueenStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label, icon) = when (status) {
        QueenStatus.AVAILABLE -> Quadruple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Queen Available (5 pts)",
            Icons.Default.Adjust
        )
        QueenStatus.PENDING_COVER -> Quadruple(
            Color(0xFFFFE082),
            Color(0xFFE65100),
            "Queen Pocketed - PENDING COVER!",
            Icons.Default.HourglassTop
        )
        QueenStatus.COVERED -> Quadruple(
            Color(0xFFC8E6C9),
            Color(0xFF1B5E20),
            "Queen Successfully COVERED (+5 pts)",
            Icons.Default.CheckCircle
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
            .fillMaxWidth()
            .testTag("queen_status_indicator")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            QueenCoinBadge(size = 22.dp, isCovered = status == QueenStatus.COVERED)
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
