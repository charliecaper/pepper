package com.caper.pepper.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import pepper.composeapp.generated.resources.Res
import pepper.composeapp.generated.resources.fk_grotesk_variable
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font

/**
 * Even OS 2.0 Design System Colors
 */
object EvenColors {
    // Text colors
    val TextPrimary = Color(0xFF232323)
    val TextSecondary = Color(0xFF7B7B7B)
    val TextHighlight = Color(0xFFFFFFFF)
    val TextWarning = Color(0xFFFF453A)
    val TextSuccess = Color(0xFF4BB956)

    // Background colors
    val BackgroundMain = Color(0xFFEEEEEE)
    val BackgroundSecondary = Color(0xFFF6F6F6)
    val BackgroundElevated = Color(0xFFE4E4E4)
    val ButtonPrimary = Color(0xFFFFFFFF)
    val HighlightAction = Color(0xFF232323)
    val AccentOngoing = Color(0xFFFEF991)
}

/**
 * Even OS 2.0 Design System
 * Typography scale per design guidelines:
 * - Very Large Title: FK 24 Regular
 * - Large Title: FK 20 Regular
 * - Medium Title: FK 17 Regular
 * - Medium Body: FK 17 Light
 * - Normal Title: FK 15 Regular
 * - Normal Body: FK 15 Light
 * - Subtitle: FK 13 Regular
 * - Detail/Caption: FK 11 Regular
 */
@Composable
@OptIn(ExperimentalResourceApi::class)
fun PepperTheme(content: @Composable () -> Unit) {
    // FK Grotesk variable font - Light (300) and Regular (400) weights
    val fontFamily = FontFamily(
        Font(Res.font.fk_grotesk_variable, weight = FontWeight.Light),
        Font(Res.font.fk_grotesk_variable, weight = FontWeight.Normal),
    )

    val typography = Typography(
        // Very Large Title: 24 Regular
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            color = EvenColors.TextPrimary,
        ),
        // Large Title: 20 Regular
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            color = EvenColors.TextPrimary,
        ),
        // Medium Title: 17 Regular
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 17.sp,
            color = EvenColors.TextPrimary,
        ),
        // Medium Body: 17 Light
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 17.sp,
            color = EvenColors.TextPrimary,
        ),
        // Normal Title: 15 Regular
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            color = EvenColors.TextPrimary,
        ),
        // Normal Body: 15 Light
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 15.sp,
            color = EvenColors.TextPrimary,
        ),
        // Subtitle: 13 Regular
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = EvenColors.TextSecondary,
        ),
        // Detail/Caption: 11 Regular
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            color = EvenColors.TextSecondary,
        ),
        // Additional styles mapped to design system
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = EvenColors.TextPrimary,
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            color = EvenColors.TextPrimary,
        ),
    )

    val colorScheme = lightColorScheme(
        primary = EvenColors.HighlightAction,
        onPrimary = EvenColors.TextHighlight,
        secondary = EvenColors.BackgroundElevated,
        onSecondary = EvenColors.TextPrimary,
        background = EvenColors.BackgroundMain,
        onBackground = EvenColors.TextPrimary,
        surface = EvenColors.BackgroundSecondary,
        onSurface = EvenColors.TextPrimary,
        error = EvenColors.TextWarning,
        onError = EvenColors.TextHighlight,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
