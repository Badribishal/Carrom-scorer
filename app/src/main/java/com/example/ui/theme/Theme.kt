package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class MinimalThemePreset(
    val displayName: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundColor: Color
) {
    SLATE(
        "Minimal Slate",
        "Clean charcoal & neutral slate with crisp modern contrast",
        SlatePrimaryLight,
        SlateSecondaryLight,
        SlateBackgroundLight
    ),
    IVORY(
        "Nordic Ivory",
        "Warm off-white birch with subtle organic linen tones",
        IvoryPrimaryLight,
        IvorySecondaryLight,
        IvoryBackgroundLight
    ),
    OBSIDIAN(
        "Midnight Obsidian",
        "True black OLED with razor-sharp minimalist accents",
        ObsidianPrimaryDark,
        ObsidianSecondaryDark,
        ObsidianBackgroundDark
    ),
    SAGE(
        "Zen Sage",
        "Calming muted botanical sage & soft graphite balance",
        SagePrimaryLight,
        SageSecondaryLight,
        SageBackgroundLight
    ),
    CLASSIC(
        "Classic Carrom",
        "Warm amber timber tones for traditional carrom heritage",
        AmberPrimaryLight,
        AmberSecondaryLight,
        AmberBackgroundLight
    )
}

// 1. Minimal Slate Color Schemes
private val SlateLightColorScheme = lightColorScheme(
    primary = SlatePrimaryLight,
    onPrimary = SlateOnPrimaryLight,
    primaryContainer = SlatePrimaryContainerLight,
    onPrimaryContainer = SlateOnPrimaryContainerLight,
    secondary = SlateSecondaryLight,
    onSecondary = SlateOnSecondaryLight,
    secondaryContainer = SlateSecondaryContainerLight,
    onSecondaryContainer = SlateOnSecondaryContainerLight,
    tertiary = SlateTertiaryLight,
    onTertiary = SlateOnTertiaryLight,
    tertiaryContainer = SlateTertiaryContainerLight,
    onTertiaryContainer = SlateOnTertiaryContainerLight,
    background = SlateBackgroundLight,
    onBackground = SlateOnBackgroundLight,
    surface = SlateSurfaceLight,
    onSurface = SlateOnSurfaceLight,
    surfaceVariant = SlateSurfaceVariantLight,
    onSurfaceVariant = SlateOnSurfaceVariantLight,
    outline = SlateOutlineLight
)

private val SlateDarkColorScheme = darkColorScheme(
    primary = SlatePrimaryDark,
    onPrimary = SlateOnPrimaryDark,
    primaryContainer = SlatePrimaryContainerDark,
    onPrimaryContainer = SlateOnPrimaryContainerDark,
    secondary = SlateSecondaryDark,
    onSecondary = SlateOnSecondaryDark,
    secondaryContainer = SlateSecondaryContainerDark,
    onSecondaryContainer = SlateOnSecondaryContainerDark,
    tertiary = SlateTertiaryDark,
    onTertiary = SlateOnTertiaryDark,
    tertiaryContainer = SlateTertiaryContainerDark,
    onTertiaryContainer = SlateOnTertiaryContainerDark,
    background = SlateBackgroundDark,
    onBackground = SlateOnBackgroundDark,
    surface = SlateSurfaceDark,
    onSurface = SlateOnSurfaceDark,
    surfaceVariant = SlateSurfaceVariantDark,
    onSurfaceVariant = SlateOnSurfaceVariantDark,
    outline = SlateOutlineDark
)

// 2. Nordic Ivory Color Schemes
private val IvoryLightColorScheme = lightColorScheme(
    primary = IvoryPrimaryLight,
    onPrimary = IvoryOnPrimaryLight,
    primaryContainer = IvoryPrimaryContainerLight,
    onPrimaryContainer = IvoryOnPrimaryContainerLight,
    secondary = IvorySecondaryLight,
    onSecondary = IvoryOnSecondaryLight,
    secondaryContainer = IvorySecondaryContainerLight,
    onSecondaryContainer = IvoryOnSecondaryContainerLight,
    tertiary = IvoryTertiaryLight,
    onTertiary = IvoryOnTertiaryLight,
    tertiaryContainer = IvoryTertiaryContainerLight,
    onTertiaryContainer = IvoryOnTertiaryContainerLight,
    background = IvoryBackgroundLight,
    onBackground = IvoryOnBackgroundLight,
    surface = IvorySurfaceLight,
    onSurface = IvoryOnSurfaceLight,
    surfaceVariant = IvorySurfaceVariantLight,
    onSurfaceVariant = IvoryOnSurfaceVariantLight,
    outline = IvoryOutlineLight
)

