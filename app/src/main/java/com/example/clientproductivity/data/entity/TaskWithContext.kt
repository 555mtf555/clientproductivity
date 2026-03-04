package com.example.clientproductivity.data.entity

import java.time.Instant

data class TaskWithContext(
    val taskId: Long,
    val projectId: Long,
    val title: String,
    val notes: String,
    val dueDate: Instant,
    val completed: Boolean,
    val completedAt: Instant? = null,
    val blocked: Boolean,
    val priority: Int = 1, // 0: Low, 1: Medium, 2: High
    val isRemoved: Boolean = false,
    val removedAt: Instant? = null,
    val clientName: String,
    val projectName: String,
    val clientEmail: String,
    val clientPhone: String
)
