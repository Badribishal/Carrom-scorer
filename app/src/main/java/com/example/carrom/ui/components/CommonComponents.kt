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
    val infiniteTransition = rememberInfiniteTransition(label = "logo_smoothness")
    
    val pulseScale by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val shimmerOffset by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = -1.2f,
            targetValue = 2.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(3200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_sweep"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    // Pure Navy Blue Background (#0A192F with subtle depth)
    val pureNavyBg = Brush.radialGradient(
        listOf(
            Color(0xFF0F2744), // Rich navy center highlight
            Color(0xFF0A192F), // Pure midnight navy
            Color(0xFF071426), // Deep navy
            Color(0xFF040C18)  // Obsidian navy rim
        )
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .shadow(
                elevation = (size.value * 0.18f).dp,
                shape = CircleShape,
                spotColor = Color(0xFF0A192F).copy(alpha = 0.6f),
                ambientColor = Color.Black.copy(alpha = 0.45f)
            )
            .clip(CircleShape)
            .background(pureNavyBg)
            .border(
                (size.value * 0.045f).coerceAtLeast(1f).dp,
                Brush.sweepGradient(
                    listOf(
                        Color(0xFF1E3A8A),
                        Color(0xFF38BDF8).copy(alpha = 0.6f),
                        Color(0xFF1E3A8A),
                        Color(0xFF60A5FA).copy(alpha = 0.5f),
                        Color(0xFF1E3A8A)
                    )
                ),
                CircleShape
            )
    ) {
        // Specular Shimmer Layer for smooth glass shine
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0f),
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0f)
                        ),
                        start = androidx.compose.ui.geometry.Offset(shimmerOffset * size.value * 2, 0f),
                        end = androidx.compose.ui.geometry.Offset((shimmerOffset + 0.8f) * size.value * 2, size.value * 2)
                    )
                )
        )

        // Outer concentric smooth carrom accent ring
        Box(
            modifier = Modifier
                .size(size * 0.72f)
                .clip(CircleShape)
                .border(
                    0.85.dp,
                    Color(0xFF38BDF8).copy(alpha = 0.35f),
                    CircleShape
                )
        )

        // Inner carrom circle guide
        Box(
            modifier = Modifier
                .size(size * 0.48f)
                .clip(CircleShape)
                .border(0.75.dp, Color(0xFF60A5FA).copy(alpha = 0.45f), CircleShape)
        )

        // Center Carrom Queen Red Dot with radiant glow and specular highlight
        Box(
            modifier = Modifier
                .size(size * 0.32f)
                .scale(pulseScale)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    spotColor = Color(0xFFFF1744).copy(alpha = 0.8f)
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFFF8A80), // Soft pink-red highlight
                            Color(0xFFFF3D00), // Vibrant carrom red-orange
                            Color(0xFFFF1744), // Vivid scarlet
                            Color(0xFFD50000), // Deep crimson
                            Color(0xFF880E4F)  // Rich burgundy edge
                        )
                    )
                )
                .border(0.85.dp, Color(0xFFFFCDD2).copy(alpha = 0.9f), CircleShape)
        ) {
            // Specular Queen reflection dot
            Box(
                modifier = Modifier
                    .size(size * 0.08f)
                    .align(Alignment.TopStart)
                    .offset(x = (size.value * 0.06f).dp, y = (size.value * 0.06f).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.75f))
            )
        }
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