private val IvoryDarkColorScheme = darkColorScheme(
    primary = IvoryPrimaryDark,
    onPrimary = IvoryOnPrimaryDark,
    primaryContainer = IvoryPrimaryContainerDark,
    onPrimaryContainer = IvoryOnPrimaryContainerDark,
    secondary = IvorySecondaryDark,
    onSecondary = IvoryOnSecondaryDark,
    secondaryContainer = IvorySecondaryContainerDark,
    onSecondaryContainer = IvoryOnSecondaryContainerDark,
    tertiary = IvoryTertiaryDark,
    onTertiary = IvoryOnTertiaryDark,
    tertiaryContainer = IvoryTertiaryContainerDark,
    onTertiaryContainer = IvoryOnTertiaryContainerDark,
    background = IvoryBackgroundDark,
    onBackground = IvoryOnBackgroundDark,
    surface = IvorySurfaceDark,
    onSurface = IvoryOnSurfaceDark,
    surfaceVariant = IvorySurfaceVariantDark,
    onSurfaceVariant = IvoryOnSurfaceVariantDark,
    outline = IvoryOutlineDark
)

// 3. Midnight Obsidian Color Schemes
private val ObsidianLightColorScheme = lightColorScheme(
    primary = ObsidianPrimaryLight,
    onPrimary = ObsidianOnPrimaryLight,
    primaryContainer = ObsidianPrimaryContainerLight,
    onPrimaryContainer = ObsidianOnPrimaryContainerLight,
    secondary = ObsidianSecondaryLight,
    onSecondary = ObsidianOnSecondaryLight,
    secondaryContainer = ObsidianSecondaryContainerLight,
    onSecondaryContainer = ObsidianOnSecondaryContainerLight,
    tertiary = ObsidianTertiaryLight,
    onTertiary = ObsidianOnTertiaryLight,
    tertiaryContainer = ObsidianTertiaryContainerLight,
    onTertiaryContainer = ObsidianOnTertiaryContainerLight,
    background = ObsidianBackgroundLight,
    onBackground = ObsidianOnBackgroundLight,
    surface = ObsidianSurfaceLight,
    onSurface = ObsidianOnSurfaceLight,
    surfaceVariant = ObsidianSurfaceVariantLight,
    onSurfaceVariant = ObsidianOnSurfaceVariantLight,
    outline = ObsidianOutlineLight
)

private val ObsidianDarkColorScheme = darkColorScheme(
    primary = ObsidianPrimaryDark,
    onPrimary = ObsidianOnPrimaryDark,
    primaryContainer = ObsidianPrimaryContainerDark,
    onPrimaryContainer = ObsidianOnPrimaryContainerDark,
    secondary = ObsidianSecondaryDark,
    onSecondary = ObsidianOnSecondaryDark,
    secondaryContainer = ObsidianSecondaryContainerDark,
    onSecondaryContainer = ObsidianOnSecondaryContainerDark,
    tertiary = ObsidianTertiaryDark,
    onTertiary = ObsidianOnTertiaryDark,
    tertiaryContainer = ObsidianTertiaryContainerDark,
    onTertiaryContainer = ObsidianOnTertiaryContainerDark,
    background = ObsidianBackgroundDark,
    onBackground = ObsidianOnBackgroundDark,
    surface = ObsidianSurfaceDark,
    onSurface = ObsidianOnSurfaceDark,
    surfaceVariant = ObsidianSurfaceVariantDark,
    onSurfaceVariant = ObsidianOnSurfaceVariantDark,
    outline = ObsidianOutlineDark
)

// 4. Zen Sage Color Schemes
private val SageLightColorScheme = lightColorScheme(
    primary = SagePrimaryLight,
    onPrimary = SageOnPrimaryLight,
    primaryContainer = SagePrimaryContainerLight,
    onPrimaryContainer = SageOnPrimaryContainerLight,
    secondary = SageSecondaryLight,
    onSecondary = SageOnSecondaryLight,
    secondaryContainer = SageSecondaryContainerLight,
    onSecondaryContainer = SageOnSecondaryContainerLight,
    tertiary = SageTertiaryLight,
    onTertiary = SageOnTertiaryLight,
    tertiaryContainer = SageTertiaryContainerLight,
    onTertiaryContainer = SageOnTertiaryContainerLight,
    background = SageBackgroundLight,
    onBackground = SageOnBackgroundLight,
    surface = SageSurfaceLight,
    onSurface = SageOnSurfaceLight,
    surfaceVariant = SageSurfaceVariantLight,
    onSurfaceVariant = SageOnSurfaceVariantLight,
    outline = SageOutlineLight
)

