package com.example.clientproductivity.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.clientproductivity.data.dao.TaskDao
import com.example.clientproductivity.data.prefs.PreferenceManager
import com.example.clientproductivity.ui.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.temporal.ChronoUnit

@HiltWorker
class TaskReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskDao: TaskDao,
    private val preferenceManager: PreferenceManager,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val remindersEnabled = preferenceManager.taskRemindersEnabled.first()
        if (!remindersEnabled) return Result.success()

        val now = Instant.now()

        // Only notify for tasks entering the 24hr window since last check (1hr ago).
        // This prevents the same task being notified on every hourly run.
        // A task qualifies if it is due between 23h and 24h from now.
        val windowStart = now.plus(23, ChronoUnit.HOURS)
        val windowEnd = now.plus(24, ChronoUnit.HOURS)

        val tasks = taskDao.getAllTasksWithContext().first()

        tasks.filter { task ->
            !task.completed &&
                    !task.isRemoved &&
                    task.dueDate.isAfter(windowStart) &&
                    task.dueDate.isBefore(windowEnd)
        }.forEach { task ->
            notificationHelper.showTaskReminder(
                taskId = task.taskId,
                taskTitle = task.title,
                clientName = task.clientName
            )
        }

        return Result.success()
    }
}