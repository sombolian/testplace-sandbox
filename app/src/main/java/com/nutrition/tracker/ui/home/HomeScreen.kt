package com.nutrition.tracker.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nutrition.tracker.data.model.DayRating
import com.nutrition.tracker.data.model.Meal
import com.nutrition.tracker.ui.components.AnimatedCircularProgress
import com.nutrition.tracker.ui.components.MacroProgressBar
import com.nutrition.tracker.ui.theme.*
import com.nutrition.tracker.util.DateUtils
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddMeal: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedDateStr by viewModel.selectedDateStr.collectAsState()
    val meals by viewModel.meals.collectAsState()
    val summary by viewModel.daySummary.collectAsState()
    val targetCalories by viewModel.targetCalories.collectAsState()
    val targetProtein by viewModel.targetProtein.collectAsState()
    val targetCarbs by viewModel.targetCarbs.collectAsState()
    val targetFat by viewModel.targetFat.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val dayRating by viewModel.dayRating.collectAsState()
    val aiAdvice by viewModel.aiAdvice.collectAsState()
    val isLoadingAdvice by viewModel.isLoadingAdvice.collectAsState()
    val today = DateUtils.getNutritionDayDate()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddMeal,
                icon = { Icon(Icons.Default.Add, "Add meal") },
                text = { Text("Add Meal") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header with gradient
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .statusBarsPadding()
                ) {
                    Column {
                        // Top bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (userName.isNotBlank()) "Hey, $userName" else "Hello!",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Let's track your nutrition",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(onClick = onOpenCalendar) {
                                    Icon(Icons.Default.CalendarMonth, "Calendar")
                                }
                                IconButton(onClick = onOpenSettings) {
                                    Icon(Icons.Default.Settings, "Settings")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Date navigation
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.navigateDay(-1) }) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous day")
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = DateUtils.formatForDisplay(selectedDateStr),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.navigateDay(1) },
                                    enabled = selectedDate < today
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next day")
                                }
                            }
                        }

                        if (selectedDate != today) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { viewModel.goToToday() },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Go to Today")
                            }
                        }
                    }
                }
            }

            // Calories & Protein main display
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Rating badge
                        AnimatedVisibility(
                            visible = dayRating != DayRating.NO_DATA,
                            enter = scaleIn() + fadeIn(),
                        ) {
                            Surface(
                                color = Color(dayRating.color).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = "${dayRating.emoji} ${dayRating.label}",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(dayRating.color)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            AnimatedCircularProgress(
                                current = (summary?.totalCalories ?: 0).toFloat(),
                                target = targetCalories.toFloat(),
                                color = CaloriesColor,
                                label = "kcal",
                                size = 130.dp,
                                strokeWidth = 14.dp
                            )

                            AnimatedCircularProgress(
                                current = (summary?.totalProtein ?: 0.0).toFloat(),
                                target = targetProtein.toFloat(),
                                color = ProteinColor,
                                label = "protein",
                                unit = "g",
                                size = 130.dp,
                                strokeWidth = 14.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Macro bars
                        MacroProgressBar(
                            label = "Carbs",
                            current = (summary?.totalCarbs ?: 0.0).toFloat(),
                            target = targetCarbs.toFloat(),
                            color = CarbsColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        MacroProgressBar(
                            label = "Fat",
                            current = (summary?.totalFat ?: 0.0).toFloat(),
                            target = targetFat.toFloat(),
                            color = FatColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // AI Advice card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AI Coach",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { viewModel.fetchAiAdvice() },
                                enabled = !isLoadingAdvice
                            ) {
                                if (isLoadingAdvice) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(if (aiAdvice != null) "Refresh" else "Get Advice")
                                }
                            }
                        }

                        if (aiAdvice != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = aiAdvice!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Meals list header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Meals (${meals.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (meals.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.RestaurantMenu,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No meals logged yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Tap + to add your first meal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            items(meals, key = { it.id }) { meal ->
                MealCard(
                    meal = meal,
                    onDelete = { viewModel.deleteMeal(meal) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .animateItem()
                )
            }
        }
    }
}

@Composable
private fun MealCard(
    meal: Meal,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Meal") },
            text = { Text("Are you sure you want to delete this meal?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(CaloriesColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MacroChip("${meal.calories} kcal", CaloriesColor)
                    MacroChip("${meal.protein.toInt()}g P", ProteinColor)
                    MacroChip("${meal.carbs.toInt()}g C", CarbsColor)
                    MacroChip("${meal.fat.toInt()}g F", FatColor)
                }
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Delete",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun MacroChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}
