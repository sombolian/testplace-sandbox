package com.nutrition.tracker.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object DateUtils {
    private val DAY_CUTOFF = LocalTime.of(5, 0) // 5:00 AM
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val DISPLAY_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMM d")
    private val SHORT_FORMAT = DateTimeFormatter.ofPattern("MMM d")

    fun getNutritionDay(dateTime: LocalDateTime = LocalDateTime.now()): String {
        val effectiveDate = if (dateTime.toLocalTime().isBefore(DAY_CUTOFF)) {
            dateTime.toLocalDate().minusDays(1)
        } else {
            dateTime.toLocalDate()
        }
        return effectiveDate.format(DATE_FORMAT)
    }

    fun getNutritionDayDate(dateTime: LocalDateTime = LocalDateTime.now()): LocalDate {
        return if (dateTime.toLocalTime().isBefore(DAY_CUTOFF)) {
            dateTime.toLocalDate().minusDays(1)
        } else {
            dateTime.toLocalDate()
        }
    }

    fun formatForDisplay(dateStr: String): String {
        val date = LocalDate.parse(dateStr, DATE_FORMAT)
        val today = getNutritionDayDate()
        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            today.plusDays(1) -> "Tomorrow"
            else -> date.format(DISPLAY_FORMAT)
        }
    }

    fun formatShort(dateStr: String): String {
        return LocalDate.parse(dateStr, DATE_FORMAT).format(SHORT_FORMAT)
    }

    fun parseDate(dateStr: String): LocalDate {
        return LocalDate.parse(dateStr, DATE_FORMAT)
    }

    fun formatDate(date: LocalDate): String {
        return date.format(DATE_FORMAT)
    }
}
