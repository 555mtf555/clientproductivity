@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.clientproductivity.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import com.example.clientproductivity.R
import com.example.clientproductivity.data.entity.TaskEntity
import com.example.clientproductivity.data.entity.TaskWithContext
import com.example.clientproductivity.data.extensions.toTaskEntity
import com.example.clientproductivity.viewmodel.DashboardMetrics
import com.example.clientproductivity.viewmodel.TaskViewModel
import com.example.clientproductivity.ui.navigation.SelectedDay
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/* ---------------- MODELS ---------------- */

data class TaskActions(
    val onUpdate: (TaskEntity) -> Unit,
    val onToggleComplete: (TaskEntity) -> Unit,
    val onToggleOverdue: (TaskEntity) -> Unit,
    val onRemove: (TaskEntity) -> Unit,
    val updatePriority: (TaskEntity, Int) -> Unit
)

sealed class TaskAction {
    object None : TaskAction()
    object Rename : TaskAction()
    object Delete : TaskAction()
    object Notify : TaskAction()
}

/* ---------------- COLORS (Standard) ---------------- */

private val OverdueRed = Color(0xFFD32F2F)
private val CompletedGreen = Color(0xFF4CAF50)
private val PriorityGold = Color(0xFFFFD700)

/* ---------------- HELPERS ---------------- */

private fun getTaskStatusColor(task: TaskWithContext, primaryAccent: Color): Color {
    return when {
        task.completed -> CompletedGreen
        task.dueDate.toEpochMilli() < System.currentTimeMillis() || task.blocked -> OverdueRed
        else -> primaryAccent
    }
}

/* ---------------- SCREEN ---------------- */

@Composable
fun DashboardScreen(
    taskViewModel: TaskViewModel,
    completionBoxColor: Color,
    dayCardColor: Color,
    scrollState: ScrollState,
    themeName: String = ""
) {
    val todayTasks by taskViewModel.todayTasks.collectAsState()
    val upcomingTasks by taskViewModel.upcomingTasks.collectAsState()
    val metrics by taskViewModel.dashboardMetrics.collectAsState()
    val expandedDay = taskViewModel.selectedDay

    DashboardContent(
        todayTasks = todayTasks,
        upcomingTasks = upcomingTasks,
        metrics = metrics,
        expandedDay = expandedDay,
        onDaySelected = { taskViewModel.selectedDay = it },
        actions = TaskActions(
            onUpdate = { taskViewModel.updateTask(it) },
            onToggleComplete = { taskViewModel.toggleComplete(it) },
            onToggleOverdue = { taskViewModel.toggleOverdue(it) },
            onRemove = { taskViewModel.removeTask(it) },
            updatePriority = { task, priority -> taskViewModel.updatePriority(task, priority) }
        ),
        completionBoxColor = completionBoxColor,
        dayCardColor = dayCardColor,
        scrollState = scrollState,
        themeName = themeName
    )
}

