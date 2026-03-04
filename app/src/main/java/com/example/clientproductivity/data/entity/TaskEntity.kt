package com.example.clientproductivity.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.clientproductivity.data.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val title: String,
    val notes: String,
    @Serializable(with = InstantSerializer::class)
    val dueDate: Instant,
    val completed: Boolean = false,
    @Serializable(with = InstantSerializer::class)
    val completedAt: Instant? = null,
    val blocked: Boolean = false,
    val priority: Int = 1, // 0: Low, 1: Medium, 2: High
    val isRemoved: Boolean = false,
    @Serializable(with = InstantSerializer::class)
    val removedAt: Instant? = null
)
