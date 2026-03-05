package com.nutrition.tracker.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OnboardingData(
    val name: String = "",
    val gender: String = "male",
    val age: String = "25",
    val heightCm: String = "170",
    val weightKg: String = "70",
    val activityLevel: String = "moderate",
    val fitnessGoal: String = "tone",
    val targetCalories: String = "2000",
    val targetProtein: String = "150",
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-2.0-flash"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: (OnboardingData) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var currentStep by remember { mutableIntStateOf(0) }
    var data by remember { mutableStateOf(OnboardingData()) }
    val totalSteps = 5

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val bgOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f + bgOffset * 0.2f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f + (1 - bgOffset) * 0.15f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Progress indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(totalSteps) { index ->
                    val width by animateDpAsState(
                        targetValue = if (index == currentStep) 32.dp else 12.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "dot_width"
                    )
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (index <= currentStep) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it } + fadeOut())
                },
                modifier = Modifier.weight(1f),
                label = "step"
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (step) {
                        0 -> WelcomeStep(data, onDataChange = { data = it })
                        1 -> BodyInfoStep(data, onDataChange = { data = it })
                        2 -> GoalsStep(data, onDataChange = { data = it })
                        3 -> TargetsStep(data, onDataChange = { data = it })
                        4 -> ApiSetupStep(data, onDataChange = { data = it })
                    }
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    TextButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentStep--
                    }) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                val buttonScale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "btn_scale"
                )

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (currentStep < totalSteps - 1) {
                            currentStep++
                        } else {
                            onComplete(data)
                        }
                    },
                    modifier = Modifier.scale(buttonScale),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                ) {
                    Text(
                        if (currentStep == totalSteps - 1) "Let's Go!" else "Next",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(data: OnboardingData, onDataChange: (OnboardingData) -> Unit) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "welcome_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 40.dp)
    ) {
        Icon(
            Icons.Default.Restaurant,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .scale(scale),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Welcome to NutriTrack",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Your AI-powered nutrition companion.\nLet's set things up to personalize your experience.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = data.name,
            onValueChange = { onDataChange(data.copy(name = it)) },
            label = { Text("What's your name?") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BodyInfoStep(data: OnboardingData, onDataChange: (OnboardingData) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StepHeader(
            icon = Icons.Default.Person,
            title = "About You",
            subtitle = "Help us calculate your nutritional needs"
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Gender selection
        Text("Gender", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("male" to "Male", "female" to "Female").forEach { (value, label) ->
                FilterChip(
                    selected = data.gender == value,
                    onClick = { onDataChange(data.copy(gender = value)) },
                    label = { Text(label) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = data.age,
                onValueChange = { onDataChange(data.copy(age = it.filter { c -> c.isDigit() })) },
                label = { Text("Age") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = data.heightCm,
                onValueChange = { onDataChange(data.copy(heightCm = it.filter { c -> c.isDigit() })) },
                label = { Text("Height (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = data.weightKg,
            onValueChange = { onDataChange(data.copy(weightKg = it.filter { c -> c.isDigit() || c == '.' })) },
            label = { Text("Weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text("Activity Level", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))

        val activities = listOf(
            "sedentary" to "Sedentary",
            "light" to "Light",
            "moderate" to "Moderate",
            "active" to "Active",
            "very_active" to "Very Active"
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            activities.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (value, label) ->
                        FilterChip(
                            selected = data.activityLevel == value,
                            onClick = { onDataChange(data.copy(activityLevel = value)) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalsStep(data: OnboardingData, onDataChange: (OnboardingData) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StepHeader(
            icon = Icons.Default.FitnessCenter,
            title = "Your Goal",
            subtitle = "What are you working towards?"
        )

        Spacer(modifier = Modifier.height(32.dp))

        val goals = listOf(
            Triple("tone", "Toning / חיטוב", "Build lean muscle, reduce body fat"),
            Triple("bulk", "Bulk / Build Muscle", "Gain muscle mass with calorie surplus"),
            Triple("cut", "Cut / Lose Fat", "Reduce body fat while preserving muscle"),
            Triple("maintain", "Maintain", "Keep current weight and composition"),
            Triple("recomp", "Body Recomp", "Build muscle and lose fat simultaneously")
        )

        goals.forEach { (value, title, desc) ->
            val isSelected = data.fitnessGoal == value
            Card(
                onClick = { onDataChange(data.copy(fitnessGoal = value)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isSelected, onClick = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(title, fontWeight = FontWeight.SemiBold)
                        Text(
                            desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetsStep(data: OnboardingData, onDataChange: (OnboardingData) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StepHeader(
            icon = Icons.Default.TrackChanges,
            title = "Daily Targets",
            subtitle = "Set your nutrition goals (you can adjust later)"
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = data.targetCalories,
            onValueChange = { onDataChange(data.copy(targetCalories = it.filter { c -> c.isDigit() })) },
            label = { Text("Daily Calories (kcal)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = data.targetProtein,
            onValueChange = { onDataChange(data.copy(targetProtein = it.filter { c -> c.isDigit() })) },
            label = { Text("Daily Protein (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "For toning, aim for high protein (1.6-2.2g per kg body weight) and a slight calorie deficit or maintenance level.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun ApiSetupStep(data: OnboardingData, onDataChange: (OnboardingData) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StepHeader(
            icon = Icons.Default.SmartToy,
            title = "AI Setup",
            subtitle = "Connect Gemini to analyze your meals"
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = data.geminiApiKey,
            onValueChange = { onDataChange(data.copy(geminiApiKey = it)) },
            label = { Text("Gemini API Key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = data.geminiModel,
            onValueChange = { onDataChange(data.copy(geminiModel = it)) },
            label = { Text("Model Name") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("Default: gemini-2.0-flash") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Get your API key from Google AI Studio (aistudio.google.com). Your key is stored locally on your device only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun StepHeader(icon: ImageVector, title: String, subtitle: String) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "header_scale"
    )

    Icon(
        icon,
        contentDescription = null,
        modifier = Modifier
            .size(56.dp)
            .scale(scale),
        tint = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        subtitle,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
