package com.caper.pepper.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import pepper.composeapp.generated.resources.Res
import pepper.composeapp.generated.resources.misans_bold
import pepper.composeapp.generated.resources.misans_light
import pepper.composeapp.generated.resources.misans_regular
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font

@Composable
@OptIn(ExperimentalResourceApi::class)
fun PepperTheme(content: @Composable () -> Unit) {
    val miSansFamily = FontFamily(
        Font(Res.font.misans_light, weight = FontWeight.Light),
        Font(Res.font.misans_regular, weight = FontWeight.Normal),
        Font(Res.font.misans_bold, weight = FontWeight.Bold),
    )
    val base = Typography()
    val typography = Typography(
        displayLarge = base.displayLarge.copy(fontFamily = miSansFamily),
        displayMedium = base.displayMedium.copy(fontFamily = miSansFamily),
        displaySmall = base.displaySmall.copy(fontFamily = miSansFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = miSansFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = miSansFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = miSansFamily),
        titleLarge = base.titleLarge.copy(fontFamily = miSansFamily),
        titleMedium = base.titleMedium.copy(fontFamily = miSansFamily),
        titleSmall = base.titleSmall.copy(fontFamily = miSansFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = miSansFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = miSansFamily),
        bodySmall = base.bodySmall.copy(fontFamily = miSansFamily),
        labelLarge = base.labelLarge.copy(fontFamily = miSansFamily),
        labelMedium = base.labelMedium.copy(fontFamily = miSansFamily),
        labelSmall = base.labelSmall.copy(fontFamily = miSansFamily),
    )
    MaterialTheme(
        typography = typography,
        content = content
    )
}
