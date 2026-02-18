package com.notifytts.data

data class AppFilterEntry(
    val packageName: String,
    val appName: String,
    val enabled: Boolean = true
)

data class KeywordRule(
    val id: String,
    val pattern: String,
    val isRegex: Boolean = false,
    val action: KeywordAction = KeywordAction.BLOCK
)

enum class KeywordAction {
    BLOCK,      // Block notification if keyword found
    REQUIRE,    // Only read if keyword found
    REPLACE     // Replace matched text before reading
}

enum class FilterMode {
    WHITELIST,  // Only read from selected apps
    BLACKLIST   // Read from all except selected apps
}

data class NotificationLogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val appPackage: String,
    val appName: String,
    val title: String,
    val text: String,
    val wasRead: Boolean,
    val skipReason: String? = null
)

data class GeminiVoice(
    val name: String,
    val gender: String = "",
    val description: String = ""
)

data class QuietHours(
    val enabled: Boolean = false,
    val startHour: Int = 22,
    val startMinute: Int = 0,
    val endHour: Int = 7,
    val endMinute: Int = 0
)
