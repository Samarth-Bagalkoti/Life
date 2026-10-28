package com.example.life.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.life.data.local.JournalEntry
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JournalScreen(
    viewModel: JournalViewModel = viewModel()
) {
    val entries by viewModel.allEntries.collectAsState()
    var editingEntry by remember { mutableStateOf<JournalEntry?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }
    var unlockedEntryId by remember { mutableLongStateOf(-1L) }
    var decryptionKeyForEntry by remember { mutableStateOf("") }
    
    var showKeyDialogForId by remember { mutableLongStateOf(-1L) }
    var keyInput by remember { mutableStateOf("") }
    var keyError by remember { mutableStateOf(false) }

    val todayStr = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date()) }
    val entryToday = entries.find { it.title == todayStr }

    if (isAddingNew || (editingEntry != null && unlockedEntryId == editingEntry?.id)) {
        val entry = editingEntry
        val currentKey = decryptionKeyForEntry
        
        WritingScreen(
            initialTitle = entry?.title ?: todayStr,
            initialContent = if (entry != null) viewModel.decryptContent(entry.encryptedContent, currentKey) else "",
            titleEnabled = false,
            onBack = { 
                isAddingNew = false
                editingEntry = null
                unlockedEntryId = -1L
                decryptionKeyForEntry = ""
            },
            onSave = { title, content ->
                if (entry == null) {
                    // New entry today
                    viewModel.addEntry(title, content, currentKey)
                } else {
                    viewModel.updateEntry(entry.id, title, content, currentKey)
                }
                isAddingNew = false
                editingEntry = null
                unlockedEntryId = -1L
                decryptionKeyForEntry = ""
            },
            onDelete = entry?.let { e -> {
                viewModel.deleteEntry(e.id)
                editingEntry = null
                unlockedEntryId = -1L
                decryptionKeyForEntry = ""
            }},
            onKeyChange = entry?.let { e -> { newKey, currentTitle, currentContent ->
                viewModel.updateEntry(e.id, currentTitle, currentContent, newKey)
                decryptionKeyForEntry = newKey
            }}
        )
    } else {
        Scaffold(
            floatingActionButton = {
                if (entryToday == null) {
                    FloatingActionButton(
                        onClick = { 
                            showKeyDialogForId = 0L // 0 means new
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Entry")
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Daily Diary",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 24.dp)
                )

                if (entries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No diary entries yet. Start today.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(entries) { entry ->
                            JournalTitleItem(
                                entry = entry,
                                onClick = { 
                                    showKeyDialogForId = entry.id
                                    keyInput = ""
                                    keyError = false
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showKeyDialogForId != -1L) {
        val entry = entries.find { it.id == showKeyDialogForId }
        AlertDialog(
            onDismissRequest = { showKeyDialogForId = -1L },
            title = { Text(if (showKeyDialogForId == 0L) "Set Encryption Key" else "Enter Encryption Key") },
            text = {
                Column {
                    Text(
                        text = if (showKeyDialogForId == 0L) 
                            "Set a key to encrypt today's diary entry. You will need this key to read it later." 
                            else "This entry is encrypted. Please enter the key to unlock it.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { 
                            keyInput = it
                            keyError = false
                        },
                        label = { Text("Encryption Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = keyError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (keyError) {
                        Text(
                            text = "Incorrect key.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (showKeyDialogForId == 0L) {
                            if (keyInput.isNotBlank()) {
                                decryptionKeyForEntry = keyInput
                                isAddingNew = true
                                showKeyDialogForId = -1L
                            }
                        } else {
                            if (entry != null && viewModel.verifyKey(entry.verificationString, keyInput)) {
                                decryptionKeyForEntry = keyInput
                                unlockedEntryId = entry.id
                                editingEntry = entry
                                showKeyDialogForId = -1L
                            } else {
                                keyError = true
                            }
                        }
                    }
                ) {
                    Text(if (showKeyDialogForId == 0L) "Set & Write" else "Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showKeyDialogForId = -1L }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun JournalTitleItem(
    entry: JournalEntry,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Encrypted Daily Entry",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.Lock,
                contentDescription = "Encrypted",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
