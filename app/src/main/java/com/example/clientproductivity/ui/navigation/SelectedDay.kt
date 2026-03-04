package com.example.clientproductivity.ui.navigation

import java.time.LocalDate

/**
 * Represents which day is currently selected in the dashboard.
 * Now using LocalDate as the source of truth for dynamic dates.
 */
data class SelectedDay(val date: LocalDate)
