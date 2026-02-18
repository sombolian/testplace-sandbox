package com.notifytts.util

object Constants {
    const val ELEVENLABS_BASE_URL = "https://api.elevenlabs.io/v1"
    const val DEFAULT_VOICE_ID = "21m00Tcm4TlvDq8ikWAM" // Rachel - default voice
    const val DEFAULT_MODEL = "eleven_multilingual_v2" // Supports Hebrew
    const val DEFAULT_STABILITY = 0.5f
    const val DEFAULT_SIMILARITY_BOOST = 0.75f
    const val DEFAULT_STYLE = 0.0f
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
