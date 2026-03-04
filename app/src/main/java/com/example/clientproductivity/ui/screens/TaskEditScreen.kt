package com.example.clientproductivity.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.clientproductivity.R
import com.example.clientproductivity.data.extensions.toTaskEntity
import com.example.clientproductivity.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    taskId: Long,
    taskViewModel: TaskViewModel,
    onClose: () -> Unit,
    onTaskSaved: () -> Unit
) {
    val taskWithContext by taskViewModel.getTaskById(taskId).collectAsState(initial = null)
    
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(Instant.now()) }

    LaunchedEffect(taskWithContext) {
        taskWithContext?.let {
            title = it.title
            notes = it.notes
            dueDate = it.dueDate
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueDate.toEpochMilli()
    )
    val showDatePicker = remember { mutableStateOf(false) }

    val selectedDateText = remember(datePickerState.selectedDateMillis) {
        val dateMillis = datePickerState.selectedDateMillis ?: dueDate.toEpochMilli()
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        sdf.format(Date(dateMillis))
    }

    if (showDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker.value = false }) {
                    Text(stringResource(R.string.task_action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker.value = false }) {
                    Text(stringResource(R.string.task_action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Edit Task") },
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.task_action_cancel))
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
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.task_creation_field_title)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.task_creation_field_notes)) },
                modifier = Modifier.fillMaxWidth()
            )

            // Date Selection
            OutlinedCard(
                onClick = { showDatePicker.value = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.task_creation_due_date, selectedDateText))
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank(),
                onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis ?: dueDate.toEpochMilli()
                    val selectedInstant = Instant.ofEpochMilli(selectedMillis)
                    
                    taskWithContext?.let {
                        val updatedTask = it.toTaskEntity(
                            title = title,
                            notes = notes
                        ).copy(dueDate = selectedInstant)
                        
                        taskViewModel.updateTask(updatedTask)
                        onTaskSaved()
                    }
                }
            ) { 
                Text(stringResource(R.string.task_creation_save))
            }
        }
    }
}
