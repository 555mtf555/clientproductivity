package com.example.clientproductivity.ui.navigation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.clientproductivity.R
import com.example.clientproductivity.data.prefs.PreferenceManager
import com.example.clientproductivity.ui.screens.ClientDetailScreen
import com.example.clientproductivity.ui.screens.ClientsScreen
import com.example.clientproductivity.ui.screens.DashboardScreen
import com.example.clientproductivity.ui.screens.ProjectsScreen
import com.example.clientproductivity.ui.screens.SettingsScreen
import com.example.clientproductivity.ui.screens.TaskCreationScreen
import com.example.clientproductivity.ui.screens.TaskEditScreen
import com.example.clientproductivity.ui.theme.AppThemes
import com.example.clientproductivity.ui.theme.ClientproductivityTheme
import com.example.clientproductivity.ui.theme.CustomAppTheme
import com.example.clientproductivity.viewmodel.BackupViewModel
import com.example.clientproductivity.viewmodel.ClientViewModel
import com.example.clientproductivity.viewmodel.TaskViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.ZoneId

sealed class Screen(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", R.string.nav_today, Icons.Default.DateRange)
    object Projects : Screen("projects", R.string.nav_tasks, Icons.Default.Work)
    object Clients : Screen("clients", R.string.nav_clients, Icons.Default.Person)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)

    object NewTask : Screen("new_task?projectId={projectId}", R.string.task_creation_title, Icons.Default.DateRange) {
        fun createRoute(projectId: Long? = null) =
            if (projectId != null) "new_task?projectId=$projectId" else "new_task"
    }

    object EditTask : Screen("edit_task/{taskId}", R.string.task_creation_title, Icons.Default.DateRange) {
        fun createRoute(taskId: Long) = "edit_task/$taskId"
    }

    object ClientDetail : Screen("client_detail/{clientId}", R.string.client_detail_title, Icons.Default.Person) {
        fun createRoute(clientId: Long) = "client_detail/$clientId"
    }
}

@Composable
fun AppScaffold(
    taskViewModel: TaskViewModel,
    clientViewModel: ClientViewModel,
    preferenceManager: PreferenceManager
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val scope = rememberCoroutineScope()

    // ScrollState created here so it is never recreated when DashboardScreen recomposes
    val dashboardScrollState = rememberScrollState()

    val selectedThemeId by preferenceManager.themeId.collectAsStateWithLifecycle(initialValue = 0)
    val currentTheme = remember(selectedThemeId) {
        AppThemes.find { it.id == selectedThemeId } ?: AppThemes[0]
    }

    ClientproductivityTheme(
        backgroundColor = currentTheme.backgroundColor,
        primaryColor = currentTheme.primaryColor,
        cardColor = currentTheme.cardColor,
        darkTheme = currentTheme.isDark
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = { AppBottomNavigation(navController, currentRoute) },
            floatingActionButton = {
                val showFab = currentRoute == Screen.Dashboard.route || currentRoute == Screen.Projects.route
                if (showFab) {
                    MainActionButton(
                        primaryColor = currentTheme.primaryColor,
                        onClick = { navController.navigate(Screen.NewTask.createRoute()) }
                    )
                }
            }
        ) { padding ->
            AppNavGraph(
                navController = navController,
                modifier = Modifier.padding(padding),
                taskViewModel = taskViewModel,
                clientViewModel = clientViewModel,
                preferenceManager = preferenceManager,
                currentTheme = currentTheme,
                selectedThemeId = selectedThemeId,
                dashboardScrollState = dashboardScrollState,
                scope = scope
            )
        }
    }
}

@Composable
private fun AppBottomNavigation(navController: NavController, currentRoute: String?) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        val navItems = listOf(Screen.Dashboard, Screen.Projects, Screen.Clients, Screen.Settings)

        navItems.forEach { screen ->
            val label = stringResource(screen.labelRes)
            val isSelected = currentRoute?.startsWith(screen.route) == true

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    // Pop everything above the start destination so subscreens
                    // like ClientDetail or NewTask are always dismissed
                    navController.popBackStack(
                        destinationId = navController.graph.startDestinationId,
                        inclusive = false
                    )
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                            inclusive = false
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                label = { Text(label, fontWeight = FontWeight.Bold) },
                icon = { Icon(imageVector = screen.icon, contentDescription = label) }
            )
        }
    }
}

@Composable
private fun MainActionButton(primaryColor: Color, onClick: () -> Unit) {
    val contentColor = remember(primaryColor) {
        if (ColorUtils.calculateLuminance(primaryColor.toArgb()) > 0.5) Color.Black else Color.White
    }

    FloatingActionButton(
        onClick = onClick,
        containerColor = primaryColor,
        contentColor = contentColor
    ) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.task_creation_title))
    }
}

@Composable
private fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier,
    taskViewModel: TaskViewModel,
    clientViewModel: ClientViewModel,
    preferenceManager: PreferenceManager,
    currentTheme: CustomAppTheme,
    selectedThemeId: Int,
    dashboardScrollState: ScrollState,
    scope: CoroutineScope
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                taskViewModel = taskViewModel,
                completionBoxColor = currentTheme.primaryColor,
                dayCardColor = currentTheme.cardColor,
                scrollState = dashboardScrollState,
                themeName = currentTheme.name
            )
        }

        composable(Screen.Projects.route) {
            ProjectsScreen(
                clientViewModel = clientViewModel,
                taskViewModel = taskViewModel,
                cardColor = currentTheme.cardColor,
                themeName = currentTheme.name
            )
        }

        composable(Screen.Clients.route) {
            ClientsScreen(
                clientViewModel = clientViewModel,
                onClientClick = { navController.navigate(Screen.ClientDetail.createRoute(it)) },
                cardColor = currentTheme.cardColor
            )
        }

        composable(Screen.Settings.route) {
            val backupViewModel: BackupViewModel = hiltViewModel()
            val remindersEnabled by preferenceManager.taskRemindersEnabled.collectAsStateWithLifecycle(initialValue = true)

            SettingsScreen(
                backupViewModel = backupViewModel,
                remindersEnabled = remindersEnabled,
                onRemindersToggle = { scope.launch { preferenceManager.setTaskRemindersEnabled(it) } },
                selectedThemeId = selectedThemeId,
                onThemeSelected = { scope.launch { preferenceManager.setThemeId(it) } }
            )
        }

        composable(
            route = Screen.ClientDetail.route,
            arguments = listOf(navArgument("clientId") { type = NavType.LongType })
        ) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getLong("clientId") ?: return@composable
            ClientDetailScreen(
                clientId = clientId,
                clientViewModel = clientViewModel,
                taskViewModel = taskViewModel,
                onBack = { navController.popBackStack() },
                cardColor = currentTheme.cardColor,
                themeName = currentTheme.name
            )
        }

        composable(
            route = Screen.NewTask.route,
            arguments = listOf(navArgument("projectId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId").takeIf { it != -1L }
            val initialDate = remember(taskViewModel.selectedDay) {
                taskViewModel.selectedDay.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }

            TaskCreationScreen(
                projectId = projectId,
                taskViewModel = taskViewModel,
                clientViewModel = clientViewModel,
                initialDateMillis = initialDate,
                onClose = { navController.popBackStack() },
                onTaskSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditTask.route,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: return@composable
            TaskEditScreen(
                taskId = taskId,
                taskViewModel = taskViewModel,
                onClose = { navController.popBackStack() },
                onTaskSaved = { navController.popBackStack() }
            )
        }
    }
}