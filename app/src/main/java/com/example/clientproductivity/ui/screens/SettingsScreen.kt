package com.example.clientproductivity.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.clientproductivity.R
import com.example.clientproductivity.ui.theme.AppThemes
import com.example.clientproductivity.viewmodel.BackupViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    backupViewModel: BackupViewModel,
    remindersEnabled: Boolean,
    onRemindersToggle: (Boolean) -> Unit,
    selectedThemeId: Int,
    onThemeSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    val showImportWarning = remember { mutableStateOf(false) }
    val pendingImportData = remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            backupViewModel.exportData { json ->
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(json.toByteArray())
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val json = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }
            if (json != null) {
                pendingImportData.value = json
                showImportWarning.value = true
            }
        }
    }

    LaunchedEffect(Unit) {
        backupViewModel.events.collectLatest { event ->
            when (event) {
                is BackupViewModel.BackupEvent.Success -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is BackupViewModel.BackupEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                }
            ) 
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Notifications Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Task Reminders", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("Notify when tasks are due in 24h", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(checked = remindersEnabled, onCheckedChange = onRemindersToggle)
                }
            }

            HorizontalDivider()

            // Theme Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "App Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(AppThemes) { theme ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onThemeSelected(theme.id) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(theme.backgroundColor)
                                    .border(
                                        width = if (selectedThemeId == theme.id) 3.dp else 1.dp,
                                        color = if (selectedThemeId == theme.id) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(theme.primaryColor)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = theme.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            HorizontalDivider()

            // Backup Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = stringResource(R.string.settings_backup_header), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = { exportLauncher.launch("productivity_backup.json") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_export_button), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { importLauncher.launch("application/json") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_import_button), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showImportWarning.value) {
        AlertDialog(
            onDismissRequest = { showImportWarning.value = false },
            title = { Text("Import Data", fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.settings_import_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportData.value?.let { backupViewModel.importData(it) }
                    showImportWarning.value = false
                }) {
                    Text(stringResource(R.string.task_action_yes), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportWarning.value = false }) {
                    Text(stringResource(R.string.task_action_no), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
