package com.nutrition.tracker.data.repository

import com.nutrition.tracker.data.database.AppDatabase
import com.nutrition.tracker.data.database.DaySummaryTuple
import com.nutrition.tracker.data.model.DayRating
import com.nutrition.tracker.data.model.DaySummary
import com.nutrition.tracker.data.model.Meal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MealRepository(private val database: AppDatabase) {
    private val mealDao = database.mealDao()

    fun getMealsForDay(day: String): Flow<List<Meal>> = mealDao.getMealsForDay(day)

    fun getDaySummary(day: String): Flow<DaySummary?> = mealDao.getDaySummary(day).map { tuple ->
        tuple?.let {
            if (it.mealCount == 0) null
            else DaySummary(
                date = it.nutritionDay,
                totalCalories = it.totalCalories,
                totalProtein = it.totalProtein,
                totalCarbs = it.totalCarbs,
                totalFat = it.totalFat,
                totalFiber = it.totalFiber,
                mealCount = it.mealCount
            )
        }
    }

    suspend fun getDaySummaries(startDay: String, endDay: String): List<DaySummary> {
        return mealDao.getDaySummaries(startDay, endDay).map { it.toDaySummary() }
    }

    suspend fun insertMeal(meal: Meal): Long = mealDao.insertMeal(meal)

    suspend fun deleteMeal(meal: Meal) = mealDao.deleteMeal(meal)

    suspend fun getAllDaysWithMeals(): List<String> = mealDao.getAllDaysWithMeals()

    companion object {
        fun calculateRating(
            summary: DaySummary?,
            targetCalories: Int,
            targetProtein: Int
        ): DayRating {
            if (summary == null || summary.mealCount == 0) return DayRating.NO_DATA

            val caloriePct = summary.totalCalories.toDouble() / targetCalories
            val proteinPct = summary.totalProtein / targetProtein

            val calorieScore = when {
                caloriePct in 0.9..1.1 -> 1.0
                caloriePct in 0.8..1.2 -> 0.7
                caloriePct in 0.7..1.3 -> 0.4
                else -> 0.1
            }

            val proteinScore = when {
                proteinPct >= 1.0 -> 1.0
                proteinPct >= 0.85 -> 0.8
                proteinPct >= 0.7 -> 0.6
                proteinPct >= 0.5 -> 0.3
                else -> 0.1
            }

            val totalScore = (calorieScore * 0.5 + proteinScore * 0.5)

            return when {
                totalScore >= 0.9 -> DayRating.PERFECT
                totalScore >= 0.75 -> DayRating.GREAT
                totalScore >= 0.6 -> DayRating.GOOD
                totalScore >= 0.45 -> DayRating.OK
                totalScore >= 0.3 -> DayRating.NEEDS_WORK
                else -> DayRating.OFF_TRACK
            }
        }
    }
}

private fun DaySummaryTuple.toDaySummary() = DaySummary(
    date = nutritionDay,
    totalCalories = totalCalories,
    totalProtein = totalProtein,
    totalCarbs = totalCarbs,
    totalFat = totalFat,
    totalFiber = totalFiber,
    mealCount = mealCount
)
