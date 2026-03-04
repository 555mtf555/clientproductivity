package com.example.clientproductivity.ui.theme

import androidx.compose.ui.graphics.Color

data class CustomAppTheme(
    val id: Int,
    val name: String,
    val backgroundColor: Color,
    val primaryColor: Color,
    val cardColor: Color,
    val isDark: Boolean = true
)
