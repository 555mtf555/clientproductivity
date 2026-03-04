package com.example.clientproductivity.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.ColorUtils
import com.example.clientproductivity.R
import com.example.clientproductivity.data.entity.TaskEntity
import com.example.clientproductivity.util.VoiceInputButton
import com.example.clientproductivity.util.rememberVoiceInputController
import com.example.clientproductivity.viewmodel.ClientViewModel
import com.example.clientproductivity.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCreationScreen(
    projectId: Long?,
    taskViewModel: TaskViewModel,
    clientViewModel: ClientViewModel,
    initialDateMillis: Long? = null,
    onClose: () -> Unit,
    onTaskSaved: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var clientEmail by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }

    // Project picker state — only shown when projectId is null (new standalone task)
    val allProjects by clientViewModel.getAllProjects().collectAsState(initial = emptyList())
    val allClients by clientViewModel.getAllClients().collectAsState(initial = emptyList())
    var selectedProjectId by remember { mutableStateOf(projectId) }
    var useExistingProject by remember { mutableStateOf(projectId != null) }
    var projectDropdownExpanded by remember { mutableStateOf(false) }

    // When a project is selected, autofill client info from the matching client
    val selectedProject = remember(selectedProjectId, allProjects) {
        allProjects.find { it.id == selectedProjectId }
    }
    val selectedClient = remember(selectedProject, allClients) {
        allClients.find { it.id == selectedProject?.clientId }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis ?: System.currentTimeMillis()
    )
    val showDatePicker = remember { mutableStateOf(false) }
    val showTimePicker = remember { mutableStateOf(false) }

    var selectedHour by remember { mutableIntStateOf(17) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var isAm by remember { mutableStateOf(false) }
    var selectingHour by remember { mutableStateOf(true) }

    val voiceController = rememberVoiceInputController(
        onResult = { parsed ->
            if (parsed.title.isNotBlank()) title = parsed.title
            if (parsed.notes.isNotBlank()) notes = parsed.notes
            parsed.dueInstant?.let { instant ->
                val zdt = instant.atZone(ZoneId.systemDefault())
                // Update date picker to parsed date (UTC millis for DatePicker)
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
        val dateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(dateMillis))
    }

    val displayHour = if (selectedHour % 12 == 0) 12 else selectedHour % 12
    val selectedTimeText = String.format(
        Locale.getDefault(), "%02d:%02d %s",
        displayHour, selectedMinute, if (isAm) "AM" else "PM"
    )

    // Date picker dialog
    if (showDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker.value = false }) {
                    Text(stringResource(R.string.task_action_ok), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker.value = false }) {
                    Text(stringResource(R.string.task_action_cancel), fontWeight = FontWeight.Bold)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog
    if (showTimePicker.value) {
        Dialog(onDismissRequest = { showTimePicker.value = false }) {
            OutlinedCard(
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Select Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val accent = MaterialTheme.colorScheme.primary
                    val onAccent = if (ColorUtils.calculateLuminance(accent.toArgb()) > 0.5) Color.Black else Color.White

                    // HH:MM + AM/PM row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeChip(
                            text = String.format(Locale.getDefault(), "%02d", displayHour),
                            selected = selectingHour,
                            accentColor = accent,
                            onAccent = onAccent,
                            onClick = { selectingHour = true }
                        )
                        Text(
                            text = ":",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        TimeChip(
                            text = String.format(Locale.getDefault(), "%02d", selectedMinute),
                            selected = !selectingHour,
                            accentColor = accent,
                            onAccent = onAccent,
                            onClick = { selectingHour = false }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            AmPmButton(
                                text = "AM", selected = isAm,
                                accentColor = accent, onAccent = onAccent,
                                onClick = {
                                    isAm = true
                                    if (selectedHour >= 12) selectedHour -= 12
                                }
                            )
                            AmPmButton(
                                text = "PM", selected = !isAm,
                                accentColor = accent, onAccent = onAccent,
                                onClick = {
                                    isAm = false
                                    if (selectedHour < 12) selectedHour += 12
                                }
                            )
                        }
                    }

                    Text(
                        text = if (selectingHour) "Tap clock to select hour" else "Tap clock to select minute",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    ClockFace(
                        hour24 = selectedHour,
                        minute = selectedMinute,
                        selectingHour = selectingHour,
                        accentColor = accent,
                        onAccent = onAccent,
                        isAm = isAm,
                        onTimeChange = { h, m ->
                            selectedHour = h
                            selectedMinute = m
                            if (selectingHour) selectingHour = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker.value = false }) {
                            Text(stringResource(R.string.task_action_cancel), fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { showTimePicker.value = false }) {
                            Text(stringResource(R.string.task_action_ok), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.task_creation_title), fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.task_action_cancel))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.task_creation_field_title)) },
                    modifier = Modifier.weight(1f)
                )
                VoiceInputButton(controller = voiceController)
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.task_creation_field_notes)) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedCard(
                    onClick = { showDatePicker.value = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Due Date", style = MaterialTheme.typography.labelSmall)
                            Text(text = selectedDateText, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    }
                }
                OutlinedCard(
                    onClick = { showTimePicker.value = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Due Time", style = MaterialTheme.typography.labelSmall)
                            Text(text = selectedTimeText, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.AccessTime, contentDescription = null)
                    }
                }
            }

            // Only show project/client section when not launched from a specific project
            if (projectId == null) {
                // Toggle: existing project vs new client
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = useExistingProject,
                        onClick = { useExistingProject = true },
                        label = { Text("Existing Project", fontWeight = FontWeight.Bold) }
                    )
                    FilterChip(
                        selected = !useExistingProject,
                        onClick = {
                            useExistingProject = false
                            selectedProjectId = null
                        },
                        label = { Text("New Client", fontWeight = FontWeight.Bold) }
                    )
                }

                if (useExistingProject) {
                    // Project dropdown
                    Box {
                        OutlinedCard(
                            onClick = { projectDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Project",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = selectedProject?.name ?: "Select a project…",
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (selectedClient != null) {
                                        Text(
                                            text = selectedClient.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Icon(Icons.Default.ExpandMore, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = projectDropdownExpanded,
                            onDismissRequest = { projectDropdownExpanded = false }
                        ) {
                            if (allProjects.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No projects yet") },
                                    onClick = { projectDropdownExpanded = false }
                                )
                            }
                            allProjects.forEach { project ->
                                val client = allClients.find { it.id == project.clientId }
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(project.name, fontWeight = FontWeight.Bold)
                                            if (client != null) {
                                                Text(
                                                    text = client.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedProjectId = project.id
                                        projectDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Autofilled client info — read only
                    if (selectedClient != null) {
                        OutlinedTextField(
                            value = selectedClient.name,
                            onValueChange = {},
                            label = { Text(stringResource(R.string.clients_field_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false
                        )
                        if (selectedClient.email.isNotBlank()) {
                            OutlinedTextField(
                                value = selectedClient.email,
                                onValueChange = {},
                                label = { Text(stringResource(R.string.clients_field_email)) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false
                            )
                        }
                        if (selectedClient.phone.isNotBlank()) {
                            OutlinedTextField(
                                value = selectedClient.phone,
                                onValueChange = {},
                                label = { Text(stringResource(R.string.clients_field_phone)) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false
                            )
                        }
                    }
                } else {
                    // New client fields
                    Text(
                        text = stringResource(R.string.task_creation_client_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text(stringResource(R.string.task_creation_client_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (clientName.isNotBlank()) {
                        OutlinedTextField(
                            value = clientEmail,
                            onValueChange = { clientEmail = it },
                            label = { Text(stringResource(R.string.clients_field_email)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = clientPhone,
                            onValueChange = { clientPhone = it },
                            label = { Text(stringResource(R.string.clients_field_phone)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && (
                        projectId != null ||
                                (useExistingProject && selectedProjectId != null) ||
                                (!useExistingProject && clientName.isNotBlank())
                        ),
                onClick = {
                    val selectedUtcMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val selectedDate = Instant.ofEpochMilli(selectedUtcMillis)
                        .atZone(ZoneOffset.UTC).toLocalDate()
                    val dueDateTime = LocalDateTime.of(selectedDate, LocalTime.of(selectedHour, selectedMinute))
                    val localDueInstant = dueDateTime.atZone(ZoneId.systemDefault()).toInstant()

                    when {
                        // Launched from a specific project (e.g. from ProjectsScreen)
                        projectId != null -> {
                            taskViewModel.addTask(
                                TaskEntity(projectId = projectId, title = title, notes = notes, dueDate = localDueInstant)
                            )
                        }
                        // User picked an existing project
                        useExistingProject && selectedProjectId != null -> {
                            taskViewModel.addTask(
                                TaskEntity(projectId = selectedProjectId!!, title = title, notes = notes, dueDate = localDueInstant)
                            )
                        }
                        // User is creating a new client
                        !useExistingProject && clientName.isNotBlank() -> {
                            taskViewModel.addTaskWithNewClient(
                                taskTitle = title, taskNotes = notes, taskDueDate = localDueInstant,
                                clientName = clientName, clientEmail = clientEmail, clientPhone = clientPhone,
                                projectName = "Default Project"
                            )
                        }
                    }
                    onTaskSaved()
                }
            ) {
                Text(stringResource(R.string.task_creation_save), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* ---------------- CLOCK FACE ---------------- */

@Composable
internal fun ClockFace(
    hour24: Int,
    minute: Int,
    selectingHour: Boolean,
    accentColor: Color,
    onAccent: Color,
    isAm: Boolean,
    onTimeChange: (hour24: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val handAngleDeg = if (selectingHour) {
        (hour24 % 12) * 30f
    } else {
        minute * 6f
    }
    val animatedAngle by animateFloatAsState(
        targetValue = handAngleDeg,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "handAngle"
    )

    Canvas(
        modifier = modifier
            .clip(CircleShape)
            .background(surfaceColor)
            .pointerInput(selectingHour, isAm) {
                detectTapGestures { offset ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    var angle = atan2(offset.y - cy, offset.x - cx) - (-PI / 2)
                    if (angle < 0) angle += 2 * PI
                    if (selectingHour) {
                        val hour12 = ((angle / (2 * PI) * 12).roundToInt() % 12)
                        val newHour24 = if (isAm) { if (hour12 == 0) 0 else hour12 }
                        else { if (hour12 == 0) 12 else hour12 + 12 }
                        onTimeChange(newHour24, minute)
                    } else {
                        onTimeChange(hour24, ((angle / (2 * PI) * 60).roundToInt() % 60))
                    }
                }
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension / 2f
        val numberRadius = radius * 0.78f
        val handLength = radius * 0.6f
        val handAngleRad = (animatedAngle - 90f) * PI.toFloat() / 180f

        // Tick marks
        val totalTicks = if (selectingHour) 12 else 60
        for (i in 0 until totalTicks) {
            val tickAngle = (i * 360f / totalTicks - 90f) * PI.toFloat() / 180f
            val isMajor = if (selectingHour) true else (i % 5 == 0)
            drawLine(
                color = if (isMajor) onSurfaceVariant.copy(alpha = 0.5f) else onSurfaceVariant.copy(alpha = 0.2f),
                start = Offset(cx + cos(tickAngle) * radius * (if (isMajor) 0.82f else 0.87f), cy + sin(tickAngle) * radius * (if (isMajor) 0.82f else 0.87f)),
                end = Offset(cx + cos(tickAngle) * radius * 0.92f, cy + sin(tickAngle) * radius * 0.92f),
                strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Hand shadow
        val handEndX = cx + cos(handAngleRad) * handLength
        val handEndY = cy + sin(handAngleRad) * handLength
        drawLine(
            color = Color.Black.copy(alpha = 0.1f),
            start = Offset(cx + 2, cy + 2),
            end = Offset(handEndX + 2, handEndY + 2),
            strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round
        )
        // Hand
        drawLine(
            color = accentColor,
            start = Offset(cx, cy), end = Offset(handEndX, handEndY),
            strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round
        )
        // Center dot
        drawCircle(color = accentColor, radius = 8.dp.toPx(), center = Offset(cx, cy))
        drawCircle(color = onAccent, radius = 3.dp.toPx(), center = Offset(cx, cy))

        // Numbers
        drawClockNumbers(cx, cy, numberRadius, selectingHour, hour24, minute, accentColor, onAccent, onSurface)
    }
}

internal fun DrawScope.drawClockNumbers(
    cx: Float, cy: Float, numberRadius: Float,
    selectingHour: Boolean, hour24: Int, minute: Int,
    accentColor: Color, onAccent: Color, onSurface: Color
) {
    for (i in 1..12) {
        val displayValue = if (selectingHour) i else (i * 5) % 60
        val angleRad = (i * 30f - 90f) * PI.toFloat() / 180f
        val x = cx + cos(angleRad) * numberRadius
        val y = cy + sin(angleRad) * numberRadius

        val isSelected = if (selectingHour) {
            (hour24 % 12).let { if (it == 0) 12 else it } == i
        } else {
            minute == displayValue % 60
        }

        if (isSelected) {
            drawCircle(color = accentColor, radius = 18.dp.toPx(), center = Offset(x, y))
        }

        val paint = android.graphics.Paint().apply {
            color = if (isSelected) onAccent.toArgb() else onSurface.copy(alpha = 0.8f).toArgb()
            textSize = 14.dp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                displayValue.toString(), x, y + paint.textSize / 3f, paint
            )
        }
    }
}

/* ---------------- TIME CHIP + AM/PM BUTTON ---------------- */

@Composable
internal fun TimeChip(
    text: String, selected: Boolean,
    accentColor: Color, onAccent: Color,
    onClick: () -> Unit
) {
    val bgAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow), label = "chipBg"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = bgAlpha))
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = if (selected) onAccent else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun AmPmButton(
    text: String, selected: Boolean,
    accentColor: Color, onAccent: Color,
    onClick: () -> Unit
) {
    val bgAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow), label = "ampmBg"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.copy(alpha = bgAlpha))
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) onAccent else MaterialTheme.colorScheme.onSurface
        )
    }
}