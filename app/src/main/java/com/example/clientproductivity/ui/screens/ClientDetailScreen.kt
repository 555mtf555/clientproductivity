package com.example.clientproductivity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.example.clientproductivity.R
import com.example.clientproductivity.data.entity.ProjectEntity
import com.example.clientproductivity.viewmodel.ClientViewModel
import com.example.clientproductivity.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: Long,
    clientViewModel: ClientViewModel,
    taskViewModel: TaskViewModel,
    onBack: () -> Unit,
    cardColor: Color,
    themeName: String = ""
) {
    val client by clientViewModel.getClient(clientId).collectAsState(initial = null)
    val projects by clientViewModel.getProjects(clientId).collectAsState(initial = emptyList())

    var name by remember(client) { mutableStateOf(client?.name ?: "") }
    var email by remember(client) { mutableStateOf(client?.email ?: "") }
    var phone by remember(client) { mutableStateOf(client?.phone ?: "") }

    val showAddProjectDialog = remember { mutableStateOf(false) }
    val showDeleteConfirm = remember { mutableStateOf(false) }

    val isLightCard = ColorUtils.calculateLuminance(cardColor.toArgb()) > 0.5
    val contentColor = if (isLightCard) Color.Black else Color.White

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = client?.name ?: stringResource(R.string.client_detail_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.clients_field_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.clients_field_email)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.clients_field_phone)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.client_detail_projects_header),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showAddProjectDialog.value = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.client_detail_add_project))
                    }
                }
            }

            // Use the same ProjectItem from ProjectsScreen for full edit/remove functionality
            items(projects) { project ->
                ProjectItem(
                    project = project,
                    clientViewModel = clientViewModel,
                    taskViewModel = taskViewModel,
                    cardColor = cardColor,
                    contentColor = contentColor,
                    themeName = themeName
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        client?.let {
                            clientViewModel.updateClient(it.copy(name = name, email = email, phone = phone))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank()
                ) {
                    Text(stringResource(R.string.client_detail_save), fontWeight = FontWeight.Bold)
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showDeleteConfirm.value = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(stringResource(R.string.client_detail_delete), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAddProjectDialog.value) {
        AddProjectDialog(
            onDismiss = { showAddProjectDialog.value = false },
            onConfirm = { projectName ->
                clientViewModel.addProject(ProjectEntity(clientId = clientId, name = projectName, description = ""))
                showAddProjectDialog.value = false
            }
        )
    }

    if (showDeleteConfirm.value) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm.value = false },
            title = { Text(stringResource(R.string.client_detail_delete), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.client_detail_delete_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    client?.let {
                        clientViewModel.deleteClient(it)
                        onBack()
                    }
                    showDeleteConfirm.value = false
                }) {
                    Text(stringResource(R.string.task_action_yes), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm.value = false }) {
                    Text(stringResource(R.string.task_action_no), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun AddProjectDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.client_detail_add_project), fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.client_detail_project_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.task_action_ok), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.task_action_cancel), fontWeight = FontWeight.Bold)
            }
        }
    )
}