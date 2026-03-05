package com.nutrition.tracker.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutrition.tracker.data.database.AppDatabase
import com.nutrition.tracker.data.model.DayRating
import com.nutrition.tracker.data.model.DaySummary
import com.nutrition.tracker.data.model.Meal
import com.nutrition.tracker.data.preferences.UserPreferences
import com.nutrition.tracker.data.repository.MealRepository
import com.nutrition.tracker.network.GeminiApiService
import com.nutrition.tracker.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MealRepository(AppDatabase.getDatabase(application))
    private val preferences = UserPreferences(application)
    private val geminiService = GeminiApiService()

    private val _selectedDate = MutableStateFlow(DateUtils.getNutritionDayDate())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    val selectedDateStr: StateFlow<String> = _selectedDate.map { DateUtils.formatDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DateUtils.getNutritionDay())

    val meals: StateFlow<List<Meal>> = selectedDateStr.flatMapLatest { day ->
        repository.getMealsForDay(day)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val daySummary: StateFlow<DaySummary?> = selectedDateStr.flatMapLatest { day ->
        repository.getDaySummary(day)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val targetCalories = preferences.targetCalories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    val targetProtein = preferences.targetProtein
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 150)

    val targetCarbs = preferences.targetCarbs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 200)

    val targetFat = preferences.targetFat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 65)

    val userName = preferences.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val dayRating: StateFlow<DayRating> = combine(daySummary, targetCalories, targetProtein) { summary, cal, prot ->
        MealRepository.calculateRating(summary, cal, prot)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayRating.NO_DATA)

    private val _aiAdvice = MutableStateFlow<String?>(null)
    val aiAdvice: StateFlow<String?> = _aiAdvice

    private val _isLoadingAdvice = MutableStateFlow(false)
    val isLoadingAdvice: StateFlow<Boolean> = _isLoadingAdvice

    fun navigateDay(offset: Int) {
        _selectedDate.value = _selectedDate.value.plusDays(offset.toLong())
    }

    fun setDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun goToToday() {
        _selectedDate.value = DateUtils.getNutritionDayDate()
    }

    fun deleteMeal(meal: Meal) {
        viewModelScope.launch {
            repository.deleteMeal(meal)
        }
    }

    fun fetchAiAdvice() {
        viewModelScope.launch {
            _isLoadingAdvice.value = true
            val apiKey = preferences.geminiApiKey.first()
            val model = preferences.geminiModel.first()
            val summary = daySummary.value
            val goal = preferences.fitnessGoal.first()
            val weight = preferences.weightKg.first()

            if (apiKey.isBlank()) {
                _aiAdvice.value = "Please set your Gemini API key in Settings to use AI Coach."
                _isLoadingAdvice.value = false
                return@launch
            }

            if (summary == null || summary.mealCount == 0) {
                _aiAdvice.value = "Log some meals first so I can give you personalized advice!"
                _isLoadingAdvice.value = false
                return@launch
            }

            val summaryText = "Calories: ${summary.totalCalories}/${targetCalories.value}, Protein: ${summary.totalProtein}g/${targetProtein.value}g, Carbs: ${summary.totalCarbs}g, Fat: ${summary.totalFat}g, Meals: ${summary.mealCount}"
            val context = "Goal: $goal, Weight: ${weight}kg, Target cal: ${targetCalories.value}, Target protein: ${targetProtein.value}g"

            geminiService.getDailyAdvice(apiKey, model, summaryText, context)
                .onSuccess { _aiAdvice.value = it }
                .onFailure { _aiAdvice.value = "Could not get advice: ${it.message}" }

            _isLoadingAdvice.value = false
        }
    }
}
