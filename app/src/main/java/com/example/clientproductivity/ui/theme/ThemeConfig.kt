package com.example.clientproductivity.ui.theme

import androidx.compose.ui.graphics.Color

val AppThemes = listOf(
    CustomAppTheme(
        id = 0,
        name = "Oceanic Steel",
        backgroundColor = Color(0xFF1E2A38),
        primaryColor = Color(0xFF4EC5B1),
        cardColor = Color(0xFFF1F3F4), // Neutral Light Grey for readability
        isDark = true
    ),
    CustomAppTheme(
        id = 1,
        name = "Electric Charcoal",
        backgroundColor = Color(0xFF2D2D2D),
        primaryColor = Color(0xFF2979FF),
        cardColor = Color(0xFFFFFFFF), // High contrast white
        isDark = true
    ),
    CustomAppTheme(
        id = 2,
        name = "Gilded Emerald",
        backgroundColor = Color(0xFF124734),
        primaryColor = Color(0xFFD4AF37),
        cardColor = Color(0xFFF5F5F5), // Soft Off-white
        isDark = true
    ),
    CustomAppTheme(
        id = 3,
        name = "Slate Coral",
        backgroundColor = Color(0xFF4A5D73),
        primaryColor = Color(0xFFFF7A70),
        cardColor = Color(0xFFE8EAF6), // Very Light Blue-Grey
        isDark = true
    ),
    CustomAppTheme(
        id = 4,
        name = "Midnight Cyber",
        backgroundColor = Color(0xFF121212),
        primaryColor = Color(0xFF00E5FF),
        cardColor = Color(0xFF2C2C2C), // Neutral Dark Grey
        isDark = true
    ),
    CustomAppTheme(
        id = 5,
        name = "Royal Amber",
        backgroundColor = Color(0xFF3F51B5),
        primaryColor = Color(0xFFFFC107),
        cardColor = Color(0xFFFAFAFA), // Clean White
        isDark = true
    )
)
