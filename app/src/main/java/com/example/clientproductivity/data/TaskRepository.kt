package com.example.clientproductivity.data

import androidx.room.withTransaction
import com.example.clientproductivity.data.dao.ActivityLogDao
import com.example.clientproductivity.data.dao.ClientDao
import com.example.clientproductivity.data.dao.ProjectDao
import com.example.clientproductivity.data.dao.TaskDao
import com.example.clientproductivity.data.entity.ActivityLogEntity
import com.example.clientproductivity.data.entity.ClientEntity
import com.example.clientproductivity.data.entity.ProjectEntity
import com.example.clientproductivity.data.entity.TaskEntity
import com.example.clientproductivity.data.entity.TaskWithContext
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val database: AppDatabase,
    private val clientDao: ClientDao,
    private val projectDao: ProjectDao,
    private val taskDao: TaskDao,
    private val activityLogDao: ActivityLogDao
) {

    fun getTasksWithContext(): Flow<List<TaskWithContext>> =
        taskDao.getAllTasksWithContext()

    fun getLogsForTask(taskId: Long): Flow<List<ActivityLogEntity>> =
        activityLogDao.getLogsForTask(taskId)

    fun getTaskWithContextById(taskId: Long): Flow<TaskWithContext?> =
        taskDao.getTaskWithContextById(taskId)

    suspend fun addTask(task: TaskEntity) {
        val taskId = taskDao.insert(task)
        activityLogDao.insert(
            ActivityLogEntity(
                taskId = taskId,
                type = "TASK_CREATED",
                description = "Task created: ${task.title}"
            )
        )
    }

    suspend fun updateTask(task: TaskEntity, logType: String? = null, logDescription: String? = null) {
        taskDao.update(task)
        if (logType != null && logDescription != null) {
            activityLogDao.insert(
                ActivityLogEntity(
                    taskId = task.id,
                    type = logType,
                    description = logDescription
                )
            )
        }
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.delete(task)
    }

    suspend fun deleteMultipleTasks(tasks: List<TaskEntity>) {
        taskDao.deleteMultiple(tasks)
    }

    suspend fun addTaskWithNewClient(
        taskTitle: String,
        taskNotes: String,
        taskDueDate: Instant,
        clientName: String,
        clientEmail: String,
        clientPhone: String,
        projectName: String
    ) {
        repositoryTransaction(
            taskTitle,
            taskNotes,
            taskDueDate,
            clientName,
            clientEmail,
            clientPhone,
            projectName
        )
    }

    private suspend fun repositoryTransaction(
        taskTitle: String,
        taskNotes: String,
        taskDueDate: Instant,
        clientName: String,
        clientEmail: String,
        clientPhone: String,
        projectName: String
    ) = database.withTransaction {
        val clientId = clientDao.insert(
            ClientEntity(
                name = clientName,
                email = clientEmail,
                phone = clientPhone
            )
        )
        val projectId = projectDao.insert(
            ProjectEntity(
                clientId = clientId,
                name = projectName,
                description = "Auto-generated project for $clientName"
            )
        )
        val taskId = taskDao.insert(
            TaskEntity(
                projectId = projectId,
                title = taskTitle,
                notes = taskNotes,
                dueDate = taskDueDate
            )
        )
        
        activityLogDao.insert(
            ActivityLogEntity(
                taskId = taskId,
                type = "TASK_CREATED",
                description = "Task created with new client: $clientName"
            )
        )
    }
}

