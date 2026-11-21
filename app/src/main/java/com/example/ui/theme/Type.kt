package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val DisplayAndHindiFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.noto_sans_devanagari, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.noto_sans_devanagari, FontWeight.Bold)
)

val TelemetryMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

fun buildAppTypography(largeTextScale: Float = 1.0f): Typography {
    fun scaled(spValue: Float) = (spValue * largeTextScale).sp
    return Typography(
        displaySmall = TextStyle(
            fontFamily = DisplayAndHindiFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = scaled(34f),
            lineHeight = scaled(42f),
            letterSpacing = (-0.2).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = DisplayAndHindiFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = scaled(26f),
            lineHeight = scaled(34f)
        ),
        titleLarge = TextStyle(
            fontFamily = DisplayAndHindiFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = scaled(21f),
            lineHeight = scaled(28f)
        ),
        titleMedium = TextStyle(
            fontFamily = DisplayAndHindiFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaled(18f),
            lineHeight = scaled(25f)
        ),
        bodyLarge = TextStyle(
            fontFamily = DisplayAndHindiFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = scaled(17f),
            lineHeight = scaled(25f),
            letterSpacing = 0.2.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = DisplayAndHindiFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = scaled(15f),
            lineHeight = scaled(22f),
            letterSpacing = 0.2.sp
        ),
        labelLarge = TextStyle(
            fontFamily = DisplayAndHindiFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = scaled(15f),
            lineHeight = scaled(20f)
        ),
        labelMedium = TextStyle(
            fontFamily = TelemetryMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = scaled(13f),
            lineHeight = scaled(18f)
        )
    )
}