private val SageDarkColorScheme = darkColorScheme(
    primary = SagePrimaryDark,
    onPrimary = SageOnPrimaryDark,
    primaryContainer = SagePrimaryContainerDark,
    onPrimaryContainer = SageOnPrimaryContainerDark,
    secondary = SageSecondaryDark,
    onSecondary = SageOnSecondaryDark,
    secondaryContainer = SageSecondaryContainerDark,
    onSecondaryContainer = SageOnSecondaryContainerDark,
    tertiary = SageTertiaryDark,
    onTertiary = SageOnTertiaryDark,
    tertiaryContainer = SageTertiaryContainerDark,
    onTertiaryContainer = SageOnTertiaryContainerDark,
    background = SageBackgroundDark,
    onBackground = SageOnBackgroundDark,
    surface = SageSurfaceDark,
    onSurface = SageOnSurfaceDark,
    surfaceVariant = SageSurfaceVariantDark,
    onSurfaceVariant = SageOnSurfaceVariantDark,
    outline = SageOutlineDark
)

// 5. Classic Carrom (Wood & Amber)
private val AmberLightColorScheme = lightColorScheme(
    primary = AmberPrimaryLight,
    onPrimary = AmberOnPrimaryLight,
    primaryContainer = AmberPrimaryContainerLight,
    onPrimaryContainer = AmberOnPrimaryContainerLight,
    secondary = AmberSecondaryLight,
    onSecondary = AmberOnSecondaryLight,
    secondaryContainer = AmberSecondaryContainerLight,
    onSecondaryContainer = AmberOnSecondaryContainerLight,
    tertiary = AmberTertiaryLight,
    onTertiary = AmberOnTertiaryLight,
    tertiaryContainer = AmberTertiaryContainerLight,
    onTertiaryContainer = AmberOnTertiaryContainerLight,
    background = AmberBackgroundLight,
    onBackground = AmberOnBackgroundLight,
    surface = AmberSurfaceLight,
    onSurface = AmberOnSurfaceLight,
    surfaceVariant = AmberSurfaceVariantLight,
    onSurfaceVariant = AmberOnSurfaceVariantLight,
    outline = AmberOutlineLight
)

private val AmberDarkColorScheme = darkColorScheme(
    primary = AmberPrimaryDark,
    onPrimary = AmberOnPrimaryDark,
    primaryContainer = AmberPrimaryContainerDark,
    onPrimaryContainer = AmberOnPrimaryContainerDark,
    secondary = AmberSecondaryDark,
    onSecondary = AmberOnSecondaryDark,
    secondaryContainer = AmberSecondaryContainerDark,
    onSecondaryContainer = AmberOnSecondaryContainerDark,
    tertiary = AmberTertiaryDark,
    onTertiary = AmberOnTertiaryDark,
    tertiaryContainer = AmberTertiaryContainerDark,
    onTertiaryContainer = AmberOnTertiaryContainerDark,
    background = AmberBackgroundDark,
    onBackground = AmberOnBackgroundDark,
    surface = AmberSurfaceDark,
    onSurface = AmberOnSurfaceDark,
    surfaceVariant = AmberSurfaceVariantDark,
    onSurfaceVariant = AmberOnSurfaceVariantDark,
    outline = AmberOutlineDark
)

@Composable
fun CarromScoreboardTheme(
    preset: MinimalThemePreset = MinimalThemePreset.SLATE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        preset == MinimalThemePreset.SLATE -> if (darkTheme) SlateDarkColorScheme else SlateLightColorScheme
        preset == MinimalThemePreset.IVORY -> if (darkTheme) IvoryDarkColorScheme else IvoryLightColorScheme
        preset == MinimalThemePreset.OBSIDIAN -> if (darkTheme) ObsidianDarkColorScheme else ObsidianLightColorScheme
        preset == MinimalThemePreset.SAGE -> if (darkTheme) SageDarkColorScheme else SageLightColorScheme
        preset == MinimalThemePreset.CLASSIC -> if (darkTheme) AmberDarkColorScheme else AmberLightColorScheme
        else -> if (darkTheme) SlateDarkColorScheme else SlateLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
