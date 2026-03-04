package com.example.clientproductivity.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.clientproductivity.R
import com.example.clientproductivity.data.entity.TaskEntity
import com.example.clientproductivity.data.entity.TaskWithContext
import com.example.clientproductivity.data.extensions.toTaskEntity
import java.time.Instant

private val PriorityGold = Color(0xFFFFD700)
private val CompletedGreen = Color(0xFF4CAF50)
private val OverdueRed = Color(0xFFD32F2F)

@Composable
fun TaskItem(
    task: TaskWithContext,
    onUpdate: (TaskEntity) -> Unit,
    onToggleComplete: (TaskEntity) -> Unit,
    onToggleOverdue: (TaskEntity) -> Unit,
    onTaskRemove: (TaskEntity) -> Unit,
    updatePriority: (TaskEntity, Int) -> Unit,
    startOfToday: Long
) {
    val showMenu = remember { mutableStateOf(false) }
    val showRenameDialog = remember { mutableStateOf(false) }
    val showDeleteConfirm = remember { mutableStateOf(false) }
    val showNotifyDialog = remember { mutableStateOf(false) }
    val newTitle = remember { mutableStateOf(task.title) }

    val isOverdue = (task.dueDate.toEpochMilli() < startOfToday && !task.completed) || (task.blocked && !task.completed)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = "Toggle Complete",
            tint = when {
                task.completed -> CompletedGreen
                isOverdue -> OverdueRed
                else -> Color.Gray
            },
            modifier = Modifier
                .size(18.dp)
                .clickable { onToggleComplete(task.toTaskEntity()) }
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                color = when {
                    task.completed -> CompletedGreen
                    isOverdue -> OverdueRed
                    else -> MaterialTheme.colorScheme.onSurface
                },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clickable { showMenu.value = true }
            )

            DropdownMenu(
                expanded = showMenu.value,
                onDismissRequest = { showMenu.value = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.task_menu_rename)) },
                    onClick = {
                        showMenu.value = false
                        newTitle.value = task.title 
                        showRenameDialog.value = true
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.task_menu_notify)) },
                    onClick = {
                        showMenu.value = false
                        showNotifyDialog.value = true
                    }
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                
                if (task.priority < 2) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.task_menu_raise_priority)) },
                        leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                        onClick = {
                            showMenu.value = false
                            updatePriority(task.toTaskEntity(), task.priority + 1)
                        }
                    )
                }
                
                if (task.priority > 0) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.task_menu_lower_priority)) },
                        leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) },
                        onClick = {
                            showMenu.value = false
                            updatePriority(task.toTaskEntity(), task.priority - 1)
                        }
                    )
                }

                DropdownMenuItem(
                    text = { 
                        Text(
                            if (task.blocked) stringResource(R.string.task_menu_clear_overdue) 
                            else stringResource(R.string.task_menu_mark_overdue)
                        ) 
                    },
                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                    onClick = {
                        showMenu.value = false
                        onToggleOverdue(task.toTaskEntity())
                    }
                )
            }
        }

        if (task.priority != 1) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (task.priority == 2) Icons.Default.Star else Icons.Default.Schedule,
                contentDescription = null,
                tint = if (task.priority == 2) PriorityGold else Color.Gray,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove Task",
            tint = Color.Gray.copy(alpha = 0.5f),
            modifier = Modifier
                .size(16.dp)
                .clickable { showDeleteConfirm.value = true }
        )
    }

    if (showRenameDialog.value) {
        AlertDialog(
            onDismissRequest = { 
                showRenameDialog.value = false 
            },
            title = { Text(stringResource(R.string.task_action_rename_title)) },
            text = {
                TextField(
                    value = newTitle.value,
                    onValueChange = { newTitle.value = it }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdate(task.toTaskEntity(title = newTitle.value))
                    showRenameDialog.value = false
                }) {
                    Text(stringResource(R.string.task_action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showRenameDialog.value = false 
                }) {
                    Text(stringResource(R.string.task_action_cancel))
                }
            }
        )
    }

    if (showDeleteConfirm.value) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirm.value = false 
            },
            title = { Text(stringResource(R.string.task_action_delete_title)) },
            text = { Text(stringResource(R.string.task_action_delete_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    onTaskRemove(task.toTaskEntity())
                    showDeleteConfirm.value = false
                }) {
                    Text(stringResource(R.string.task_action_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteConfirm.value = false 
                }) {
                    Text(stringResource(R.string.task_action_no))
                }
            }
        )
    }

    if (showNotifyDialog.value) {
        NotifyClientDialog(
            task = task,
            isOverdue = isOverdue,
            onDismiss = { showNotifyDialog.value = false }
        )
    }
}

@Composable
fun NotifyClientDialog(
    task: TaskWithContext,
    isOverdue: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedMethod by remember { mutableStateOf("Text") } // "Text" or "Email"
    
    val presetMessage = when {
        task.completed -> "Hi ${task.clientName}, the task '${task.title}' is now complete!"
        isOverdue -> "Hi ${task.clientName}, just wanted to let you know that '${task.title}' is slightly behind schedule."
        else -> "Hi ${task.clientName}, I'm currently working on '${task.title}'."
    }
    
    var customMessage by remember { mutableStateOf(presetMessage) }
    var usePreset by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notify_client_title, task.clientName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Method Selection
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(
                        selected = selectedMethod == "Text",
                        onClick = { selectedMethod = "Text" },
                        label = { Text(stringResource(R.string.notify_client_text)) },
                        leadingIcon = { Icon(Icons.Default.Sms, null, Modifier.size(18.dp)) }
                    )
                    FilterChip(
                        selected = selectedMethod == "Email",
                        onClick = { selectedMethod = "Email" },
                        label = { Text(stringResource(R.string.notify_client_email)) },
                        leadingIcon = { Icon(Icons.Default.Email, null, Modifier.size(18.dp)) }
                    )
                }

                Text(stringResource(R.string.notify_client_method), style = MaterialTheme.typography.labelLarge)
                
                OutlinedTextField(
                    value = customMessage,
                    onValueChange = { 
                        customMessage = it
                        usePreset = false
                    },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text(stringResource(R.string.notify_client_placeholder)) }
                )
                
                if (!usePreset) {
                    TextButton(onClick = { 
                        customMessage = presetMessage
                        usePreset = true
                    }) {
                        Text(stringResource(R.string.notify_client_reset))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (selectedMethod == "Text") {
                    sendSms(context, task.clientPhone, customMessage)
                } else {
                    sendEmail(context, task.clientEmail, "Update: ${task.title}", customMessage)
                }
                onDismiss()
            }) {
                Text(stringResource(R.string.notify_client_send))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.task_action_cancel))
            }
        }
    )
}

private fun sendSms(context: Context, phoneNumber: String, message: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "smsto:$phoneNumber".toUri()
        putExtra("sms_body", message)
    }
    context.startActivity(intent)
}

private fun sendEmail(context: Context, emailAddress: String, subject: String, body: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "mailto:$emailAddress".toUri()
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(intent)
}
