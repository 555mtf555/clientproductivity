package com.example.clientproductivity.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.clientproductivity.R
import com.example.clientproductivity.data.extensions.toTaskEntity
import com.example.clientproductivity.ui.components.EmptyState
import com.example.clientproductivity.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TasksScreen(
    taskViewModel: TaskViewModel,
    onTaskClick: (Long) -> Unit
) {
    val allTasks by taskViewModel.allTasks.collectAsState()
    val removedTasks by taskViewModel.recentlyRemovedTasks.collectAsState()
    val searchQuery by taskViewModel.searchQuery.collectAsState()
    
    val selectedTaskIds = remember { mutableStateListOf<Long>() }
    val isMultiSelectMode by remember { derivedStateOf { selectedTaskIds.isNotEmpty() } }
    
    val showBulkDeleteConfirm = remember { mutableStateOf(false) }
    val showBulkRestoreConfirm = remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (isMultiSelectMode) {
                TopAppBar(
                    title = { Text("${selectedTaskIds.size} Selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectedTaskIds.clear() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showBulkRestoreConfirm.value = true }) {
                            Icon(Icons.Default.Restore, contentDescription = "Restore Selected")
                        }
                        IconButton(onClick = { showBulkDeleteConfirm.value = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = Color.Red)
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.tasks_title)) }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search Bar (Hidden during multi-select)
            AnimatedVisibility(visible = !isMultiSelectMode) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { taskViewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Search tasks, clients, or projects...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            if (allTasks.isEmpty() && removedTasks.isEmpty()) {
                EmptyState(
                    message = stringResource(R.string.tasks_empty_state),
                    icon = Icons.Default.Assignment
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (allTasks.isNotEmpty() && !isMultiSelectMode) {
                        item {
                            Text(
                                stringResource(R.string.tasks_active_header),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        items(allTasks) { task ->
                            val logs by taskViewModel.getLogsForTask(task.taskId).collectAsState(initial = emptyList())
                            TaskRow(
                                task = task,
                                logs = logs,
                                onToggleComplete = {
                                    taskViewModel.toggleComplete(task.toTaskEntity())
                                },
                                onToggleOverdue = {
                                    taskViewModel.toggleOverdue(task.toTaskEntity())
                                },
                                onTaskRemove = {
                                    taskViewModel.removeTask(task.toTaskEntity())
                                },
                                onClick = { onTaskClick(task.taskId) }
                            )
                        }
                    }

                    if (removedTasks.isNotEmpty() && searchQuery.isEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                stringResource(R.string.tasks_removed_header),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(removedTasks) { task ->
                            val isSelected = selectedTaskIds.contains(task.taskId)
                            val logs by taskViewModel.getLogsForTask(task.taskId).collectAsState(initial = emptyList())
                            
                            Box(modifier = Modifier.combinedClickable(
                                onClick = {
                                    if (isMultiSelectMode) {
                                        if (isSelected) selectedTaskIds.remove(task.taskId)
                                        else selectedTaskIds.add(task.taskId)
                                    }
                                },
                                onLongClick = {
                                    if (!isMultiSelectMode) {
                                        selectedTaskIds.add(task.taskId)
                                    }
                                }
                            )) {
                                TaskRow(
                                    task = task,
                                    logs = logs,
                                    onToggleComplete = {},
                                    onToggleOverdue = {},
                                    onRestore = {
                                        taskViewModel.restoreTask(task.toTaskEntity())
                                    },
                                    onTaskRemove = {
                                        taskViewModel.deletePermanently(task.toTaskEntity())
                                    },
                                    highlightColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBulkDeleteConfirm.value) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm.value = false },
            title = { Text("Delete Multiple Tasks") },
            text = { Text("Are you sure you want to permanently delete these ${selectedTaskIds.size} tasks?") },
            confirmButton = {
                TextButton(onClick = {
                    val tasksToDelete = removedTasks.filter { it.taskId in selectedTaskIds }
                    taskViewModel.deleteMultiplePermanently(tasksToDelete.map { it.toTaskEntity() })
                    selectedTaskIds.clear()
                    showBulkDeleteConfirm.value = false
                }) {
                    Text(stringResource(R.string.task_action_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm.value = false }) {
                    Text(stringResource(R.string.task_action_no))
                }
            }
        )
    }

    if (showBulkRestoreConfirm.value) {
        AlertDialog(
            onDismissRequest = { showBulkRestoreConfirm.value = false },
            title = { Text("Restore Multiple Tasks") },
            text = { Text("Are you sure you want to restore these ${selectedTaskIds.size} tasks?") },
            confirmButton = {
                TextButton(onClick = {
                    val tasksToRestore = removedTasks.filter { it.taskId in selectedTaskIds }
                    taskViewModel.restoreMultiple(tasksToRestore.map { it.toTaskEntity() })
                    selectedTaskIds.clear()
                    showBulkRestoreConfirm.value = false
                }) {
                    Text(stringResource(R.string.task_action_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkRestoreConfirm.value = false }) {
                    Text(stringResource(R.string.task_action_no))
                }
            }
        )
    }
}