@Composable
fun DashboardContent(
    todayTasks: List<TaskWithContext>,
    upcomingTasks: List<TaskWithContext>,
    metrics: DashboardMetrics,
    expandedDay: SelectedDay,
    onDaySelected: (SelectedDay) -> Unit,
    actions: TaskActions,
    completionBoxColor: Color,
    dayCardColor: Color,
    scrollState: ScrollState,
    themeName: String = ""
) {
    val startOfToday = remember {
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    val today = LocalDate.now()
    var metricsSectionBottom by remember { mutableIntStateOf(0) }
    val previousExpandedDay = remember { mutableStateOf<SelectedDay?>(null) }

    LaunchedEffect(expandedDay) {
        val isNewUpcomingSelection =
            expandedDay.date.isAfter(today.plusDays(1)) &&
                    expandedDay != previousExpandedDay.value
        if (isNewUpcomingSelection && metricsSectionBottom > 0) {
            scrollState.animateScrollTo(
                value = metricsSectionBottom,
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            )
        }
        previousExpandedDay.value = expandedDay
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier.onGloballyPositioned { coords ->
                // scroll to halfway through the metrics card
                metricsSectionBottom = (coords.positionInParent().y + coords.size.height / 2).toInt()
            }
        ) {
            MetricsSection(metrics, completionBoxColor, themeName = themeName)
        }
        Spacer(modifier = Modifier.height(24.dp))
        AccordionRow(
            todayAndOverdue = todayTasks,
            upcomingTasks = upcomingTasks,
            expandedDay = expandedDay,
            onDaySelected = onDaySelected,
            actions = actions,
            startOfToday = startOfToday,
            dayCardColor = dayCardColor,
            primaryAccent = completionBoxColor
        )
        Spacer(modifier = Modifier.height(32.dp))
        Box(modifier = Modifier.heightIn(min = 400.dp)) {
            UpcomingSection(
                tasks = upcomingTasks,
                expandedDay = expandedDay,
                onDaySelected = onDaySelected,
                actions = actions,
                dayCardColor = dayCardColor,
                primaryAccent = completionBoxColor
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/* ---------------- METRICS SECTION ---------------- */

@Composable
fun MetricsSection(
    metrics: DashboardMetrics,
    themePrimary: Color,
    themeName: String = "",
    modifier: Modifier = Modifier
) {
    val isLightBackground = ColorUtils.calculateLuminance(themePrimary.toArgb()) > 0.5
    val contentColor = if (isLightBackground) Color.Black else Color.White

    val dueTodayIconColor = if (themeName == "Electric Charcoal") Color.Black else Color(0xFF1565C0)
    val overdueIconColor = if (themeName == "Slate Coral") Color.White else Color(0xFFD32F2F)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = themePrimary,
        contentColor = contentColor,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Weekly Completion",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = metrics.weekLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${(metrics.completionRate * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isLightBackground) CompletedGreen.copy(alpha = 0.8f) else Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { metrics.completionRate },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = CompletedGreen,
                trackColor = contentColor.copy(alpha = 0.2f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(20.dp))

            CompositionLocalProvider(LocalContentColor provides contentColor) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(
                        label = "Overdue",
                        value = metrics.overdueCount.toString(),
                        icon = Icons.Default.Warning,
                        color = overdueIconColor
                    )
                    MetricItem(
                        label = "Due Today",
                        value = metrics.dueTodayCount.toString(),
                        icon = Icons.Default.Today,
                        color = dueTodayIconColor
                    )
                    MetricItem(
                        label = "Due Soon",
                        value = metrics.dueSoonCount.toString(),
                        icon = Icons.Default.Schedule,
                        color = if (isLightBackground) Color(0xFFF57F17) else Color(0xFFFFE082)
                    )
                }
            }
        }
    }
}

@Composable
fun MetricItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalContentColor.current.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
        )
    }
}

/* ---------------- ACCORDION ROW ---------------- */

