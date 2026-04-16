package com.notifytts.util

object Constants {
    const val GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models"
    const val DEFAULT_VOICE_NAME = "Kore" // Neutral, professional female voice
    const val DEFAULT_GEMINI_MODEL = "gemini-3.1-flash-tts-preview" // Latest, best controllability
    const val DEFAULT_SPEED = 1.0f
    const val MAX_TEXT_LENGTH = 500
    const val DUPLICATE_TIMEOUT_MS = 30000L
    const val MAX_QUEUE_SIZE = 20
    const val DEFAULT_MESSAGE_FORMAT = "{app}: {title}. {text}"

    // Audio
    const val AUDIO_STREAM_MUSIC = 3 // AudioManager.STREAM_MUSIC

    // Notification channels
    const val CHANNEL_SERVICE = "ntts_service"
    const val CHANNEL_ERRORS = "ntts_errors"
}
