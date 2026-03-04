package com.example.clientproductivity.viewmodel

data class DashboardMetrics(
    val overdueCount: Int = 0,
    val dueTodayCount: Int = 0,
    val completionRate: Float = 0f,
    val dueSoonCount: Int = 0,
    val totalTasksThisWeek: Int = 0,
    val completedTasksThisWeek: Int = 0,
    val weekLabel: String = ""
)