@Composable
fun AccordionRow(
    todayAndOverdue: List<TaskWithContext>,
    upcomingTasks: List<TaskWithContext>,
    expandedDay: SelectedDay,
    onDaySelected: (SelectedDay) -> Unit,
    actions: TaskActions,
    startOfToday: Long,
    dayCardColor: Color,
    primaryAccent: Color,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val tomorrow = today.plusDays(1)

    val yesterdayMillis = remember(startOfToday) {
        yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val tomorrowMillis = remember(startOfToday) {
        tomorrow.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    val yesterdayTasks by remember(todayAndOverdue, startOfToday) {
        derivedStateOf {
            todayAndOverdue.filter {
                val dueDateMillis = it.dueDate.toEpochMilli()
                (dueDateMillis < System.currentTimeMillis() && !it.completed) ||
                        (it.completed && it.completedAt != null && it.completedAt.toEpochMilli() >= yesterdayMillis && it.completedAt.toEpochMilli() < startOfToday) ||
                        (it.blocked && !it.completed)
            }.sortedByDescending { it.priority }
        }
    }

    val todayTasksOnly by remember(todayAndOverdue, startOfToday) {
        derivedStateOf {
            todayAndOverdue.filter {
                val dueDateMillis = it.dueDate.toEpochMilli()
                (dueDateMillis in startOfToday..<tomorrowMillis) ||
                        (it.completed && it.completedAt != null && it.completedAt.toEpochMilli() >= startOfToday) ||
                        (dueDateMillis < System.currentTimeMillis() && !it.completed)
            }.sortedByDescending { it.priority }
        }
    }

    val tomorrowTasks by remember(upcomingTasks) {
        derivedStateOf { getTasksForDay(upcomingTasks, tomorrow) }
    }

    val yestWeight by animateFloatAsState(if (expandedDay.date == yesterday) 2.2f else 1f, label = "yestWeight")
    val todayWeight by animateFloatAsState(if (expandedDay.date == today) 2.2f else 1f, label = "todayWeight")
    val tomwWeight by animateFloatAsState(if (expandedDay.date == tomorrow) 2.2f else 1f, label = "tomwWeight")

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        DayCard(
            title = stringResource(R.string.dashboard_yesterday),
            shortTitle = "Yest.",
            dateLabel = getFormattedDate(yesterday),
            tasks = yesterdayTasks,
            modifier = Modifier.weight(yestWeight),
            isExpanded = expandedDay.date == yesterday,
            onClick = { onDaySelected(SelectedDay(yesterday)) },
            actions = actions,
            cardColor = dayCardColor,
            primaryAccent = primaryAccent
        )
        DayCard(
            title = stringResource(R.string.dashboard_today),
            shortTitle = stringResource(R.string.dashboard_today),
            dateLabel = getFormattedDate(today),
            tasks = todayTasksOnly,
            modifier = Modifier.weight(todayWeight),
            isExpanded = expandedDay.date == today,
            onClick = { onDaySelected(SelectedDay(today)) },
            actions = actions,
            cardColor = dayCardColor,
            primaryAccent = primaryAccent
        )
        DayCard(
            title = stringResource(R.string.dashboard_tomorrow),
            shortTitle = "Tom.",
            dateLabel = getFormattedDate(tomorrow),
            tasks = tomorrowTasks,
            modifier = Modifier.weight(tomwWeight),
            isExpanded = expandedDay.date == tomorrow,
            onClick = { onDaySelected(SelectedDay(tomorrow)) },
            actions = actions,
            cardColor = dayCardColor,
            primaryAccent = primaryAccent
        )
    }
}

/* ---------------- DAY CARD ---------------- */

@Composable
fun DayCard(
    title: String,
    shortTitle: String,
    dateLabel: String,
    actions: TaskActions,
    cardColor: Color,
    primaryAccent: Color,
    modifier: Modifier = Modifier,
    tasks: List<TaskWithContext> = emptyList(),
    isExpanded: Boolean = false,
    onClick: () -> Unit = {}
) {
    val animatedMinHeight by animateDpAsState(
        targetValue = if (isExpanded) 280.dp else 120.dp,
        label = "minHeight"
    )

    Card(
        modifier = modifier
            .heightIn(min = animatedMinHeight)
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
            .focusProperties { canFocus = false }
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) cardColor.copy(alpha = 0.8f) else cardColor
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isExpanded) title else shortTitle,
                style = MaterialTheme.typography.titleMedium,
                color = if (ColorUtils.calculateLuminance(cardColor.toArgb()) > 0.5) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = (if (ColorUtils.calculateLuminance(cardColor.toArgb()) > 0.5) Color.Black else Color.White).copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (isExpanded) {
                ExpandedTaskContent(tasks = tasks, actions = actions, cardColor = cardColor)
            } else {
                CollapsedTaskContent(tasks, primaryAccent, cardColor)
            }
        }
    }
}

@Composable
private fun ExpandedTaskContent(tasks: List<TaskWithContext>, actions: TaskActions, cardColor: Color) {
    if (tasks.isEmpty()) {
        EmptyStateView(stringResource(R.string.dashboard_no_tasks), cardColor)
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            tasks.forEach { task ->
                key(task.taskId) {
                    TaskItem(task, actions = actions, cardColor = cardColor)
                }
            }
        }
    }
}

