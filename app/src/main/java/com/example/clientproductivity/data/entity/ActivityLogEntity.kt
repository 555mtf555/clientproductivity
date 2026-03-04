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
    tableName = "activity_logs",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val type: String,
    val description: String,
    @Serializable(with = InstantSerializer::class)
    val timestamp: Instant = Instant.now()
)

object ActivityTypes {
    const val STATUS_UPDATED = "STATUS_UPDATED"
    const val DUE_DATE_CHANGED = "DUE_DATE_CHANGED"
    const val CLIENT_NOTIFIED = "CLIENT_NOTIFIED"
    const val COMMENT_ADDED = "COMMENT_ADDED"
}
