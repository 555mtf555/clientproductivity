package com.example.clientproductivity.data.dao

import androidx.room.*
import com.example.clientproductivity.data.entity.TaskEntity
import com.example.clientproductivity.data.entity.TaskWithContext
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("""
        SELECT
            t.id as taskId,
            t.projectId as projectId,
            t.title as title,
            t.notes as notes,
            t.dueDate as dueDate,
            t.completed as completed,
            t.completedAt as completedAt,
            t.blocked as blocked,
            t.priority as priority,
            t.isRemoved as isRemoved,
            t.removedAt as removedAt,
            c.name as clientName,
            p.name as projectName,
            c.email as clientEmail,
            c.phone as clientPhone
        FROM tasks t
        JOIN projects p ON t.projectId = p.id
        JOIN clients c ON p.clientId = c.id
    """)
    fun getAllTasksWithContext(): Flow<List<TaskWithContext>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksList(): List<TaskEntity>

    @Query("""
        SELECT
            t.id as taskId,
            t.projectId as projectId,
            t.title as title,
            t.notes as notes,
            t.dueDate as dueDate,
            t.completed as completed,
            t.completedAt as completedAt,
            t.blocked as blocked,
            t.priority as priority,
            t.isRemoved as isRemoved,
            t.removedAt as removedAt,
            c.name as clientName,
            p.name as projectName,
            c.email as clientEmail,
            c.phone as clientPhone
        FROM tasks t
        JOIN projects p ON t.projectId = p.id
        JOIN clients c ON p.clientId = c.id
        WHERE t.id = :taskId
    """)
    fun getTaskWithContextById(taskId: Long): Flow<TaskWithContext?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Delete
    suspend fun deleteMultiple(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}
