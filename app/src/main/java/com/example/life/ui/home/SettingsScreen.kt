package com.example.life.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.life.data.local.SettingsManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settingsManager: SettingsManager,
    onRemindersToggled: (Boolean) -> Unit,
    isBiometricAvailable: Boolean,
    isBiometricEnabled: Boolean,
    onToggleBiometrics: (Boolean) -> Unit,
    onLockClick: () -> Unit,
    onResetAccount: () -> Unit,
    onAutoLockTimeoutChange: (Int) -> Unit
) {
    var remindersEnabled by remember { mutableStateOf(settingsManager.remindersEnabled) }
    var newMoonDate by remember { mutableLongStateOf(settingsManager.newMoonReferenceDate) }
    var fullMoonDate by remember { mutableLongStateOf(settingsManager.fullMoonReferenceDate) }
    var autoLockTimeout by remember { mutableIntStateOf(settingsManager.autoLockTimeoutSeconds) }

    var showNewMoonPicker by remember { mutableStateOf(false) }
    var showFullMoonPicker by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Security Section
            item {
                SettingsHeader("Security")
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (isBiometricAvailable) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Biometric Authentication",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Unlock with fingerprint or face ID",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = isBiometricEnabled,
                                    onCheckedChange = onToggleBiometrics
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTimeoutDialog = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Auto-lock Timeout",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    )
                                    val timeoutLabel = when (autoLockTimeout) {
                                        0 -> "Immediately"
                                        60 -> "1 minute"
                                        300 -> "5 minutes"
                                        else -> "$autoLockTimeout seconds"
                                    }
                                    Text(
                                        text = "Lock after $timeoutLabel of inactivity",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        }

                        OutlinedButton(
                            onClick = onLockClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Lock App Now")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reset Local Account",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Notifications Section
            item {
                SettingsHeader("Notifications")
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Check-in Reminders",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Receive a notification every 2 hours to check in.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = {
                            remindersEnabled = it
                            onRemindersToggled(it)
                        }
                    )
                }
            }

            // Moon Phase Section
            item {
                SettingsHeader("Moon Phase Setup")
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Enter known moon dates to enable offline tracking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    MoonDateItem(
                        label = "Known New Moon",
                        date = if (newMoonDate > 0) dateFormatter.format(Date(newMoonDate)) else "Not set",
                        onClick = { showNewMoonPicker = true }
                    )

                    MoonDateItem(
                        label = "Known Full Moon",
                        date = if (fullMoonDate > 0) dateFormatter.format(Date(fullMoonDate)) else "Not set",
                        onClick = { showFullMoonPicker = true }
                    )
                }
            }
        }
    }

    if (showTimeoutDialog) {
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            title = { Text("Auto-lock Timeout") },
            text = {
                Column {
                    listOf(0, 60, 300).forEach { timeout ->
                        val label = when (timeout) {
                            0 -> "Immediately"
                            60 -> "1 minute"
                            300 -> "5 minutes"
                            else -> "$timeout seconds"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    autoLockTimeout = timeout
                                    onAutoLockTimeoutChange(timeout)
                                    showTimeoutDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = autoLockTimeout == timeout,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimeoutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Local Account?") },
            text = {
                Text("This will delete your local account credentials and security setup from this device. Are you sure?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNewMoonPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = if (newMoonDate > 0) newMoonDate else System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showNewMoonPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        newMoonDate = it
                        settingsManager.newMoonReferenceDate = it
                    }
                    showNewMoonPicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showNewMoonPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showFullMoonPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = if (fullMoonDate > 0) fullMoonDate else System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showFullMoonPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        fullMoonDate = it
                        settingsManager.fullMoonReferenceDate = it
                    }
                    showFullMoonPicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showFullMoonPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun MoonDateItem(label: String, date: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = date, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
