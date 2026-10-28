package com.example.life

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.life.data.local.SettingsManager
import com.example.life.ui.checkin.CheckInScreen
import com.example.life.ui.checkin.CheckInViewModel
import com.example.life.ui.home.HomeScreen
import com.example.life.ui.home.InsightsScreen
import com.example.life.ui.home.JournalScreen
import com.example.life.ui.home.MoonScreen
import com.example.life.ui.home.NotesScreen
import com.example.life.ui.home.ScreenTimeScreen
import com.example.life.ui.home.SettingsScreen
import com.example.life.ui.home.TasksScreen
import com.example.life.ui.navigation.BottomNavItems
import com.example.life.ui.navigation.Screen
import com.example.life.ui.placeholder.PlaceholderScreen
import com.example.life.ui.theme.LifeTheme
import com.example.life.worker.ReminderScheduler
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import java.util.Calendar

import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.life.security.AuthState
import com.example.life.security.InactivityLockManager
import com.example.life.ui.auth.AuthLoginScreen
import com.example.life.ui.auth.AuthSetupScreen
import com.example.life.ui.auth.AuthViewModel

class MainActivity : FragmentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var inactivityLockManager: InactivityLockManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val settingsManager = SettingsManager(this)
            if (settingsManager.remindersEnabled) {
                ReminderScheduler.scheduleReminders(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val settingsManager = SettingsManager(this)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        inactivityLockManager = InactivityLockManager(
            onLockRequired = {
                authViewModel.lock()
            }
        ).apply {
            lockTimeoutMillis = settingsManager.autoLockTimeoutSeconds * 1000L
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(inactivityLockManager)

        if (settingsManager.remindersEnabled) {
            checkAndRequestNotificationPermission()
            ReminderScheduler.scheduleReminders(this)
        }

        setContent {
            LifeTheme {
                val authState by authViewModel.authState.collectAsState()
                val errorMessage by authViewModel.errorMessage.collectAsState()
                val navigateTo = intent.getStringExtra("navigate_to")

                when (authState) {
                    is AuthState.Unregistered -> {
                        AuthSetupScreen(
                            errorMessage = errorMessage,
                            isBiometricAvailable = authViewModel.isBiometricAvailable,
                            onRegister = { username, pin, confirmPin, enableBiometrics ->
                                authViewModel.register(username, pin, confirmPin, enableBiometrics)
                            },
                            onClearError = { authViewModel.clearError() }
                        )
                    }

                    is AuthState.Locked -> {
                        AuthLoginScreen(
                            username = authViewModel.username,
                            errorMessage = errorMessage,
                            isBiometricAvailable = authViewModel.isBiometricAvailable,
                            isBiometricEnabled = authViewModel.isBiometricEnabled,
                            onLogin = { pinOrPassword ->
                                authViewModel.login(pinOrPassword)
                            },
                            onBiometricClick = {
                                authViewModel.triggerBiometricLogin(this@MainActivity)
                            },
                            onClearError = { authViewModel.clearError() }
                        )
                    }

                    is AuthState.Authenticated -> {
                        MainScreen(
                            settingsManager = settingsManager,
                            navigateTo = navigateTo,
                            authViewModel = authViewModel,
                            inactivityLockManager = inactivityLockManager
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::inactivityLockManager.isInitialized) {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(inactivityLockManager)
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun MainScreen(
    settingsManager: SettingsManager,
    navigateTo: String?,
    authViewModel: AuthViewModel,
    inactivityLockManager: InactivityLockManager
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val checkInViewModel: CheckInViewModel = viewModel()
    val checkIns by checkInViewModel.checkIns.collectAsState()

    LaunchedEffect(navigateTo) {
        if (navigateTo == "check_in") {
            navController.navigate(Screen.CheckIn.route)
        }
    }
    
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    
    val todayCheckIns = checkIns.filter { it.timestamp.time >= today }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val mainRoutes = BottomNavItems.map { it.route }
            val showBottomBar = currentDestination?.route in mainRoutes
            
            if (showBottomBar) {
                NavigationBar {
                    BottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { 
                                screen.icon?.let { 
                                    Icon(imageVector = it, contentDescription = screen.title) 
                                } 
                            },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { 
                HomeScreen(
                    latestCheckIn = checkIns.firstOrNull(),
                    totalCheckInsToday = todayCheckIns.size,
                    onNavigateToCheckIn = { navController.navigate(Screen.CheckIn.route) },
                    onNavigateToScreenTime = { navController.navigate(Screen.ScreenTime.route) }
                ) 
            }
            composable(Screen.CheckIn.route) {
                CheckInScreen(
                    onBack = { navController.popBackStack() },
                    onSave = { situation, mood, reason, nextAction ->
                        checkInViewModel.addCheckIn(situation, mood, reason, nextAction)
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Journal.route) { JournalScreen() }
            composable(Screen.Tasks.route) { TasksScreen() }
            composable(Screen.Insights.route) { InsightsScreen() }
            composable(Screen.More.route) { MoreScreen(navController) }
            
            composable(Screen.Settings.route) { 
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    settingsManager = settingsManager,
                    onRemindersToggled = { enabled ->
                        settingsManager.remindersEnabled = enabled
                        if (enabled) {
                            ReminderScheduler.scheduleReminders(navController.context)
                        } else {
                            ReminderScheduler.cancelReminders(navController.context)
                        }
                    },
                    isBiometricAvailable = authViewModel.isBiometricAvailable,
                    isBiometricEnabled = authViewModel.isBiometricEnabled,
                    onToggleBiometrics = { enabled ->
                        authViewModel.toggleBiometricSetting(enabled)
                    },
                    onLockClick = {
                        authViewModel.lock()
                    },
                    onResetAccount = {
                        authViewModel.resetAccount()
                    },
                    onAutoLockTimeoutChange = { seconds ->
                        settingsManager.autoLockTimeoutSeconds = seconds
                        inactivityLockManager.lockTimeoutMillis = seconds * 1000L
                    }
                ) 
            }
            composable(Screen.Notes.route) { NotesScreen() }
            composable(Screen.Moon.route) { 
                MoonScreen(
                    onBack = { navController.popBackStack() },
                    settingsManager = settingsManager
                ) 
            }
            composable(Screen.ScreenTime.route) { 
                ScreenTimeScreen(onBack = { navController.popBackStack() }) 
            }
        }
    }
}

@Composable
fun MoreScreen(navController: NavController) {
    val moreItems = listOf(
        Screen.Notes,
        Screen.Moon,
        Screen.ScreenTime,
        Screen.Settings
    )
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "More",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        items(moreItems) { screen ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                onClick = { navController.navigate(screen.route) }
            ) {
                Text(
                    text = screen.title,
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
