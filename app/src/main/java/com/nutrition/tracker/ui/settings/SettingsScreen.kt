package com.nutrition.tracker.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nutrition.tracker.data.preferences.UserPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val geminiModel by viewModel.geminiModel.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val heightCm by viewModel.heightCm.collectAsState()
    val weightKg by viewModel.weightKg.collectAsState()
    val age by viewModel.age.collectAsState()
    val gender by viewModel.gender.collectAsState()
    val activityLevel by viewModel.activityLevel.collectAsState()
    val fitnessGoal by viewModel.fitnessGoal.collectAsState()
    val targetCalories by viewModel.targetCalories.collectAsState()
    val targetProtein by viewModel.targetProtein.collectAsState()
    val targetCarbs by viewModel.targetCarbs.collectAsState()
    val targetFat by viewModel.targetFat.collectAsState()
    val targetFiber by viewModel.targetFiber.collectAsState()
    val mealsPerDay by viewModel.mealsPerDay.collectAsState()
    val waterGoalLiters by viewModel.waterGoalLiters.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val customInstructions by viewModel.customInstructions.collectAsState()
    val dietaryRestrictions by viewModel.dietaryRestrictions.collectAsState()
    val allergies by viewModel.allergies.collectAsState()

    var showApiKey by remember { mutableStateOf(false) }
    var showSavedConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    showSavedConfirmation = true
                    scope.launch {
                        delay(1500)
                        showSavedConfirmation = false
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    if (showSavedConfirmation) Icons.Default.Check else Icons.Default.Save,
                    contentDescription = "Save"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (showSavedConfirmation) "Saved!" else "Save",
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // AI Section
            SettingsSection(title = "AI Configuration", icon = Icons.Default.SmartToy) {
                SettingsTextField(
                    value = geminiApiKey,
                    onValueChange = { viewModel.updateString(UserPreferences.GEMINI_API_KEY, it) },
                    label = "Gemini API Key",
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility"
                            )
                        }
                    }
                )
                SettingsTextField(
                    value = geminiModel,
                    onValueChange = { viewModel.updateString(UserPreferences.GEMINI_MODEL, it) },
                    label = "Model Name",
                    supportingText = "e.g., gemini-2.0-flash, gemini-1.5-pro"
                )
            }

            // Personal Info Section
            SettingsSection(title = "Personal Information", icon = Icons.Default.Person) {
                SettingsTextField(
                    value = userName,
                    onValueChange = { viewModel.updateString(UserPreferences.USER_NAME, it) },
                    label = "Name"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingsNumberField(
                        value = age.toString(),
                        onValueChange = { it.toIntOrNull()?.let { v -> viewModel.updateInt(UserPreferences.AGE, v) } },
                        label = "Age",
                        modifier = Modifier.weight(1f)
                    )
                    SettingsNumberField(
                        value = heightCm.toString(),
                        onValueChange = { it.toIntOrNull()?.let { v -> viewModel.updateInt(UserPreferences.HEIGHT_CM, v) } },
                        label = "Height (cm)",
                        modifier = Modifier.weight(1f)
                    )
                }

                SettingsNumberField(
                    value = if (weightKg == weightKg.toInt().toDouble()) weightKg.toInt().toString() else weightKg.toString(),
                    onValueChange = { it.toDoubleOrNull()?.let { v -> viewModel.updateDouble(UserPreferences.WEIGHT_KG, v) } },
                    label = "Weight (kg)",
                    decimal = true
                )

                // Gender
                Text("Gender", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("male" to "Male", "female" to "Female").forEach { (value, label) ->
                        FilterChip(
                            selected = gender == value,
                            onClick = { viewModel.updateString(UserPreferences.GENDER, value) },
                            label = { Text(label) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Activity Level
                Text("Activity Level", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                val activities = listOf("sedentary" to "Sedentary", "light" to "Light", "moderate" to "Moderate", "active" to "Active", "very_active" to "Very Active")
                Column {
                    activities.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { (value, label) ->
                                FilterChip(
                                    selected = activityLevel == value,
                                    onClick = { viewModel.updateString(UserPreferences.ACTIVITY_LEVEL, value) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }

                // Fitness Goal
                Text("Fitness Goal", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                val goals = listOf("tone" to "Toning/חיטוב", "bulk" to "Bulk", "cut" to "Cut", "maintain" to "Maintain", "recomp" to "Recomp")
                Column {
                    goals.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { (value, label) ->
                                FilterChip(
                                    selected = fitnessGoal == value,
                                    onClick = { viewModel.updateString(UserPreferences.FITNESS_GOAL, value) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Nutrition Targets
            SettingsSection(title = "Daily Targets", icon = Icons.Default.TrackChanges) {
                SettingsNumberField(
                    value = targetCalories.toString(),
                    onValueChange = { it.toIntOrNull()?.let { v -> viewModel.updateInt(UserPreferences.TARGET_CALORIES, v) } },
                    label = "Calories (kcal)"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingsNumberField(
                        value = targetProtein.toString(),
                        onValueChange = { it.toIntOrNull()?.let { v -> viewModel.updateInt(UserPreferences.TARGET_PROTEIN, v) } },
                        label = "Protein (g)",
                        modifier = Modifier.weight(1f)
                    )
                    SettingsNumberField(
                        value = targetCarbs.toString(),
                        onValueChange = { it.toIntOrNull()?.let { v -> viewModel.updateInt(UserPreferences.TARGET_CARBS, v) } },
                        label = "Carbs (g)",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingsNumberField(
                        value = targetFat.toString(),
                        onValueChange = { it.toIntOrNull()?.let { v -> viewModel.updateInt(UserPreferences.TARGET_FAT, v) } },
                        label = "Fat (g)",
                        modifier = Modifier.weight(1f)
                    )
                    SettingsNumberField(
                        value = targetFiber.toString(),
                        onValueChange = { it.toIntOrNull()?.let { v -> viewModel.updateInt(UserPreferences.TARGET_FIBER, v) } },
                        label = "Fiber (g)",
                        modifier = Modifier.weight(1f)
                    )
                }
                SettingsNumberField(
                    value = mealsPerDay.toString(),
                    onValueChange = { it.toIntOrNull()?.let { v -> viewModel.updateInt(UserPreferences.MEALS_PER_DAY, v) } },
                    label = "Meals per day"
                )
                SettingsNumberField(
                    value = if (waterGoalLiters == waterGoalLiters.toInt().toDouble()) waterGoalLiters.toInt().toString() else waterGoalLiters.toString(),
                    onValueChange = { it.toDoubleOrNull()?.let { v -> viewModel.updateDouble(UserPreferences.WATER_GOAL_LITERS, v) } },
                    label = "Water goal (liters)",
                    decimal = true
                )
            }

            // Dietary Preferences
            SettingsSection(title = "Dietary Preferences", icon = Icons.Default.RestaurantMenu) {
                SettingsTextField(
                    value = dietaryRestrictions,
                    onValueChange = { viewModel.updateString(UserPreferences.DIETARY_RESTRICTIONS, it) },
                    label = "Dietary Restrictions",
                    supportingText = "e.g., vegetarian, kosher, halal, vegan",
                    singleLine = false,
                    minLines = 2
                )
                SettingsTextField(
                    value = allergies,
                    onValueChange = { viewModel.updateString(UserPreferences.ALLERGIES, it) },
                    label = "Allergies",
                    supportingText = "e.g., nuts, dairy, gluten",
                    singleLine = false,
                    minLines = 2
                )
                SettingsTextField(
                    value = customInstructions,
                    onValueChange = { viewModel.updateString(UserPreferences.CUSTOM_INSTRUCTIONS, it) },
                    label = "Custom AI Instructions",
                    supportingText = "Extra context for the AI when analyzing meals",
                    singleLine = false,
                    minLines = 3
                )
            }

            // App Preferences
            SettingsSection(title = "App", icon = Icons.Default.Tune) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Use dark theme",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = darkMode,
                        onCheckedChange = { viewModel.updateBoolean(UserPreferences.DARK_MODE, it) }
                    )
                }
            }

            // Info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "NutriTrack v2.0",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Day resets at 5:00 AM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(12.dp),
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        supportingText = supportingText?.let { { Text(it) } }
    )
}

@Composable
private fun SettingsNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            val filtered = newValue.filter { it.isDigit() || (decimal && it == '.') }
            onValueChange(filtered)
        },
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
        )
    )
}
