package com.nutrition.tracker.ui.calendar

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nutrition.tracker.data.model.DayRating
import com.nutrition.tracker.util.DateUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    onDaySelected: (String) -> Unit,
    viewModel: CalendarViewModel = viewModel()
) {
    val currentMonth by viewModel.currentMonth.collectAsState()
    val daySummaries by viewModel.daySummaries.collectAsState()
    val today = DateUtils.getNutritionDayDate()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Month navigation
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month")
                    }
                    Text(
                        text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Day of week headers
            Row(modifier = Modifier.fillMaxWidth()) {
                val daysOfWeek = listOf(
                    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
                    DayOfWeek.SATURDAY
                )
                daysOfWeek.forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar grid
            val firstDay = currentMonth.atDay(1)
            val firstDayOfWeek = firstDay.dayOfWeek.value % 7 // Sunday = 0
            val daysInMonth = currentMonth.lengthOfMonth()

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Empty cells before first day
                items(firstDayOfWeek) {
                    Spacer(modifier = Modifier.aspectRatio(1f))
                }

                // Day cells
                items(daysInMonth) { index ->
                    val day = index + 1
                    val date = currentMonth.atDay(day)
                    val dateStr = DateUtils.formatDate(date)
                    val rating = viewModel.getRatingForDay(dateStr)
                    val isToday = date == today
                    val isFuture = date.isAfter(today)
                    val summary = daySummaries[dateStr]

                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isFuture) {
                                onDaySelected(dateStr)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isToday -> MaterialTheme.colorScheme.primaryContainer
                                isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                rating != DayRating.NO_DATA -> Color(rating.color).copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        ),
                        elevation = CardDefaults.cardElevation(if (isToday) 2.dp else 0.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$day",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isFuture) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.onSurface
                                )

                                if (rating != DayRating.NO_DATA && !isFuture) {
                                    Text(
                                        text = rating.emoji,
                                        fontSize = 10.sp
                                    )
                                }

                                if (summary != null) {
                                    Text(
                                        text = "${summary.totalCalories}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Legend
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Legend",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val ratings = DayRating.entries.filter { it != DayRating.NO_DATA }
                    ratings.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            row.forEach { rating ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(rating.color))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "${rating.emoji} ${rating.label}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
