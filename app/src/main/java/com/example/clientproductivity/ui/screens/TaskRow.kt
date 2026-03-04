package com.example.clientproductivity.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.clientproductivity.R
import com.example.clientproductivity.data.entity.ActivityLogEntity
import com.example.clientproductivity.data.entity.TaskWithContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun TaskRow(
    task: TaskWithContext,
    logs: List<ActivityLogEntity> = emptyList(),
    highlightColor: Color? = null,
    onToggleComplete: () -> Unit,
    onToggleOverdue: () -> Unit,
    onTaskRemove: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    onExpand: () -> Unit = {},
    onClick: (() -> Unit)? = null,
    themeName: String = ""
) {
    val showDeleteConfirm = remember { mutableStateOf(false) }
    val expanded = remember { mutableStateOf(false) }

    val completedColor = if (themeName == "Gilded Emerald") {
        Color(
            androidx.core.graphics.ColorUtils.blendARGB(
                MaterialTheme.colorScheme.surfaceVariant.toArgb(),
                0xFF42A5F5.toInt(), // Material Blue 400 — vivid, stands out on dark green
                0.55f
            )
        )
    } else {
        Color(
            androidx.core.graphics.ColorUtils.blendARGB(
                MaterialTheme.colorScheme.surfaceVariant.toArgb(),
                0xFF2E7D32.toInt(), // Material Green 800 — rich, saturated green
                0.45f
            )
        )
    }

    val cardColor = highlightColor ?: when {
        task.blocked -> MaterialTheme.colorScheme.errorContainer
        task.completed -> completedColor
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${task.clientName} • ${task.projectName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    val timeText = remember(task.dueDate) {
                        task.dueDate.atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.getDefault()))
                    }
                    Text(
                        text = "Due $timeText",
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            task.completed -> Color(0xFF4CAF50).copy(alpha = 0.8f)
                            task.blocked -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.isRemoved) {
                        IconButton(onClick = { onRestore?.invoke() }) {
                            Icon(Icons.Default.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showDeleteConfirm.value = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Permanently", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Checkbox(
                            checked = task.completed,
                            onCheckedChange = { onToggleComplete() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF4CAF50),
                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                        IconButton(onClick = { showDeleteConfirm.value = true }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            if (task.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = task.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded.value) Int.MAX_VALUE else 2
                )
            }

            if (!task.isRemoved) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!task.completed) {
                        AssistChip(
                            onClick = onToggleOverdue,
                            label = {
                                Text(
                                    if (task.blocked) stringResource(R.string.task_menu_clear_overdue)
                                    else stringResource(R.string.task_menu_mark_overdue)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = if (task.blocked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    AssistChip(
                        onClick = {
                            expanded.value = !expanded.value
                            if (expanded.value) onExpand()
                        },
                        label = { Text("History") },
                        leadingIcon = { Icon(Icons.Default.History, null, Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            } else {
                task.removedAt?.let {
                    val daysLeft = 7 - ChronoUnit.DAYS.between(it, Instant.now())
                    Text(
                        text = "Auto-deletes in $daysLeft days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded.value) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Activity History",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (logs.isEmpty()) {
                        Text(
                            text = "No activity recorded yet.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    } else {
                        logs.forEach { log -> ActivityLogRow(log) }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm.value) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm.value = false },
            title = { Text(stringResource(R.string.task_action_delete_title)) },
            text = { Text(stringResource(R.string.task_action_delete_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    onTaskRemove?.invoke()
                    showDeleteConfirm.value = false
                }) { Text(stringResource(R.string.task_action_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm.value = false }) {
                    Text(stringResource(R.string.task_action_no))
                }
            }
        )
    }
}

@Composable
fun ActivityLogRow(log: ActivityLogEntity) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = log.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatter.format(log.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}