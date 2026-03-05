package com.nutrition.tracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        // Gemini
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")

        // Personal info
        val USER_NAME = stringPreferencesKey("user_name")
        val HEIGHT_CM = intPreferencesKey("height_cm")
        val WEIGHT_KG = doublePreferencesKey("weight_kg")
        val AGE = intPreferencesKey("age")
        val GENDER = stringPreferencesKey("gender")
        val ACTIVITY_LEVEL = stringPreferencesKey("activity_level")
        val FITNESS_GOAL = stringPreferencesKey("fitness_goal")

        // Targets
        val TARGET_CALORIES = intPreferencesKey("target_calories")
        val TARGET_PROTEIN = intPreferencesKey("target_protein")
        val TARGET_CARBS = intPreferencesKey("target_carbs")
        val TARGET_FAT = intPreferencesKey("target_fat")
        val TARGET_FIBER = intPreferencesKey("target_fiber")

        // Preferences
        val MEALS_PER_DAY = intPreferencesKey("meals_per_day")
        val WATER_GOAL_LITERS = doublePreferencesKey("water_goal_liters")
        val LANGUAGE = stringPreferencesKey("language")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")

        // Custom prompt additions
        val CUSTOM_INSTRUCTIONS = stringPreferencesKey("custom_instructions")
        val DIETARY_RESTRICTIONS = stringPreferencesKey("dietary_restrictions")
        val ALLERGIES = stringPreferencesKey("allergies")
    }

    val geminiApiKey: Flow<String> = context.dataStore.data.map { it[GEMINI_API_KEY] ?: "" }
    val geminiModel: Flow<String> = context.dataStore.data.map { it[GEMINI_MODEL] ?: "gemini-2.0-flash" }
    val userName: Flow<String> = context.dataStore.data.map { it[USER_NAME] ?: "" }
    val heightCm: Flow<Int> = context.dataStore.data.map { it[HEIGHT_CM] ?: 170 }
    val weightKg: Flow<Double> = context.dataStore.data.map { it[WEIGHT_KG] ?: 70.0 }
    val age: Flow<Int> = context.dataStore.data.map { it[AGE] ?: 25 }
    val gender: Flow<String> = context.dataStore.data.map { it[GENDER] ?: "male" }
    val activityLevel: Flow<String> = context.dataStore.data.map { it[ACTIVITY_LEVEL] ?: "moderate" }
    val fitnessGoal: Flow<String> = context.dataStore.data.map { it[FITNESS_GOAL] ?: "tone" }
    val targetCalories: Flow<Int> = context.dataStore.data.map { it[TARGET_CALORIES] ?: 2000 }
    val targetProtein: Flow<Int> = context.dataStore.data.map { it[TARGET_PROTEIN] ?: 150 }
    val targetCarbs: Flow<Int> = context.dataStore.data.map { it[TARGET_CARBS] ?: 200 }
    val targetFat: Flow<Int> = context.dataStore.data.map { it[TARGET_FAT] ?: 65 }
    val targetFiber: Flow<Int> = context.dataStore.data.map { it[TARGET_FIBER] ?: 30 }
    val mealsPerDay: Flow<Int> = context.dataStore.data.map { it[MEALS_PER_DAY] ?: 4 }
    val waterGoalLiters: Flow<Double> = context.dataStore.data.map { it[WATER_GOAL_LITERS] ?: 3.0 }
    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE] ?: false }
    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_COMPLETE] ?: false }
    val customInstructions: Flow<String> = context.dataStore.data.map { it[CUSTOM_INSTRUCTIONS] ?: "" }
    val dietaryRestrictions: Flow<String> = context.dataStore.data.map { it[DIETARY_RESTRICTIONS] ?: "" }
    val allergies: Flow<String> = context.dataStore.data.map { it[ALLERGIES] ?: "" }

    suspend fun <T> setValue(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun setOnboardingComplete() {
        context.dataStore.edit { it[ONBOARDING_COMPLETE] = true }
    }
}