@Composable
private fun CollapsedTaskContent(tasks: List<TaskWithContext>, primaryAccent: Color, cardColor: Color) {
    if (tasks.isEmpty()) {
        EmptyStateView(stringResource(R.string.dashboard_free), cardColor)
    } else {
        val contentColor = if (ColorUtils.calculateLuminance(cardColor.toArgb()) > 0.5) Color.Black else Color.White
        Column(modifier = Modifier.fillMaxWidth()) {
            tasks.take(3).forEach { task ->
                val statusColor = getTaskStatusColor(task, primaryAccent)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    val isOverdue = task.dueDate.toEpochMilli() < System.currentTimeMillis() || task.blocked
                    Text(
                        text = task.title,
                        color = if (task.completed) statusColor else if (isOverdue) OverdueRed else contentColor,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
            if (tasks.size > 3) MoreTasksLabel(tasks.size - 3, cardColor)
        }
    }
}

@Composable
private fun EmptyStateView(text: String, cardColor: Color) {
    val contentColor = if (ColorUtils.calculateLuminance(cardColor.toArgb()) > 0.5) Color.Black else Color.White
    Box(modifier = Modifier.padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        Text(text = text, color = contentColor.copy(alpha = 0.4f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MoreTasksLabel(count: Int, cardColor: Color) {
    val contentColor = if (ColorUtils.calculateLuminance(cardColor.toArgb()) > 0.5) Color.Black else Color.White
    Text(
        text = stringResource(R.string.dashboard_more_tasks, count),
        style = MaterialTheme.typography.labelSmall,
        color = contentColor.copy(alpha = 0.6f),
        modifier = Modifier.padding(top = 2.dp),
        fontWeight = FontWeight.Bold
    )
}

/* ---------------- TASK ITEM ---------------- */

@Composable
fun TaskItem(
    task: TaskWithContext,
    actions: TaskActions,
    cardColor: Color,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var activeAction by remember { mutableStateOf<TaskAction>(TaskAction.None) }
    var newTitle by remember { mutableStateOf(task.title) }

    val isOverdue = (task.dueDate.toEpochMilli() < System.currentTimeMillis() && !task.completed) || (task.blocked && !task.completed)
    val contentColor = if (ColorUtils.calculateLuminance(cardColor.toArgb()) > 0.5) Color.Black else Color.White

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = stringResource(R.string.task_action_toggle_complete),
            tint = when {
                task.completed -> CompletedGreen
                isOverdue -> OverdueRed
                else -> contentColor.copy(alpha = 0.5f)
            },
            modifier = Modifier.size(18.dp).clickable { actions.onToggleComplete(task.toTaskEntity()) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            Column {
                Text(
                    text = task.title,
                    color = when {
                        task.completed -> CompletedGreen
                        isOverdue -> OverdueRed
                        else -> contentColor
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showMenu = true }
                )
                val timeText = remember(task.dueDate) {
                    task.dueDate.atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
                }
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        task.completed -> CompletedGreen.copy(alpha = 0.7f)
                        isOverdue -> OverdueRed.copy(alpha = 0.7f)
                        else -> contentColor.copy(alpha = 0.5f)
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text(text = "Project: ${task.projectName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                    onClick = { showMenu = false },
                    enabled = false
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.task_menu_rename), fontWeight = FontWeight.Bold) },
                    onClick = { showMenu = false; newTitle = task.title; activeAction = TaskAction.Rename }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.task_menu_notify), fontWeight = FontWeight.Bold) },
                    onClick = { showMenu = false; activeAction = TaskAction.Notify }
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                if (task.priority < 2) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.task_menu_raise_priority), fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                        onClick = { showMenu = false; actions.updatePriority(task.toTaskEntity(), task.priority + 1) }
                    )
                }
                if (task.priority > 0) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.task_menu_lower_priority), fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) },
                        onClick = { showMenu = false; actions.updatePriority(task.toTaskEntity(), task.priority - 1) }
                    )
                }
                DropdownMenuItem(
                    text = { Text(if (task.blocked) stringResource(R.string.task_menu_clear_overdue) else stringResource(R.string.task_menu_mark_overdue), fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                    onClick = { showMenu = false; actions.onToggleOverdue(task.toTaskEntity()) }
                )
            }
        }
        if (task.priority != 1) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (task.priority == 2) Icons.Default.Star else Icons.Default.Schedule,
                contentDescription = if (task.priority == 2) "High Priority" else "Low Priority",
                tint = if (task.priority == 2) PriorityGold else contentColor.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.task_action_delete_title),
            tint = contentColor.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp).clickable { activeAction = TaskAction.Delete }
        )
    }

    when (activeAction) {
        is TaskAction.Rename -> {
            AlertDialog(
                onDismissRequest = { activeAction = TaskAction.None },
                title = { Text(stringResource(R.string.task_action_rename_title), fontWeight = FontWeight.Bold) },
                text = { TextField(value = newTitle, onValueChange = { newTitle = it }) },
                confirmButton = {
                    TextButton(onClick = { actions.onUpdate(task.toTaskEntity(title = newTitle)); activeAction = TaskAction.None }) {
                        Text(stringResource(R.string.task_action_ok), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeAction = TaskAction.None }) {
                        Text(stringResource(R.string.task_action_cancel), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
        is TaskAction.Delete -> {
            AlertDialog(
                onDismissRequest = { activeAction = TaskAction.None },
                title = { Text(stringResource(R.string.task_action_delete_title), fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(R.string.task_action_delete_msg)) },
                confirmButton = {
                    TextButton(onClick = { actions.onRemove(task.toTaskEntity()); activeAction = TaskAction.None }) {
                        Text(stringResource(R.string.task_action_yes), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeAction = TaskAction.None }) {
                        Text(stringResource(R.string.task_action_no), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
        is TaskAction.Notify -> {
            NotifyClientDialog(task = task, isOverdue = isOverdue, onDismiss = { activeAction = TaskAction.None })
        }
        else -> {}
    }
}

/* ---------------- NOTIFY CLIENT DIALOG ---------------- */

@Composable
fun NotifyClientDialog(
    task: TaskWithContext,
    isOverdue: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedMethod by remember { mutableStateOf("Text") }
    val presetMessage = when {
        task.completed -> stringResource(R.string.notify_client_msg_complete, task.clientName, task.title)
        isOverdue -> stringResource(R.string.notify_client_msg_overdue, task.clientName, task.title)
        else -> stringResource(R.string.notify_client_msg_working, task.clientName, task.title)
    }
    val emailSubject = stringResource(R.string.notify_client_subject, task.title)
    var customMessage by remember { mutableStateOf(presetMessage) }
    var usePreset by remember { mutableStateOf(true) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notify_client_title, task.clientName), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(selected = selectedMethod == "Text", onClick = { selectedMethod = "Text" },
                        label = { Text(stringResource(R.string.notify_client_text), fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Sms, null, Modifier.size(18.dp)) })
                    FilterChip(selected = selectedMethod == "Email", onClick = { selectedMethod = "Email" },
                        label = { Text(stringResource(R.string.notify_client_email), fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Email, null, Modifier.size(18.dp)) })
                }
                Text(stringResource(R.string.notify_client_method), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = customMessage,
                    onValueChange = { customMessage = it; usePreset = false },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text(stringResource(R.string.notify_client_placeholder)) }
                )
                if (!usePreset) {
                    TextButton(onClick = { customMessage = presetMessage; usePreset = true }) {
                        Text(stringResource(R.string.notify_client_reset), fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (selectedMethod == "Text") sendSms(context, task.clientPhone, customMessage)
                else sendEmail(context, task.clientEmail, emailSubject, customMessage)
                onDismiss()
            }) { Text(stringResource(R.string.notify_client_send), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.task_action_cancel), fontWeight = FontWeight.Bold) }
        }
    )
}

/* ---------------- INTENT HELPERS ---------------- */

private fun sendSms(context: Context, phoneNumber: String, message: String) {
    try {
        val uri = "smsto:$phoneNumber".toUri()
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply { putExtra("sms_body", message) }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No SMS app found", Toast.LENGTH_SHORT).show()
    }
}

private fun sendEmail(context: Context, emailAddress: String, subject: String, body: String) {
    try {
        val uri = "mailto:$emailAddress".toUri()
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
    }
}

/* ---------------- UPCOMING SECTION ---------------- */

@Composable
fun UpcomingSection(
    tasks: List<TaskWithContext>,
    expandedDay: SelectedDay,
    onDaySelected: (SelectedDay) -> Unit,
    actions: TaskActions,
    dayCardColor: Color,
    primaryAccent: Color,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.dashboard_upcoming),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        val density = androidx.compose.ui.platform.LocalDensity.current
        var rowWidthDp by remember { mutableStateOf(0.dp) }
        val expandedCardWidth = if (rowWidthDp > 0.dp) (rowWidthDp - 16.dp) * (2.2f / 4.2f) else 280.dp

        LazyRow(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .onSizeChanged {
                    if (rowWidthDp == 0.dp) {
                        rowWidthDp = with(density) { it.width.toDp() }
                    }
                },
            contentPadding = PaddingValues(end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            items(items = (2..7).toList()) { daysOffset ->
                val targetDate = today.plusDays(daysOffset.toLong())
                val dayTasks = getTasksForDay(tasks, targetDate)
                val dayLabel = getDayName(targetDate)
                val dateLabel = getFormattedDate(targetDate)
                val current = SelectedDay(targetDate)
                val isExpanded = expandedDay.date == targetDate

                UpcomingDayCard(
                    label = dayLabel,
                    dateLabel = dateLabel,
                    tasks = dayTasks,
                    isExpanded = isExpanded,
                    onClick = { onDaySelected(current) },
                    actions = actions,
                    cardColor = dayCardColor,
                    primaryAccent = primaryAccent,
                    expandedWidth = expandedCardWidth
                )
            }
        }
    }
}

/* ---------------- UPCOMING DAY CARD ---------------- */

@Composable
fun UpcomingDayCard(
    label: String,
    dateLabel: String,
    tasks: List<TaskWithContext>,
    isExpanded: Boolean,
    onClick: () -> Unit,
    actions: TaskActions,
    cardColor: Color,
    primaryAccent: Color,
    expandedWidth: androidx.compose.ui.unit.Dp = 280.dp,
    modifier: Modifier = Modifier
) {
    val animatedWidth by animateDpAsState(
        targetValue = if (isExpanded) expandedWidth else 120.dp,
        label = "width"
    )
    val animatedMinHeight by animateDpAsState(
        targetValue = if (isExpanded) 280.dp else 120.dp,
        label = "minHeight"
    )

    Card(
        modifier = modifier
            .width(animatedWidth)
            .heightIn(min = animatedMinHeight)
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
            .focusProperties { canFocus = false }
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) cardColor.copy(alpha = 0.8f) else cardColor
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = if (ColorUtils.calculateLuminance(cardColor.toArgb()) > 0.5) Color.Black else Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = (if (ColorUtils.calculateLuminance(cardColor.toArgb()) > 0.5) Color.Black else Color.White).copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (isExpanded) {
                ExpandedTaskContent(tasks = tasks, actions = actions, cardColor = cardColor)
            } else {
                CollapsedTaskContent(tasks, primaryAccent, cardColor)
            }
        }
    }
}

/* ---------------- HELPERS ---------------- */

private fun getTasksForDay(allTasks: List<TaskWithContext>, date: LocalDate): List<TaskWithContext> {
    return allTasks.filter { task ->
        task.dueDate.atZone(ZoneId.systemDefault()).toLocalDate() == date
    }.sortedByDescending { it.priority }
}

private fun getDayName(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
}

private fun getFormattedDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
}