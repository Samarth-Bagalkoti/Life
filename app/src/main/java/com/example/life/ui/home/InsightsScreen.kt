package com.example.life.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.life.data.model.CheckIn
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = viewModel()
) {
    val dailyInsights by viewModel.dailyInsights.collectAsState()

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = "Insights",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (dailyInsights.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("Not enough data yet. Keep checking in!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                item {
                    Text(
                        text = "Mood Trend",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MoodMoonGraph(dailyInsights)
                }

                item {
                    Text(
                        text = "Daily Summary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(dailyInsights, key = { it.timestamp }) { insight ->
                    DailyInsightCard(insight)
                }
            }
        }
    }
}

@Composable
fun DailyInsightCard(insight: DailyInsight) {
    val timeFormatter = remember { 
        try {
            SimpleDateFormat("h:mm a", Locale.getDefault()) 
        } catch (e: Exception) {
            SimpleDateFormat("HH:mm", Locale.getDefault())
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = insight.dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Mainly ${insight.commonMood.label}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Mood: ${"%.1f".format(insight.averageMoodScore)} | Moon illumination: ${insight.moonIllumination}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = insight.moonPhase.icon, fontSize = 28.sp)
                }
            }
            
            if (insight.checkIns.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))
                
                insight.checkIns.forEach { checkIn ->
                    CheckInDetailItem(checkIn, timeFormatter)
                    if (checkIn != insight.checkIns.last()) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CheckInDetailItem(checkIn: CheckIn, timeFormatter: SimpleDateFormat) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = timeFormatter.format(checkIn.timestamp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = checkIn.mood.emoji, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = checkIn.mood.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Happening: ${checkIn.situation}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Next: ${checkIn.nextAction}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MoodMoonGraph(insights: List<DailyInsight>) {
    // Reverse for chronological order in graph
    val trendData = insights.take(7).reversed()
    val moodColor = MaterialTheme.colorScheme.primary
    val moonColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val width = size.width
                val height = size.height
                val spacing = width / (trendData.size.coerceAtLeast(2) - 1).coerceAtLeast(1)
                
                // Draw grid lines
                for (i in 0..4) {
                    val y = height - (i * height / 4)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                if (trendData.size >= 2) {
                    val moodPath = Path()
                    val moonPath = Path()
                    
                    trendData.filter { !it.averageMoodScore.isNaN() }.forEachIndexed { index, insight ->
                        val x = index * spacing
                        
                        // Mood Score 1-5 maps to height
                        val moodY = height - ((insight.averageMoodScore - 1) * height / 4).toFloat()
                        
                        // Moon Illumination 0-100 maps to height
                        val moonY = height - (insight.moonIllumination * height / 100)
                        
                        if (index == 0) {
                            moodPath.moveTo(x, moodY)
                            moonPath.moveTo(x, moonY)
                        } else {
                            moodPath.lineTo(x, moodY)
                            moonPath.lineTo(x, moonY)
                        }
                        
                        drawCircle(color = moodColor, radius = 4.dp.toPx(), center = Offset(x, moodY))
                    }
                    
                    drawPath(
                        path = moonPath,
                        color = moonColor,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    
                    drawPath(
                        path = moodPath,
                        color = moodColor,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // X-axis labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                trendData.forEach { insight ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = insight.moonPhase.icon, fontSize = 12.sp)
                        Text(text = "${insight.moonIllumination}%", fontSize = 10.sp, color = moonColor)
                    }
                }
            }
        }
    }
}
