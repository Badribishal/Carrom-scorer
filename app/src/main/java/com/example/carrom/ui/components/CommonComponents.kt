package com.example.carrom.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
    count: Int? = null,
    size: Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    val bgBrush = if (color == TeamColor.WHITE) {
        Brush.radialGradient(
            listOf(
                Color(0xFFFFFDF7),
                Color(0xFFEFEBE9),
                Color(0xFFD7CCC8),
                Color(0xFFBCAAA4)
            )
        )
    } else {
        Brush.radialGradient(
            listOf(
                Color(0xFF424242),
                Color(0xFF263238),
                Color(0xFF212121),
                Color(0xFF111111)
            )
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
    animated: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Plain Navy Blue Background (#0A192F)
    val plainNavyColor = Color(0xFF0A192F)
    val brightRedColor = Color(0xFFFF0000)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .shadow(
                elevation = (size.value * 0.12f).dp,
                shape = CircleShape,
                spotColor = Color(0xFF0A192F).copy(alpha = 0.5f),
                ambientColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(CircleShape)
            .background(plainNavyColor)
    ) {
        // Bright red colour dot
        Box(
            modifier = Modifier
                .size(size * 0.24f)
                .clip(CircleShape)
                .background(brightRedColor)
        )
    }
}

@Composable
fun FrostedFloatingBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
        border = BorderStroke(
            1.2.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.65f),
                    Color.White.copy(alpha = 0.2f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                )
            )
        ),
        shadowElevation = 16.dp,
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                ambientColor = Color.Black.copy(alpha = 0.2f)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

@Composable
fun QueenCoinBadge(
    modifier: Modifier = Modifier,
    status: QueenStatus = QueenStatus.COVERED,
    isCovered: Boolean = true,
    size: Dp = 28.dp
) {
    val covered = status == QueenStatus.COVERED && isCovered
    val pending = status == QueenStatus.PENDING_COVER

    val bgBrush = when {
        covered -> Brush.radialGradient(
            listOf(
                Color(0xFFFF8A80),
                Color(0xFFFF5252),
                CarromQueenRed,
                Color(0xFFB71C1C)
            )
        )
        pending -> Brush.radialGradient(
            listOf(
                Color(0xFFFFD180),
                Color(0xFFFFAB40),
                Color(0xFFFF9100)
            )
        )
        else -> Brush.radialGradient(
            listOf(
                Color(0xFFEF9A9A),
                CarromQueenRed,
                Color(0xFF880E4F)
            )
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .shadow(if (covered || pending) 4.dp else 2.dp, CircleShape)
            .clip(CircleShape)
            .background(bgBrush)
            .border(
                1.5.dp,
                if (covered) Color(0xFFFFD700) else Color(0xFFFFCDD2),
                CircleShape
            )
    ) {
        Text(
            text = "Q",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.5f).sp
        )
    }
}
