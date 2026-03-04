package com.example.clientproductivity.data.backup

import androidx.room.withTransaction
import com.example.clientproductivity.data.AppDatabase
import com.example.clientproductivity.data.dao.ClientDao
import com.example.clientproductivity.data.dao.ProjectDao
import com.example.clientproductivity.data.dao.TaskDao
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val database: AppDatabase,
    private val clientDao: ClientDao,
    private val projectDao: ProjectDao,
    private val taskDao: TaskDao
) {
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun exportData(): String {
        val backup = BackupModel(
            clients = clientDao.getAllClientsList(),
            projects = projectDao.getAllProjectsList(),
            tasks = taskDao.getAllTasksList()
        )
        return json.encodeToString(backup)
    }

    suspend fun importData(jsonData: String) {
        val backup = json.decodeFromString<BackupModel>(jsonData)
        database.withTransaction {
            taskDao.deleteAll()
            projectDao.deleteAll()
            clientDao.deleteAll()
            clientDao.insertAll(backup.clients)
            projectDao.insertAll(backup.projects)
            taskDao.insertAll(backup.tasks)
        }
    }
}
