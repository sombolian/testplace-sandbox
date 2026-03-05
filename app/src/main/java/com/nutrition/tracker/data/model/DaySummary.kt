package com.nutrition.tracker.data.model

data class DaySummary(
    val date: String, // yyyy-MM-dd
    val totalCalories: Int,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val totalFiber: Double,
    val mealCount: Int
)

enum class DayRating(val label: String, val emoji: String, val color: Long) {
    PERFECT("Perfect", "🏆", 0xFFFFD700),
    GREAT("Great", "⭐", 0xFF4CAF50),
    GOOD("Good", "👍", 0xFF8BC34A),
    OK("OK", "👌", 0xFFFFC107),
    NEEDS_WORK("Needs Work", "💪", 0xFFFF9800),
    OFF_TRACK("Off Track", "📉", 0xFFF44336),
    NO_DATA("No Data", "—", 0xFF9E9E9E);
}
