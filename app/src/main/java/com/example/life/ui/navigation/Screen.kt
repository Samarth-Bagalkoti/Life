package com.example.life.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Journal : Screen("journal", "Journal", Icons.AutoMirrored.Filled.MenuBook)
    object Tasks : Screen("tasks", "Tasks", Icons.Default.TaskAlt)
    object Insights : Screen("insights", "Insights", Icons.Default.Timeline)
    object More : Screen("more", "More", Icons.Default.MoreHoriz)
    
    // Sub-screens for "More" or elsewhere
    object Notes : Screen("notes", "Notes")
    object Moon : Screen("moon", "Moon")
    object ScreenTime : Screen("screen_time", "Screen Time")
    object Settings : Screen("settings", "Settings")
    object CheckIn : Screen("check_in", "Check In")
}

val BottomNavItems = listOf(
    Screen.Home,
    Screen.Journal,
    Screen.Tasks,
    Screen.Insights,
    Screen.More,
)
