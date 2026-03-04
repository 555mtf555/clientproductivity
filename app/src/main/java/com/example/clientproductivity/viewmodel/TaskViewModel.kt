package com.example.clientproductivity.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clientproductivity.data.TaskRepository
import com.example.clientproductivity.data.entity.ActivityLogEntity
import com.example.clientproductivity.data.entity.TaskEntity
import com.example.clientproductivity.ui.navigation.SelectedDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(private val repository: TaskRepository) : ViewModel() {

    // FIX: selectedDay lives in the ViewModel so it survives recomposition of AppScaffold,
    // preventing the dashboard scroll position from resetting when a day card is tapped.
    var selectedDay by mutableStateOf(SelectedDay(LocalDate.now()))
        internal set

    fun setSelectedDay(day: SelectedDay) {
        selectedDay = day
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Base stream: all tasks with client + project info
    private val allTasksFlow = repository.getTasksWithContext()

    // --- All Active Tasks (sorted by date, with search filter) ---
    val allTasks: StateFlow<List<TaskWithContext>> = combine(allTasksFlow, _searchQuery) { tasks, query ->
        tasks.filter { task ->
            !task.isRemoved && (
                    task.title.contains(query, ignoreCase = true) ||
                            task.clientName.contains(query, ignoreCase = true) ||
                            task.projectName.contains(query, ignoreCase = true)
                    )
        }.sortedBy { it.dueDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Dashboard Metrics ---
    val dashboardMetrics: StateFlow<DashboardMetrics> = allTasksFlow.map { tasks ->
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val today = now.atZone(zone).toLocalDate()

        val sunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val saturday = sunday.plusDays(6)

        val startOfWeek = sunday.atStartOfDay(zone).toInstant()
        val endOfWeek = saturday.atTime(23, 59, 59, 999_999_999).atZone(zone).toInstant()

        val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")
        val weekLabel = "${sunday.format(dateFormatter)} - ${saturday.format(dateFormatter)}"

        val activeTasks = tasks.filter { !it.isRemoved }

        val startOfToday = today.atStartOfDay(zone).toInstant()
        val endOfToday = today.plusDays(1).atStartOfDay(zone).toInstant().minusNanos(1)

        // Overdue logic: Current time passed due date OR manually marked overdue
        val overdue = activeTasks.filter {
            (!it.completed && it.dueDate.isBefore(now)) || (!it.completed && it.blocked)
        }

        // Due Today logic: Due within the next 24 hours from now
        val twentyFourHoursFromNow = now.plus(24, ChronoUnit.HOURS)
        val dueToday = activeTasks.filter {
            !it.completed && it.dueDate.isAfter(now) && it.dueDate.isBefore(twentyFourHoursFromNow)
        }

        val thisWeekTasks = tasks.filter { task ->
            if (task.isRemoved) return@filter false
            val isDueThisWeek = !task.dueDate.isBefore(startOfWeek) && !task.dueDate.isAfter(endOfWeek)
            val isCompletedThisWeek = task.completed && task.completedAt != null &&
                    !task.completedAt.isBefore(startOfWeek) && !task.completedAt.isAfter(endOfWeek)
            isDueThisWeek || isCompletedThisWeek
        }

        val completedThisWeek = thisWeekTasks.count { it.completed }
        val totalThisWeek = thisWeekTasks.size

        // Due Soon logic: Due between 24 hours and 7 days from now
        val sevenDaysFromNow = now.plus(7, ChronoUnit.DAYS)
        val dueSoonCount = activeTasks.count {
            !it.completed && it.dueDate.isAfter(twentyFourHoursFromNow) && it.dueDate.isBefore(sevenDaysFromNow)
        }

        DashboardMetrics(
            overdueCount = overdue.size,
            dueTodayCount = dueToday.size,
            completionRate = if (totalThisWeek > 0) completedThisWeek.toFloat() / totalThisWeek else 0f,
            dueSoonCount = dueSoonCount,
            totalTasksThisWeek = totalThisWeek,
            completedTasksThisWeek = completedThisWeek,
            weekLabel = weekLabel
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardMetrics())

    // --- Tasks for today / overdue (Active only) ---
    val todayTasks: StateFlow<List<TaskWithContext>> = allTasksFlow
        .map { tasks ->
            val endOfToday = Instant.now()
                .atZone(ZoneId.systemDefault())
                .withHour(23)
                .withMinute(59)
                .withSecond(59)
                .withNano(999_999_999)
                .toInstant()

            tasks.filter { !it.isRemoved && it.dueDate.isBefore(endOfToday.plusNanos(1)) }
                .sortedBy { it.dueDate }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Upcoming tasks (next 7 days, Active only) ---
    val upcomingTasks: StateFlow<List<TaskWithContext>> = allTasksFlow
        .map { tasks ->
            val now = Instant.now().atZone(ZoneId.systemDefault())
            val endOfToday = now
                .withHour(23)
                .withMinute(59)
                .withSecond(59)
                .withNano(999_999_999)
                .toInstant()

            val endOfWeek = now
                .plusDays(7)
                .withHour(23)
                .withMinute(59)
                .withSecond(59)
                .withNano(999_999_999)
                .toInstant()

            tasks.filter { !it.isRemoved && it.dueDate.isAfter(endOfToday) && it.dueDate.isBefore(endOfWeek.plusNanos(1)) }
                .sortedBy { it.dueDate }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Recently Removed Tasks (last 7 days) ---
    val recentlyRemovedTasks: StateFlow<List<TaskWithContext>> = allTasksFlow
        .map { tasks ->
            val oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS)
            tasks.filter { it.isRemoved && it.removedAt != null && it.removedAt.isAfter(oneWeekAgo) }
                .sortedByDescending { it.removedAt }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getLogsForTask(taskId: Long): Flow<List<ActivityLogEntity>> = repository.getLogsForTask(taskId)

    fun getTaskById(taskId: Long): Flow<TaskWithContext?> = repository.getTaskWithContextById(taskId)

    fun addTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.addTask(task)
        }
    }

    fun updateTask(task: TaskEntity, logType: String? = null, logDescription: String? = null) {
        viewModelScope.launch {
            repository.updateTask(task, logType, logDescription)
        }
    }

    fun toggleComplete(task: TaskEntity) {
        val newCompletedStatus = !task.completed
        val logDescription = if (newCompletedStatus) "Task marked as completed" else "Task marked as incomplete"
        updateTask(
            task.copy(
                completed = newCompletedStatus,
                completedAt = if (newCompletedStatus) Instant.now() else null
            ),
            logType = "STATUS_UPDATED",
            logDescription = logDescription
        )
    }

    fun toggleOverdue(task: TaskEntity) {
        val newOverdueStatus = !task.blocked
        val logDescription = if (newOverdueStatus) "Task manually marked as overdue" else "Manual overdue mark cleared"
        updateTask(
            task.copy(blocked = newOverdueStatus),
            logType = "OVERDUE_TOGGLED",
            logDescription = logDescription
        )
    }

    fun updatePriority(task: TaskEntity, newPriority: Int) {
        val priorityLabels = listOf("Low", "Medium", "High")
        val logDescription = "Priority changed from ${priorityLabels.getOrElse(task.priority) { "Unknown" }} to ${priorityLabels.getOrElse(newPriority) { "Unknown" }}"
        updateTask(
            task.copy(priority = newPriority),
            logType = "PRIORITY_CHANGED",
            logDescription = logDescription
        )
    }

    fun removeTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(
                task.copy(isRemoved = true, removedAt = Instant.now()),
                logType = "TASK_REMOVED",
                logDescription = "Task moved to Recently Removed"
            )
        }
    }

    fun restoreTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(
                task.copy(isRemoved = false, removedAt = null),
                logType = "TASK_RESTORED",
                logDescription = "Task restored from Recently Removed"
            )
        }
    }

    fun deletePermanently(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun deleteMultiplePermanently(tasks: List<TaskEntity>) {
        viewModelScope.launch {
            repository.deleteMultipleTasks(tasks)
        }
    }

    fun restoreMultiple(tasks: List<TaskEntity>) {
        viewModelScope.launch {
            tasks.forEach { task ->
                repository.updateTask(
                    task.copy(isRemoved = false, removedAt = null),
                    logType = "TASK_RESTORED",
                    logDescription = "Task restored from Recently Removed (Bulk)"
                )
            }
        }
    }

    fun addTaskWithNewClient(
        taskTitle: String,
        taskNotes: String,
        taskDueDate: Instant,
        clientName: String,
        clientEmail: String,
        clientPhone: String,
        projectName: String
    ) {
        viewModelScope.launch {
            repository.addTaskWithNewClient(
                taskTitle,
                taskNotes,
                taskDueDate,
                clientName,
                clientEmail,
                clientPhone,
                projectName
            )
        }
    }
}