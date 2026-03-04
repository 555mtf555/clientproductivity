package com.example.clientproductivity.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

@Composable
fun ClientproductivityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    backgroundColor: Color = Color(0xFF0B1E3D),
    primaryColor: Color = Color(0xFF2196F3),
    cardColor: Color = Color(0xFFFFFFFF),
    content: @Composable () -> Unit
) {
    val isDarkBackground = ColorUtils.calculateLuminance(backgroundColor.toArgb()) < 0.5
    val isLightCard = ColorUtils.calculateLuminance(cardColor.toArgb()) > 0.5

    // Slightly contrasted surface so OutlinedTextFields stand out from the background
    val surfaceColor = if (isDarkBackground) {
        Color(ColorUtils.blendARGB(backgroundColor.toArgb(), Color.White.toArgb(), 0.08f))
    } else {
        Color(ColorUtils.blendARGB(backgroundColor.toArgb(), Color.Black.toArgb(), 0.05f))
    }

    // Surface variant used for text field containers — more contrast than surface
    val surfaceVariantColor = if (isDarkBackground) {
        Color(ColorUtils.blendARGB(backgroundColor.toArgb(), Color.White.toArgb(), 0.15f))
    } else {
        Color(ColorUtils.blendARGB(backgroundColor.toArgb(), Color.Black.toArgb(), 0.08f))
    }

    // OutlinedTextField container background — needs the most contrast to be legible
    val textFieldContainerColor = if (isDarkBackground) {
        Color(ColorUtils.blendARGB(backgroundColor.toArgb(), Color.White.toArgb(), 0.12f))
    } else {
        Color(ColorUtils.blendARGB(backgroundColor.toArgb(), Color.Black.toArgb(), 0.06f))
    }

    // Outline color — bright enough to always be visible against the background
    val outlineColor = if (isDarkBackground) {
        Color.White.copy(alpha = 0.6f)
    } else {
        Color.Black.copy(alpha = 0.5f)
    }

    val colorScheme = if (isDarkBackground) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = if (ColorUtils.calculateLuminance(primaryColor.toArgb()) > 0.5) Color.Black else Color.White,
            background = backgroundColor,
            onBackground = Color.White,
            surface = surfaceColor,
            onSurface = Color.White,
            surfaceVariant = surfaceVariantColor,
            onSurfaceVariant = Color.White,
            surfaceContainerHighest = textFieldContainerColor,
            secondaryContainer = primaryColor.copy(alpha = 0.2f),
            onSecondaryContainer = Color.White,
            outline = outlineColor
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = if (ColorUtils.calculateLuminance(primaryColor.toArgb()) > 0.5) Color.Black else Color.White,
            background = backgroundColor,
            onBackground = Color.Black,
            surface = surfaceColor,
            onSurface = Color.Black,
            surfaceVariant = surfaceVariantColor,
            onSurfaceVariant = Color.Black,
            surfaceContainerHighest = textFieldContainerColor,
            secondaryContainer = primaryColor.copy(alpha = 0.2f),
            onSecondaryContainer = Color.Black,
            outline = outlineColor
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}