package com.example.clientproductivity.data.extensions

import com.example.clientproductivity.data.entity.TaskEntity
import com.example.clientproductivity.data.entity.TaskWithContext
import java.time.Instant

fun TaskWithContext.toTaskEntity(
    title: String = this.title,
    notes: String = this.notes,
    completed: Boolean = this.completed,
    completedAt: Instant? = this.completedAt,
    blocked: Boolean = this.blocked,
    priority: Int = this.priority,
    isRemoved: Boolean = this.isRemoved,
    removedAt: Instant? = this.removedAt
): TaskEntity {
    return TaskEntity(
        id = this.taskId,
        projectId = this.projectId,
        title = title,
        notes = notes,
        dueDate = this.dueDate,
        completed = completed,
        completedAt = completedAt,
        blocked = blocked,
        priority = priority,
        isRemoved = isRemoved,
        removedAt = removedAt
    )
}
