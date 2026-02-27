package com.notifytts.service

import android.app.Notification
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
import com.notifytts.data.*

class NotificationProcessor(
    private val prefs: PreferencesManager,
    private val packageManager: PackageManager
) {

    private val recentNotifications = mutableMapOf<String, Long>()

    /** Tracks recently read app+title combos to avoid repeating "App: Title" for chat-like messages */
    private val recentSenders = mutableMapOf<String, Long>()
    private val SENDER_REPEAT_WINDOW = 120_000L // 2 minutes

    /** Strip invisible Unicode characters (RTL/LTR marks, zero-width chars, bidi controls) */
    private fun stripInvisibleChars(text: String): String {
        return text.replace(Regex("[\u200B-\u200F\u2028-\u202F\u2060-\u206F\uFEFF]"), "")
    }

    data class ProcessResult(
        val shouldRead: Boolean,
        val text: String = "",
        val skipReason: String? = null,
        val appName: String = "",
        val title: String = "",
        val content: String = ""
    )

    companion object {
        private const val TAG = "NotifProcessor"
    }

    fun process(sbn: StatusBarNotification): ProcessResult {
        val notification = sbn.notification ?: return ProcessResult(false, skipReason = "Null notification")
        val extras = notification.extras

        // Get app name
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            sbn.packageName
        }

        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        // Extract richer text from notification extras
        // Apps like Telegram put actual message content in EXTRA_TEXT_LINES or EXTRA_BIG_TEXT
        // while EXTRA_TEXT may only say "posted 2 photos" or "photo"
        val textLines = try {
            extras?.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.mapNotNull { cs -> cs?.toString()?.takeIf { s -> s.isNotBlank() } }
                ?.takeIf { list -> list.isNotEmpty() }
        } catch (_: Exception) { null }

        // Build the best content: prefer text lines (if text is a media summary) > big text > text
        val content: String = when {
            textLines != null && isMediaSummary(text) -> {
                // Text lines contain the actual messages; join the most recent ones
                textLines.takeLast(3).joinToString(". ")
            }
            !bigText.isNullOrBlank() -> bigText
            textLines != null && textLines.last().length > text.length -> textLines.last()
            else -> text
        }

        Log.d(TAG, "Notification from $appName: title='$title', text='$text', bigText='${bigText?.take(50)}', " +
                "textLines=${textLines?.size ?: 0}, content='${content.take(80)}'")


        // Check if notification is empty
        if (prefs.ignoreEmpty && title.isBlank() && content.isBlank()) {
            return ProcessResult(false, skipReason = "Empty notification", appName = appName, title = title, content = content)
        }

        // Check ongoing
        if (prefs.ignoreOngoing && (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) {
            return ProcessResult(false, skipReason = "Ongoing notification", appName = appName, title = title, content = content)
        }

        // Check group summary
        if (prefs.ignoreGroupSummary && (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            return ProcessResult(false, skipReason = "Group summary", appName = appName, title = title, content = content)
        }

        // Check priority
        @Suppress("DEPRECATION")
        val priority = notification.priority
        if (priority < prefs.minPriority) {
            return ProcessResult(false, skipReason = "Low priority ($priority)", appName = appName, title = title, content = content)
        }

        // Check silent (no sound channel)
        if (prefs.ignoreSilent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // If notification has no sound indicator
            if (notification.sound == null && (notification.defaults and Notification.DEFAULT_SOUND) == 0) {
                // Only skip if the channel importance is low
            }
        }

        // Check app filter
        if (!prefs.isAppAllowed(sbn.packageName)) {
            return ProcessResult(false, skipReason = "App filtered", appName = appName, title = title, content = content)
        }

        // Check keyword rules
        // Include appName, title, and content so block rules match anywhere
        // Strip invisible Unicode chars (RTL marks, zero-width spaces, etc.) so Hebrew rules match
        val fullText = stripInvisibleChars("$appName $title $content")
        val keywordRules = prefs.getKeywordRules()

        for (rule in keywordRules) {
            val cleanPattern = stripInvisibleChars(rule.pattern)
            val matches = if (rule.isRegex) {
                try {
                    Regex(cleanPattern, RegexOption.IGNORE_CASE).containsMatchIn(fullText)
                } catch (e: Exception) {
                    false
                }
            } else {
                fullText.contains(cleanPattern, ignoreCase = true)
            }

            when (rule.action) {
                KeywordAction.BLOCK -> {
                    if (matches) {
                        return ProcessResult(false, skipReason = "Keyword blocked: ${rule.pattern}",
                            appName = appName, title = title, content = content)
                    }
                }
                KeywordAction.REQUIRE -> {
                    if (!matches) {
                        return ProcessResult(false, skipReason = "Required keyword missing: ${rule.pattern}",
                            appName = appName, title = title, content = content)
                    }
                }
                KeywordAction.REPLACE -> {
                    // Replacement handled during text formatting
                }
            }
        }

        // Check duplicate
        val dedupeKey = "${sbn.packageName}:$title:$content"
        val now = System.currentTimeMillis()
        val lastSeen = recentNotifications[dedupeKey]
        if (lastSeen != null && (now - lastSeen) < prefs.duplicateTimeout) {
            return ProcessResult(false, skipReason = "Duplicate", appName = appName, title = title, content = content)
        }
        recentNotifications[dedupeKey] = now

        // Clean old entries
        recentNotifications.entries.removeAll { (now - it.value) > prefs.duplicateTimeout * 2 }

        // Check if same app+title was recently read (chat-like repeated messages)
        val senderKey = "${sbn.packageName}:$title"
        val lastRead = recentSenders[senderKey]
        val isRepeatSender = lastRead != null && (now - lastRead) < SENDER_REPEAT_WINDOW
        recentSenders[senderKey] = now

        // Clean old sender entries
        recentSenders.entries.removeAll { (now - it.value) > SENDER_REPEAT_WINDOW * 2 }

        // Build the message text
        val messageText = formatMessage(appName, title, content, isRepeatSender)

        return ProcessResult(
            shouldRead = true,
            text = messageText,
            appName = appName,
            title = title,
            content = content
        )
    }

    private fun formatMessage(appName: String, title: String, content: String, isRepeatSender: Boolean = false): String {
        val contentPart = if (prefs.readContent) content else ""

        if (isRepeatSender) {
            // Repeat sender within 2 min: skip both app name and title, read content only
            var result = contentPart

            // Apply replacement keyword rules
            val keywordRules = prefs.getKeywordRules()
            for (rule in keywordRules) {
                if (rule.action == KeywordAction.REPLACE) {
                    result = if (rule.isRegex) {
                        try { result.replace(Regex(rule.pattern, RegexOption.IGNORE_CASE), "") } catch (_: Exception) { result }
                    } else {
                        result.replace(rule.pattern, "", ignoreCase = true)
                    }
                }
            }

            result = result.replace(Regex("\\s+"), " ").trim()
            if (result.length > prefs.maxTextLength) result = result.take(prefs.maxTextLength)
            return result
        }

        // First message from this sender: always include app name and title
        val appPart = if (prefs.readAppName) appName else ""
        val titlePart = if (prefs.readTitle) title else ""

        var result = prefs.messageFormat
            .replace("{app}", appPart)
            .replace("{title}", titlePart)
            .replace("{text}", contentPart)
            .replace("{content}", contentPart)

        // Apply replacement keyword rules
        val keywordRules = prefs.getKeywordRules()
        for (rule in keywordRules) {
            if (rule.action == KeywordAction.REPLACE) {
                result = if (rule.isRegex) {
                    try {
                        result.replace(Regex(rule.pattern, RegexOption.IGNORE_CASE), "")
                    } catch (e: Exception) {
                        result
                    }
                } else {
                    result.replace(rule.pattern, "", ignoreCase = true)
                }
            }
        }

        // Clean up extra whitespace and punctuation
        result = result.replace(Regex("\\s+"), " ")
            .replace(Regex("[:.]\\s*[:.]"), ".")
            .trim()

        // Truncate if needed
        if (result.length > prefs.maxTextLength) {
            result = result.take(prefs.maxTextLength)
        }

        return result
    }

    /** Detects summary-only text like "posted 2 photos", "photo", "video", "sticker" etc. */
    private fun isMediaSummary(text: String): Boolean {
        val t = text.trim().lowercase()
        return t.matches(Regex("(posted )?\\d+ photos?")) ||
                t.matches(Regex("(posted )?\\d+ videos?")) ||
                t.matches(Regex("(posted )?\\d+ (files?|stickers?|gifs?|documents?)")) ||
                t in listOf("photo", "video", "sticker", "gif", "voice message", "video message",
                    "document", "file", "audio", "animation", "contact", "location")
    }
}
