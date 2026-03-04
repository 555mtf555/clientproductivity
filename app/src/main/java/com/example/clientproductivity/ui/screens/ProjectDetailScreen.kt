package com.example.clientproductivity.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.clientproductivity.R
import com.example.clientproductivity.data.extensions.toTaskEntity
import com.example.clientproductivity.viewmodel.ClientViewModel
import com.example.clientproductivity.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: Long,
    clientViewModel: ClientViewModel,
    taskViewModel: TaskViewModel,
    onBack: () -> Unit,
    onAddTask: (Long) -> Unit
) {
    val project by clientViewModel.getProject(projectId).collectAsState(initial = null)
    val tasks by taskViewModel.allTasks.collectAsState()
    
    var name by remember(project) { mutableStateOf(project?.name ?: "") }
    var description by remember(project) { mutableStateOf(project?.description ?: "") }

    val projectTasks = remember(tasks, projectId) {
        tasks.filter { it.projectId == projectId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "Project Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            project?.let {
                                clientViewModel.updateProject(it.copy(name = name, description = description))
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddTask(projectId) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Project Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.nav_tasks), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn {
                items(projectTasks) { task ->
                    val logs by taskViewModel.getLogsForTask(task.taskId).collectAsState(initial = emptyList())
                    TaskRow(
                        task = task,
                        logs = logs,
                        onToggleComplete = { taskViewModel.toggleComplete(task.toTaskEntity()) },
                        onToggleOverdue = { taskViewModel.toggleOverdue(task.toTaskEntity()) },
                        onTaskRemove = { taskViewModel.removeTask(task.toTaskEntity()) },
                        onRestore = { taskViewModel.restoreTask(task.toTaskEntity()) }
                    )
                }
            }
        }
    }
}
