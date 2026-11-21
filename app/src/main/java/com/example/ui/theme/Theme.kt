package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private fun buildAndhaHelperColorScheme(highContrast: Boolean) = darkColorScheme(
    primary = SonarCyan,
    onPrimary = ObsidianBg,
    primaryContainer = SonarCyanContainer,
    onPrimaryContainer = TextPrimary,
    secondary = SafeEmerald,
    onSecondary = ObsidianBg,
    secondaryContainer = SafeEmeraldContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = CautionAmber,
    onTertiary = ObsidianBg,
    tertiaryContainer = CautionAmberContainer,
    onTertiaryContainer = TextPrimary,
    error = SosCrimson,
    onError = TextPrimary,
    errorContainer = SosCrimsonContainer,
    onErrorContainer = TextPrimary,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHigh = ObsidianCardElevated,
    outline = if (highContrast) ObsidianOutlineHighContrast else ObsidianOutline
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AndhaHelperTheme(
    highContrast: Boolean = true,
    largeTextMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val scale = if (largeTextMode) 1.15f else 1.0f
    MaterialTheme(
        colorScheme = buildAndhaHelperColorScheme(highContrast),
        typography = buildAppTypography(scale),
        shapes = AppShapes,
        content = content
    )
}
