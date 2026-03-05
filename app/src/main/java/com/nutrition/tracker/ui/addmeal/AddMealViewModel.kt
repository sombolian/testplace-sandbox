package com.nutrition.tracker.ui.addmeal

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutrition.tracker.data.database.AppDatabase
import com.nutrition.tracker.data.model.Meal
import com.nutrition.tracker.data.preferences.UserPreferences
import com.nutrition.tracker.data.repository.MealRepository
import com.nutrition.tracker.network.GeminiApiService
import com.nutrition.tracker.network.NutritionEstimate
import com.nutrition.tracker.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AddMealUiState(
    val description: String = "",
    val imageBitmap: Bitmap? = null,
    val imageUri: String? = null,
    val isAnalyzing: Boolean = false,
    val estimate: NutritionEstimate? = null,
    val error: String? = null,
    val isSaved: Boolean = false,
    // Manual override fields
    val manualCalories: String = "",
    val manualProtein: String = "",
    val manualCarbs: String = "",
    val manualFat: String = "",
    val manualFiber: String = "",
    val showManualEdit: Boolean = false
)

class AddMealViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MealRepository(AppDatabase.getDatabase(application))
    private val preferences = UserPreferences(application)
    private val geminiService = GeminiApiService()

    private val _uiState = MutableStateFlow(AddMealUiState())
    val uiState: StateFlow<AddMealUiState> = _uiState

    fun updateDescription(text: String) {
        _uiState.update { it.copy(description = text) }
    }

    fun setImage(bitmap: Bitmap?, uri: String?) {
        _uiState.update { it.copy(imageBitmap = bitmap, imageUri = uri) }
    }

    fun clearImage() {
        _uiState.update { it.copy(imageBitmap = null, imageUri = null) }
    }

    fun toggleManualEdit() {
        _uiState.update { state ->
            val show = !state.showManualEdit
            if (show && state.estimate != null) {
                state.copy(
                    showManualEdit = true,
                    manualCalories = state.estimate.calories.toString(),
                    manualProtein = state.estimate.protein.toInt().toString(),
                    manualCarbs = state.estimate.carbs.toInt().toString(),
                    manualFat = state.estimate.fat.toInt().toString(),
                    manualFiber = state.estimate.fiber.toInt().toString()
                )
            } else {
                state.copy(showManualEdit = show)
            }
        }
    }

    fun updateManualField(field: String, value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        _uiState.update {
            when (field) {
                "calories" -> it.copy(manualCalories = filtered)
                "protein" -> it.copy(manualProtein = filtered)
                "carbs" -> it.copy(manualCarbs = filtered)
                "fat" -> it.copy(manualFat = filtered)
                "fiber" -> it.copy(manualFiber = filtered)
                else -> it
            }
        }
    }

    fun analyzeMeal() {
        val state = _uiState.value
        if (state.description.isBlank() && state.imageBitmap == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null) }

            val apiKey = preferences.geminiApiKey.first()
            val model = preferences.geminiModel.first()
            val goal = preferences.fitnessGoal.first()
            val weight = preferences.weightKg.first()
            val restrictions = preferences.dietaryRestrictions.first()
            val custom = preferences.customInstructions.first()

            val userContext = buildString {
                append("Fitness goal: $goal, Weight: ${weight}kg")
                if (restrictions.isNotBlank()) append(", Dietary restrictions: $restrictions")
                if (custom.isNotBlank()) append(", Additional: $custom")
            }

            geminiService.analyzeMeal(
                apiKey = apiKey,
                modelName = model,
                textDescription = state.description.ifBlank { null },
                image = state.imageBitmap,
                userContext = userContext
            ).onSuccess { estimate ->
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        estimate = estimate,
                        manualCalories = estimate.calories.toString(),
                        manualProtein = estimate.protein.toInt().toString(),
                        manualCarbs = estimate.carbs.toInt().toString(),
                        manualFat = estimate.fat.toInt().toString(),
                        manualFiber = estimate.fiber.toInt().toString()
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isAnalyzing = false, error = e.message) }
            }
        }
    }

    fun saveMeal() {
        val state = _uiState.value
        val estimate = state.estimate ?: return

        viewModelScope.launch {
            val calories = if (state.showManualEdit) state.manualCalories.toIntOrNull() ?: estimate.calories else estimate.calories
            val protein = if (state.showManualEdit) state.manualProtein.toDoubleOrNull() ?: estimate.protein else estimate.protein
            val carbs = if (state.showManualEdit) state.manualCarbs.toDoubleOrNull() ?: estimate.carbs else estimate.carbs
            val fat = if (state.showManualEdit) state.manualFat.toDoubleOrNull() ?: estimate.fat else estimate.fat
            val fiber = if (state.showManualEdit) state.manualFiber.toDoubleOrNull() ?: estimate.fiber else estimate.fiber

            val meal = Meal(
                description = state.description.ifBlank { estimate.description },
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                fiber = fiber,
                imageUri = state.imageUri,
                nutritionDay = DateUtils.getNutritionDay()
            )

            repository.insertMeal(meal)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun saveManual() {
        val state = _uiState.value
        viewModelScope.launch {
            val meal = Meal(
                description = state.description.ifBlank { "Manual entry" },
                calories = state.manualCalories.toIntOrNull() ?: 0,
                protein = state.manualProtein.toDoubleOrNull() ?: 0.0,
                carbs = state.manualCarbs.toDoubleOrNull() ?: 0.0,
                fat = state.manualFat.toDoubleOrNull() ?: 0.0,
                fiber = state.manualFiber.toDoubleOrNull() ?: 0.0,
                imageUri = state.imageUri,
                nutritionDay = DateUtils.getNutritionDay()
            )
            repository.insertMeal(meal)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
