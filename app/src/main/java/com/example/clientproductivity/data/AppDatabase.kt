package com.example.clientproductivity.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.clientproductivity.data.converters.DateConverters
import com.example.clientproductivity.data.dao.ActivityLogDao
import com.example.clientproductivity.data.dao.ClientDao
import com.example.clientproductivity.data.dao.ProjectDao
import com.example.clientproductivity.data.dao.TaskDao
import com.example.clientproductivity.data.entity.ActivityLogEntity
import com.example.clientproductivity.data.entity.ClientEntity
import com.example.clientproductivity.data.entity.ProjectEntity
import com.example.clientproductivity.data.entity.TaskEntity

@Database(
    entities = [
        ClientEntity::class,
        ProjectEntity::class,
        TaskEntity::class,
        ActivityLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun activityLogDao(): ActivityLogDao
}
