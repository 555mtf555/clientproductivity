package com.example.clientproductivity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.clientproductivity.data.prefs.PreferenceManager
import com.example.clientproductivity.data.worker.TaskReminderWorker
import com.example.clientproductivity.ui.navigation.AppScaffold
import com.example.clientproductivity.viewmodel.ClientViewModel
import com.example.clientproductivity.viewmodel.TaskViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scheduleTaskReminders()

        setContent {
            val context = LocalContext.current
            val taskViewModel: TaskViewModel = hiltViewModel()
            val clientViewModel: ClientViewModel = hiltViewModel()

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { _ -> }
            )

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            AppScaffold(
                taskViewModel = taskViewModel,
                clientViewModel = clientViewModel,
                preferenceManager = preferenceManager
            )
        }
    }

    private fun scheduleTaskReminders() {
        // WorkManager is auto-initialized via ClientProductivityApp.workManagerConfiguration
        // KEEP policy means the schedule survives app restarts without creating duplicates
        val workRequest = PeriodicWorkRequestBuilder<TaskReminderWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "task_reminders",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}