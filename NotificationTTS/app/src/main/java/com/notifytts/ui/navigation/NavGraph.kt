package com.notifytts.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : Screen(
        route = "dashboard",
        title = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    )
    data object AppFilter : Screen(
        route = "app_filter",
        title = "Apps",
        selectedIcon = Icons.Filled.Apps,
        unselectedIcon = Icons.Outlined.Apps
    )
    data object Rules : Screen(
        route = "rules",
        title = "Rules",
        selectedIcon = Icons.Filled.FilterAlt,
        unselectedIcon = Icons.Outlined.FilterAlt
    )
    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
    data object NotificationLog : Screen(
        route = "log",
        title = "Log",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    )
    data object TTSSettings : Screen(
        route = "tts_settings",
        title = "TTS Settings",
        selectedIcon = Icons.Filled.RecordVoiceOver,
        unselectedIcon = Icons.Outlined.RecordVoiceOver
    )
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.AppFilter,
    Screen.Rules,
    Screen.Settings,
    Screen.NotificationLog
)
