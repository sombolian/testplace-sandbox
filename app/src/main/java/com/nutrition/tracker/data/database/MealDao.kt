package com.nutrition.tracker.data.database

import androidx.room.*
import com.nutrition.tracker.data.model.Meal
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert
    suspend fun insertMeal(meal: Meal): Long

    @Update
    suspend fun updateMeal(meal: Meal)

    @Delete
    suspend fun deleteMeal(meal: Meal)

    @Query("SELECT * FROM meals WHERE nutritionDay = :day ORDER BY timestamp DESC")
    fun getMealsForDay(day: String): Flow<List<Meal>>

    @Query("SELECT * FROM meals WHERE nutritionDay = :day ORDER BY timestamp DESC")
    suspend fun getMealsForDayOnce(day: String): List<Meal>

    @Query("""
        SELECT nutritionDay,
               SUM(calories) as totalCalories,
               SUM(protein) as totalProtein,
               SUM(carbs) as totalCarbs,
               SUM(fat) as totalFat,
               SUM(fiber) as totalFiber,
               COUNT(*) as mealCount
        FROM meals
        WHERE nutritionDay BETWEEN :startDay AND :endDay
        GROUP BY nutritionDay
        ORDER BY nutritionDay DESC
    """)
    suspend fun getDaySummaries(startDay: String, endDay: String): List<DaySummaryTuple>

    @Query("""
        SELECT nutritionDay,
               SUM(calories) as totalCalories,
               SUM(protein) as totalProtein,
               SUM(carbs) as totalCarbs,
               SUM(fat) as totalFat,
               SUM(fiber) as totalFiber,
               COUNT(*) as mealCount
        FROM meals
        WHERE nutritionDay = :day
    """)
    fun getDaySummary(day: String): Flow<DaySummaryTuple?>

    @Query("SELECT DISTINCT nutritionDay FROM meals ORDER BY nutritionDay DESC")
    suspend fun getAllDaysWithMeals(): List<String>
}

data class DaySummaryTuple(
    val nutritionDay: String,
    val totalCalories: Int,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val totalFiber: Double,
    val mealCount: Int
)
