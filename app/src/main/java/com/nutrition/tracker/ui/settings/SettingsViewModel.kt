package com.nutrition.tracker.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutrition.tracker.data.preferences.UserPreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    val preferences = UserPreferences(application)

    val geminiApiKey = preferences.geminiApiKey.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val geminiModel = preferences.geminiModel.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gemini-2.0-flash")
    val userName = preferences.userName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val heightCm = preferences.heightCm.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 170)
    val weightKg = preferences.weightKg.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 70.0)
    val age = preferences.age.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25)
    val gender = preferences.gender.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "male")
    val activityLevel = preferences.activityLevel.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "moderate")
    val fitnessGoal = preferences.fitnessGoal.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "tone")
    val targetCalories = preferences.targetCalories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)
    val targetProtein = preferences.targetProtein.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 150)
    val targetCarbs = preferences.targetCarbs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 200)
    val targetFat = preferences.targetFat.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 65)
    val targetFiber = preferences.targetFiber.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)
    val mealsPerDay = preferences.mealsPerDay.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)
    val waterGoalLiters = preferences.waterGoalLiters.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3.0)
    val darkMode = preferences.darkMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val customInstructions = preferences.customInstructions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val dietaryRestrictions = preferences.dietaryRestrictions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val allergies = preferences.allergies.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun updateString(key: androidx.datastore.preferences.core.Preferences.Key<String>, value: String) {
        viewModelScope.launch { preferences.setValue(key, value) }
    }

    fun updateInt(key: androidx.datastore.preferences.core.Preferences.Key<Int>, value: Int) {
        viewModelScope.launch { preferences.setValue(key, value) }
    }

    fun updateDouble(key: androidx.datastore.preferences.core.Preferences.Key<Double>, value: Double) {
        viewModelScope.launch { preferences.setValue(key, value) }
    }

    fun updateBoolean(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, value: Boolean) {
        viewModelScope.launch { preferences.setValue(key, value) }
    }
}
