package com.example.life.ui.checkin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.life.data.model.Mood

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    onBack: () -> Unit,
    onSave: (situation: String, mood: Mood, reason: String?, nextAction: String) -> Unit,
) {
    var situation by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf<Mood?>(null) }
    var reason by remember { mutableStateOf("") }
    var nextAction by remember { mutableStateOf("") }

    val quickActivities = listOf("Work", "Study", "Relax", "Exercise", "Eating", "Commuting")
    val quickNextActions = listOf("Work", "Study", "Relax", "Exercise", "Sleep", "Eat", "Socialize")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Check-in", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.ime
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // 1. What's happening right now?
            CheckInSection(title = "What's happening right now?") {
                OutlinedTextField(
                    value = situation,
                    onValueChange = { situation = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Currently I am...") },
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                QuickChoicesRow(choices = quickActivities) { situation = it }
            }

            // 2. How are you feeling?
            CheckInSection(title = "How are you feeling?") {
                MoodSelector(
                    selectedMood = selectedMood,
                    onMoodSelected = { selectedMood = it }
                )
            }

            // 3. Why are you feeling this way? (Optional)
            CheckInSection(title = "Why are you feeling this way? (Optional)") {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("A brief reason...") },
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // 4. What will you do next?
            CheckInSection(title = "What will you do next?") {
                OutlinedTextField(
                    value = nextAction,
                    onValueChange = { nextAction = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("I will go to...") },
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                QuickChoicesRow(choices = quickNextActions) { nextAction = it }
            }

            // Save Button
            Button(
                onClick = {
                    selectedMood?.let {
                        onSave(situation, it, reason, nextAction)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = (situation.isNotBlank() && selectedMood != null && nextAction.isNotBlank()),
            ) {
                Text("Save Check-in", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CheckInSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        content()
    }
}

@Composable
fun QuickChoicesRow(
    choices: List<String>,
    onChoiceSelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(choices) { choice ->
            SuggestionChip(
                onClick = { onChoiceSelected(choice) },
                label = { Text(choice) },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun MoodSelector(
    selectedMood: Mood?,
    onMoodSelected: (Mood) -> Unit
) {
    val moods = Mood.entries
    
    // We can use a grid for moods
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        moods.chunked(3).forEach { rowMoods ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowMoods.forEach { mood ->
                    MoodItem(
                        mood = mood,
                        isSelected = selectedMood == mood,
                        modifier = Modifier.weight(1f),
                        onClick = { onMoodSelected(mood) }
                    )
                }
                // Fill empty slots if last row has less than 3
                if (rowMoods.size < 3) {
                    repeat(3 - rowMoods.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun MoodItem(
    mood: Mood,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        onClick = onClick,
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = mood.emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = mood.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
