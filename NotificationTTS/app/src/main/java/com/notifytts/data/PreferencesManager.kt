package com.notifytts.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.notifytts.util.Constants

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ntts_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // ── General ──────────────────────────────────────────────────────

    var serviceEnabled: Boolean
        get() = prefs.getBoolean("service_enabled", false)
        set(value) = prefs.edit().putBoolean("service_enabled", value).apply()

    var onlyWhenBluetooth: Boolean
        get() = prefs.getBoolean("only_bluetooth", true)
        set(value) = prefs.edit().putBoolean("only_bluetooth", value).apply()

    var alsoWiredHeadphones: Boolean
        get() = prefs.getBoolean("also_wired", false)
        set(value) = prefs.edit().putBoolean("also_wired", value).apply()

    var alsoSpeaker: Boolean
        get() = prefs.getBoolean("also_speaker", false)
        set(value) = prefs.edit().putBoolean("also_speaker", value).apply()

    var respectDoNotDisturb: Boolean
        get() = prefs.getBoolean("respect_dnd", true)
        set(value) = prefs.edit().putBoolean("respect_dnd", value).apply()

    var screenOffOnly: Boolean
        get() = prefs.getBoolean("screen_off_only", false)
        set(value) = prefs.edit().putBoolean("screen_off_only", value).apply()

    var shakeToPause: Boolean
        get() = prefs.getBoolean("shake_to_pause", false)
        set(value) = prefs.edit().putBoolean("shake_to_pause", value).apply()

    // ── App Filter ───────────────────────────────────────────────────

    var appFilterMode: FilterMode
        get() = FilterMode.valueOf(
            prefs.getString("app_filter_mode", FilterMode.BLACKLIST.name) ?: FilterMode.BLACKLIST.name
        )
        set(value) = prefs.edit().putString("app_filter_mode", value.name).apply()

    fun getAppFilterList(): Map<String, AppFilterEntry> {
        val json = prefs.getString("app_filter_list", null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, AppFilterEntry>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun setAppFilterList(list: Map<String, AppFilterEntry>) {
        prefs.edit().putString("app_filter_list", gson.toJson(list)).apply()
    }

    fun isAppAllowed(packageName: String): Boolean {
        val filterList = getAppFilterList()
        val entry = filterList[packageName]
        return when (appFilterMode) {
            FilterMode.BLACKLIST -> entry?.enabled != true  // blocked if in list and enabled
            FilterMode.WHITELIST -> entry?.enabled == true  // allowed only if in list and enabled
        }
    }

    // ── Keyword Filter ───────────────────────────────────────────────

    fun getKeywordRules(): List<KeywordRule> {
        val json = prefs.getString("keyword_rules", null) ?: return emptyList()
        val type = object : TypeToken<List<KeywordRule>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setKeywordRules(rules: List<KeywordRule>) {
        prefs.edit().putString("keyword_rules", gson.toJson(rules)).apply()
    }

    // ── Notification Settings ────────────────────────────────────────

    var minPriority: Int
        get() = prefs.getInt("min_priority", -1) // PRIORITY_MIN
        set(value) = prefs.edit().putInt("min_priority", value).apply()

    var ignoreOngoing: Boolean
        get() = prefs.getBoolean("ignore_ongoing", true)
        set(value) = prefs.edit().putBoolean("ignore_ongoing", value).apply()

    var ignoreGroupSummary: Boolean
        get() = prefs.getBoolean("ignore_group_summary", true)
        set(value) = prefs.edit().putBoolean("ignore_group_summary", value).apply()

    var ignoreSilent: Boolean
        get() = prefs.getBoolean("ignore_silent", false)
        set(value) = prefs.edit().putBoolean("ignore_silent", value).apply()

    var ignoreEmpty: Boolean
        get() = prefs.getBoolean("ignore_empty", true)
        set(value) = prefs.edit().putBoolean("ignore_empty", value).apply()

    var duplicateTimeout: Long
        get() = prefs.getLong("duplicate_timeout", Constants.DUPLICATE_TIMEOUT_MS)
        set(value) = prefs.edit().putLong("duplicate_timeout", value).apply()

    var maxQueueSize: Int
        get() = prefs.getInt("max_queue_size", Constants.MAX_QUEUE_SIZE)
        set(value) = prefs.edit().putInt("max_queue_size", value).apply()

    var readAppName: Boolean
        get() = prefs.getBoolean("read_app_name", true)
        set(value) = prefs.edit().putBoolean("read_app_name", value).apply()

    var readTitle: Boolean
        get() = prefs.getBoolean("read_title", true)
        set(value) = prefs.edit().putBoolean("read_title", value).apply()

    var readContent: Boolean
        get() = prefs.getBoolean("read_content", true)
        set(value) = prefs.edit().putBoolean("read_content", value).apply()

    var messageFormat: String
        get() = prefs.getString("message_format", Constants.DEFAULT_MESSAGE_FORMAT)
            ?: Constants.DEFAULT_MESSAGE_FORMAT
        set(value) = prefs.edit().putString("message_format", value).apply()

    var maxTextLength: Int
        get() = prefs.getInt("max_text_length", Constants.MAX_TEXT_LENGTH)
        set(value) = prefs.edit().putInt("max_text_length", value).apply()

    // ── TTS / ElevenLabs ─────────────────────────────────────────────

    var elevenLabsApiKey: String
        get() = prefs.getString("elevenlabs_api_key", "") ?: ""
        set(value) = prefs.edit().putString("elevenlabs_api_key", value).apply()

    var elevenLabsVoiceId: String
        get() = prefs.getString("elevenlabs_voice_id", Constants.DEFAULT_VOICE_ID)
            ?: Constants.DEFAULT_VOICE_ID
        set(value) = prefs.edit().putString("elevenlabs_voice_id", value).apply()

    var elevenLabsVoiceName: String
        get() = prefs.getString("elevenlabs_voice_name", "Rachel") ?: "Rachel"
        set(value) = prefs.edit().putString("elevenlabs_voice_name", value).apply()

    var elevenLabsModel: String
        get() = prefs.getString("elevenlabs_model", Constants.DEFAULT_MODEL)
            ?: Constants.DEFAULT_MODEL
        set(value) = prefs.edit().putString("elevenlabs_model", value).apply()

    var ttsStability: Float
        get() = prefs.getFloat("tts_stability", Constants.DEFAULT_STABILITY)
        set(value) = prefs.edit().putFloat("tts_stability", value).apply()

    var ttsSimilarityBoost: Float
        get() = prefs.getFloat("tts_similarity", Constants.DEFAULT_SIMILARITY_BOOST)
        set(value) = prefs.edit().putFloat("tts_similarity", value).apply()

    var ttsStyle: Float
        get() = prefs.getFloat("tts_style", Constants.DEFAULT_STYLE)
        set(value) = prefs.edit().putFloat("tts_style", value).apply()

    var ttsSpeed: Float
        get() = prefs.getFloat("tts_speed", Constants.DEFAULT_SPEED)
        set(value) = prefs.edit().putFloat("tts_speed", value).apply()

    var fallbackToDeviceTts: Boolean
        get() = prefs.getBoolean("fallback_device_tts", true)
        set(value) = prefs.edit().putBoolean("fallback_device_tts", value).apply()

    var useDeviceTtsOnly: Boolean
        get() = prefs.getBoolean("use_device_tts_only", false)
        set(value) = prefs.edit().putBoolean("use_device_tts_only", value).apply()

    // ── Quiet Hours ──────────────────────────────────────────────────

    var quietHours: QuietHours
        get() {
            val json = prefs.getString("quiet_hours", null) ?: return QuietHours()
            return try {
                gson.fromJson(json, QuietHours::class.java) ?: QuietHours()
            } catch (e: Exception) {
                QuietHours()
            }
        }
        set(value) = prefs.edit().putString("quiet_hours", gson.toJson(value)).apply()

    fun isInQuietHours(): Boolean {
        val qh = quietHours
        if (!qh.enabled) return false
        val now = java.util.Calendar.getInstance()
        val currentMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
        val startMinutes = qh.startHour * 60 + qh.startMinute
        val endMinutes = qh.endHour * 60 + qh.endMinute
        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }

    // ── Notification Log ─────────────────────────────────────────────

    var logEnabled: Boolean
        get() = prefs.getBoolean("log_enabled", true)
        set(value) = prefs.edit().putBoolean("log_enabled", value).apply()

    var maxLogEntries: Int
        get() = prefs.getInt("max_log_entries", 200)
        set(value) = prefs.edit().putInt("max_log_entries", value).apply()

    fun getNotificationLog(): List<NotificationLogEntry> {
        val json = prefs.getString("notification_log", null) ?: return emptyList()
        val type = object : TypeToken<List<NotificationLogEntry>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addLogEntry(entry: NotificationLogEntry) {
        if (!logEnabled) return
        val log = getNotificationLog().toMutableList()
        log.add(0, entry)
        while (log.size > maxLogEntries) {
            log.removeAt(log.size - 1)
        }
        prefs.edit().putString("notification_log", gson.toJson(log)).apply()
    }

    fun clearLog() {
        prefs.edit().remove("notification_log").apply()
    }

    // ── Cached voices ────────────────────────────────────────────────

    fun getCachedVoices(): List<ElevenLabsVoice> {
        val json = prefs.getString("cached_voices", null) ?: return emptyList()
        val type = object : TypeToken<List<ElevenLabsVoice>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setCachedVoices(voices: List<ElevenLabsVoice>) {
        prefs.edit().putString("cached_voices", gson.toJson(voices)).apply()
    }
}
