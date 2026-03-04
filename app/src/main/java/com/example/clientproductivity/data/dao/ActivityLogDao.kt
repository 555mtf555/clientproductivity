package com.example.clientproductivity.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.clientproductivity.data.entity.ActivityLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs WHERE taskId = :taskId ORDER BY timestamp DESC")
    fun getLogsForTask(taskId: Long): Flow<List<ActivityLogEntity>>

    @Insert
    suspend fun insert(log: ActivityLogEntity)

    @Query("DELETE FROM activity_logs")
    suspend fun deleteAll()
}
