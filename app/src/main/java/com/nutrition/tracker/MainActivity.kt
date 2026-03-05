package com.nutrition.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.nutrition.tracker.data.preferences.UserPreferences
import com.nutrition.tracker.ui.navigation.NutritionNavGraph
import com.nutrition.tracker.ui.navigation.Screen
import com.nutrition.tracker.ui.theme.NutritionTrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferences = UserPreferences(this)
        val onboardingComplete = runBlocking { preferences.onboardingComplete.first() }
        val darkMode = runBlocking { preferences.darkMode.first() }

        setContent {
            var isDarkMode by remember { mutableStateOf(darkMode) }

            // Listen for dark mode changes
            LaunchedEffect(Unit) {
                preferences.darkMode.collect { isDarkMode = it }
            }

            NutritionTrackerTheme(darkTheme = isDarkMode, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val startDestination = if (onboardingComplete) Screen.Home.route else Screen.Onboarding.route

                    NutritionNavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        onOnboardingComplete = { data ->
                            lifecycleScope.launch {
                                preferences.setValue(UserPreferences.USER_NAME, data.name)
                                preferences.setValue(UserPreferences.GENDER, data.gender)
                                preferences.setValue(UserPreferences.AGE, data.age.toIntOrNull() ?: 25)
                                preferences.setValue(UserPreferences.HEIGHT_CM, data.heightCm.toIntOrNull() ?: 170)
                                preferences.setValue(UserPreferences.WEIGHT_KG, data.weightKg.toDoubleOrNull() ?: 70.0)
                                preferences.setValue(UserPreferences.ACTIVITY_LEVEL, data.activityLevel)
                                preferences.setValue(UserPreferences.FITNESS_GOAL, data.fitnessGoal)
                                preferences.setValue(UserPreferences.TARGET_CALORIES, data.targetCalories.toIntOrNull() ?: 2000)
                                preferences.setValue(UserPreferences.TARGET_PROTEIN, data.targetProtein.toIntOrNull() ?: 150)
                                preferences.setValue(UserPreferences.GEMINI_API_KEY, data.geminiApiKey)
                                preferences.setValue(UserPreferences.GEMINI_MODEL, data.geminiModel)
                                preferences.setOnboardingComplete()
                            }
                        }
                    )
                }
            }
        }
    }
}
