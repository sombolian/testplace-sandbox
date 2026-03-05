package com.nutrition.tracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val nutritionDay: String // format: yyyy-MM-dd, based on 5AM cutoff
)
