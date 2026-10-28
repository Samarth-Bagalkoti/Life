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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.life.data.local.NoteEntity

@Composable
fun NotesScreen(
    viewModel: NotesViewModel = viewModel()
) {
    val notes by viewModel.allNotes.collectAsState()
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }
    var unlockedNoteId by remember { mutableLongStateOf(-1L) }
    var decryptionKeyForNote by remember { mutableStateOf("") }
    
    var showKeyDialogForId by remember { mutableLongStateOf(-1L) }
    var keyInput by remember { mutableStateOf("") }
    var keyError by remember { mutableStateOf(false) }

    if (isAddingNew || (editingNote != null && unlockedNoteId == editingNote?.id)) {
        val note = editingNote
        val currentKey = decryptionKeyForNote
        
        WritingScreen(
            initialTitle = note?.title ?: "",
            initialContent = if (note != null) viewModel.decryptContent(note.encryptedContent, currentKey) else "",
            onBack = { 
                isAddingNew = false
                editingNote = null
                unlockedNoteId = -1L
                decryptionKeyForNote = ""
            },
            onSave = { title, content ->
                if (note == null) {
                    viewModel.addNote(title, content, currentKey)
                } else {
                    viewModel.updateNote(note.id, title, content, currentKey)
                }
                isAddingNew = false
                editingNote = null
                unlockedNoteId = -1L
                decryptionKeyForNote = ""
            },
            onDelete = note?.let { n -> {
                viewModel.deleteNote(n)
                editingNote = null
                unlockedNoteId = -1L
                decryptionKeyForNote = ""
            }},
            onKeyChange = note?.let { n -> { newKey, currentTitle, currentContent ->
                viewModel.updateNote(n.id, currentTitle, currentContent, newKey)
                decryptionKeyForNote = newKey
            }}
        )
    } else {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { 
                        showKeyDialogForId = 0L 
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Note")
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
                    text = "Notes",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 24.dp)
                )

                if (notes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Your notes will appear here.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(notes) { note ->
                            NoteTitleItem(
                                note = note,
                                onClick = { 
                                    showKeyDialogForId = note.id
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
        val note = notes.find { it.id == showKeyDialogForId }
        AlertDialog(
            onDismissRequest = { showKeyDialogForId = -1L },
            title = { Text(if (showKeyDialogForId == 0L) "Set Encryption Key" else "Enter Encryption Key") },
            text = {
                Column {
                    Text(
                        text = if (showKeyDialogForId == 0L) 
                            "Set a key to encrypt this note. You will need this key to read it later." 
                            else "This note is encrypted. Please enter the key to unlock it.",
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
                                decryptionKeyForNote = keyInput
                                isAddingNew = true
                                showKeyDialogForId = -1L
                            }
                        } else {
                            if (note != null && viewModel.verifyKey(note.verificationString, keyInput)) {
                                decryptionKeyForNote = keyInput
                                unlockedNoteId = note.id
                                editingNote = note
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
fun NoteTitleItem(
    note: NoteEntity,
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
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Encrypted Note",
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
