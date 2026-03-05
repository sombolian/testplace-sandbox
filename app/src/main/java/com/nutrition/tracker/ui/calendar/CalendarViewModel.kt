package com.nutrition.tracker.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutrition.tracker.data.database.AppDatabase
import com.nutrition.tracker.data.model.DayRating
import com.nutrition.tracker.data.model.DaySummary
import com.nutrition.tracker.data.preferences.UserPreferences
import com.nutrition.tracker.data.repository.MealRepository
import com.nutrition.tracker.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MealRepository(AppDatabase.getDatabase(application))
    private val preferences = UserPreferences(application)

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    private val _daySummaries = MutableStateFlow<Map<String, DaySummary>>(emptyMap())
    val daySummaries: StateFlow<Map<String, DaySummary>> = _daySummaries

    val targetCalories = preferences.targetCalories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    val targetProtein = preferences.targetProtein
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 150)

    init {
        viewModelScope.launch {
            _currentMonth.collectLatest { month ->
                loadMonth(month)
            }
        }
    }

    private suspend fun loadMonth(month: YearMonth) {
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        val summaries = repository.getDaySummaries(
            DateUtils.formatDate(start),
            DateUtils.formatDate(end)
        )
        _daySummaries.value = summaries.associateBy { it.date }
    }

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun getRatingForDay(dateStr: String): DayRating {
        val summary = _daySummaries.value[dateStr]
        return MealRepository.calculateRating(summary, targetCalories.value, targetProtein.value)
    }
}
