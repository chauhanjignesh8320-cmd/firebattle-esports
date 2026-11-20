package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FirebattleColorScheme = darkColorScheme(
    primary = BlazeOrange,
    onPrimary = TextPrimary,
    primaryContainer = BlazeOrangeContainer,
    onPrimaryContainer = BlazeOrangeBright,
    secondary = EmeraldWin,
    onSecondary = ArenaObsidian,
    secondaryContainer = EmeraldWinContainer,
    onSecondaryContainer = EmeraldWin,
    tertiary = AmberPending,
    onTertiary = ArenaObsidian,
    tertiaryContainer = AmberPendingContainer,
    onTertiaryContainer = AmberPending,
    error = CrimsonReject,
    onError = TextPrimary,
    errorContainer = CrimsonRejectContainer,
    onErrorContainer = CrimsonReject,
    background = ArenaObsidian,
    onBackground = TextPrimary,
    surface = ArenaSlateDark,
    onSurface = TextPrimary,
    surfaceVariant = ArenaSlateSurface,
    onSurfaceVariant = TextSecondary,
    outline = ArenaSlateBorder,
    outlineVariant = TextMuted
)

private val FirebattleShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FirebattleTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FirebattleColorScheme,
        typography = FirebattleTypography,
        shapes = FirebattleShapes,
        content = content
    )
}
