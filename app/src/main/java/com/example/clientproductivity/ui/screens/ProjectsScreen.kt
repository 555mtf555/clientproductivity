package com.example.clientproductivity.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.example.clientproductivity.data.entity.ClientEntity
import com.example.clientproductivity.data.entity.ProjectEntity
import com.example.clientproductivity.data.entity.TaskEntity
import com.example.clientproductivity.data.extensions.toTaskEntity
import com.example.clientproductivity.viewmodel.ClientViewModel
import com.example.clientproductivity.viewmodel.TaskViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import com.example.clientproductivity.util.VoiceInputButton
import com.example.clientproductivity.util.rememberVoiceInputController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    clientViewModel: ClientViewModel,
    taskViewModel: TaskViewModel,
    cardColor: Color,
    themeName: String = ""
) {
    val clients by clientViewModel.getClients().collectAsState(initial = emptyList())
    var expandedClientId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Projects & Folders",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            items(clients, key = { it.id }) { client ->
                ClientFolderItem(
                    client = client,
                    isExpanded = expandedClientId == client.id,
                    onToggle = {
                        expandedClientId = if (expandedClientId == client.id) null else client.id
                    },
                    clientViewModel = clientViewModel,
                    taskViewModel = taskViewModel,
                    cardColor = cardColor,
                    themeName = themeName
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun ClientFolderItem(
    client: ClientEntity,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    clientViewModel: ClientViewModel,
    taskViewModel: TaskViewModel,
    cardColor: Color,
    themeName: String = "",
    modifier: Modifier = Modifier
) {
    val projects by clientViewModel.getProjects(client.id).collectAsState(initial = emptyList())
    val showAddProjectDialog = remember { mutableStateOf(false) }

    val isLightCard = ColorUtils.calculateLuminance(cardColor.toArgb()) > 0.5
    val contentColor = if (isLightCard) Color.Black else Color.White

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) cardColor.copy(alpha = 0.9f) else cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                    contentDescription = null,
                    tint = if (isExpanded) MaterialTheme.colorScheme.primary else contentColor
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = client.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Text(
                        text = "${projects.size} Projects",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = { showAddProjectDialog.value = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Project", tint = contentColor)
                }
            }

            if (isExpanded) {
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    projects.forEach { project ->
                        ProjectItem(
                            project = project,
                            clientViewModel = clientViewModel,
                            taskViewModel = taskViewModel,
                            cardColor = cardColor,
                            contentColor = contentColor,
                            themeName = themeName
                        )
                    }
                    if (projects.isEmpty()) {
                        Text(
                            "No projects yet",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = contentColor.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    if (showAddProjectDialog.value) {
        var projectName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddProjectDialog.value = false },
            title = { Text("New Project for ${client.name}", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    clientViewModel.addProject(ProjectEntity(clientId = client.id, name = projectName, description = ""))
                    showAddProjectDialog.value = false
                }) { Text("Create", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddProjectDialog.value = false }) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun ProjectItem(
    project: ProjectEntity,
    clientViewModel: ClientViewModel,
    taskViewModel: TaskViewModel,
    cardColor: Color,
    contentColor: Color,
    themeName: String = "",
    modifier: Modifier = Modifier
) {
    var isProjectExpanded by remember { mutableStateOf(false) }
    val allTasks by taskViewModel.allTasks.collectAsState()
    val projectTasks = remember(allTasks, project.id) {
        allTasks.filter { it.projectId == project.id }
    }

    val showAddTaskDialog = remember { mutableStateOf(false) }
    val showRenameDialog = remember { mutableStateOf(false) }
    val showDeleteProjectDialog = remember { mutableStateOf(false) }

    // Slightly elevated card so projects visually separate from parent card background
    val projectCardColor = if (ColorUtils.calculateLuminance(cardColor.toArgb()) > 0.5) {
        Color(ColorUtils.blendARGB(cardColor.toArgb(), Color.Black.toArgb(), 0.08f))
    } else {
        Color(ColorUtils.blendARGB(cardColor.toArgb(), Color.White.toArgb(), 0.12f))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = projectCardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isProjectExpanded = !isProjectExpanded }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isProjectExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    color = contentColor
                )
                IconButton(onClick = { showRenameDialog.value = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(16.dp), tint = contentColor)
                }
                IconButton(onClick = { showAddTaskDialog.value = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.AddTask, contentDescription = "Add Task", modifier = Modifier.size(16.dp), tint = contentColor)
                }
                IconButton(onClick = { showDeleteProjectDialog.value = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Project", modifier = Modifier.size(16.dp), tint = Color(0xFFD32F2F))
                }
            }

            if (isProjectExpanded) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    projectTasks.forEach { task ->
                        val logs by taskViewModel.getLogsForTask(task.taskId).collectAsState(initial = emptyList())
                        TaskRow(
                            task = task,
                            logs = logs,
                            onToggleComplete = { taskViewModel.toggleComplete(task.toTaskEntity()) },
                            onToggleOverdue = { taskViewModel.toggleOverdue(task.toTaskEntity()) },
                            onTaskRemove = { taskViewModel.removeTask(task.toTaskEntity()) },
                            onClick = null,
                            themeName = themeName
                        )
                    }
                    if (projectTasks.isEmpty()) {
                        Text(
                            "No tasks",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 4.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    if (showRenameDialog.value) {
        var newName by remember { mutableStateOf(project.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog.value = false },
            title = { Text("Rename Project", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it })
            },
            confirmButton = {
                Button(onClick = {
                    clientViewModel.updateProject(project.copy(name = newName))
                    showRenameDialog.value = false
                }) { Text("Save", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog.value = false }) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAddTaskDialog.value) {
        AddTaskToProjectDialog(
            projectName = project.name,
            onDismiss = { showAddTaskDialog.value = false },
            onConfirm = { title, notes, dueInstant ->
                taskViewModel.addTask(
                    TaskEntity(projectId = project.id, title = title, notes = notes, dueDate = dueInstant)
                )
                showAddTaskDialog.value = false
            }
        )
    }

    if (showDeleteProjectDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteProjectDialog.value = false },
            title = { Text("Delete Project", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to delete \"${project.name}\"?")
                    if (projectTasks.isNotEmpty()) {
                        Text(
                            text = "This will also permanently delete ${projectTasks.size} task${if (projectTasks.size == 1) "" else "s"}.",
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text("This project has no tasks.")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clientViewModel.deleteProject(project)
                        showDeleteProjectDialog.value = false
                    }
                ) {
                    Text("Delete", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteProjectDialog.value = false }) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/* ---------------- ADD TASK TO PROJECT DIALOG ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskToProjectDialog(
    projectName: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, notes: String, dueDate: Instant) -> Unit
) {
    var taskTitle by remember { mutableStateOf("") }
    var taskNotes by remember { mutableStateOf("") }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var selectedHour by remember { mutableIntStateOf(17) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var isAm by remember { mutableStateOf(false) }
    var selectingHour by remember { mutableStateOf(true) }

    val voiceController = rememberVoiceInputController(
        onResult = { parsed ->
            if (parsed.title.isNotBlank()) taskTitle = parsed.title
            if (parsed.notes.isNotBlank()) taskNotes = parsed.notes
            parsed.dueInstant?.let { instant ->
                val zdt = instant.atZone(ZoneId.systemDefault())
                val utcMillis = zdt.toLocalDate()
                    .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                datePickerState.selectedDateMillis = utcMillis
                selectedHour = zdt.hour
                selectedMinute = zdt.minute
                isAm = zdt.hour < 12
            }
        }
    )

    val selectedDateText = remember(datePickerState.selectedDateMillis) {
        val millis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(millis))
    }
    val displayHour = if (selectedHour % 12 == 0) 12 else selectedHour % 12
    val selectedTimeText = String.format(
        Locale.getDefault(), "%02d:%02d %s",
        displayHour, selectedMinute, if (isAm) "AM" else "PM"
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showTimePicker = false }) {
            OutlinedCard(shape = RoundedCornerShape(24.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Select Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())

                    val accent = MaterialTheme.colorScheme.primary
                    val onAccent = if (ColorUtils.calculateLuminance(accent.toArgb()) > 0.5) Color.Black else Color.White

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        TimeChip(text = String.format(Locale.getDefault(), "%02d", displayHour), selected = selectingHour, accentColor = accent, onAccent = onAccent, onClick = { selectingHour = true })
                        Text(":", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                        TimeChip(text = String.format(Locale.getDefault(), "%02d", selectedMinute), selected = !selectingHour, accentColor = accent, onAccent = onAccent, onClick = { selectingHour = false })
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            AmPmButton(text = "AM", selected = isAm, accentColor = accent, onAccent = onAccent, onClick = { isAm = true; if (selectedHour >= 12) selectedHour -= 12 })
                            AmPmButton(text = "PM", selected = !isAm, accentColor = accent, onAccent = onAccent, onClick = { isAm = false; if (selectedHour < 12) selectedHour += 12 })
                        }
                    }

                    Text(
                        text = if (selectingHour) "Tap clock to select hour" else "Tap clock to select minute",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    ClockFace(
                        hour24 = selectedHour, minute = selectedMinute,
                        selectingHour = selectingHour, accentColor = accent, onAccent = onAccent, isAm = isAm,
                        onTimeChange = { h, m -> selectedHour = h; selectedMinute = m; if (selectingHour) selectingHour = false },
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(4.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel", fontWeight = FontWeight.Bold) }
                        TextButton(onClick = { showTimePicker = false }) { Text("OK", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Task for $projectName", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Task Title") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    VoiceInputButton(controller = voiceController)
                }
                OutlinedTextField(
                    value = taskNotes,
                    onValueChange = { taskNotes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Due Date", style = MaterialTheme.typography.labelSmall)
                                Text(selectedDateText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                    OutlinedCard(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Due Time", style = MaterialTheme.typography.labelSmall)
                                Text(selectedTimeText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val utcMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val date = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    val instant = LocalDateTime.of(date, LocalTime.of(selectedHour, selectedMinute))
                        .atZone(ZoneId.systemDefault()).toInstant()
                    onConfirm(taskTitle, taskNotes, instant)
                },
                enabled = taskTitle.isNotBlank()
            ) { Text("Add", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", fontWeight = FontWeight.Bold) }
        }
    )
}