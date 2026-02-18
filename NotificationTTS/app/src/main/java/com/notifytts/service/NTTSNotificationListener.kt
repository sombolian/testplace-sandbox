package com.notifytts.service

import android.app.NotificationManager
import android.content.Context
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.notifytts.data.NotificationLogEntry
import com.notifytts.data.PreferencesManager

class NTTSNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "NTTSListener"
        var isRunning = false
            private set
    }

    private lateinit var prefs: PreferencesManager
    private lateinit var ttsManager: TTSManager
    private lateinit var bluetoothMonitor: BluetoothMonitor
    private lateinit var processor: NotificationProcessor
    private var shakeDetector: ShakeDetector? = null

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(this)
        ttsManager = TTSManager(this)
        bluetoothMonitor = BluetoothMonitor(this)
        processor = NotificationProcessor(prefs, packageManager)

        bluetoothMonitor.startMonitoring { connected ->
            Log.d(TAG, "Audio device connection changed: $connected")
        }

        startShakeDetector()

        isRunning = true
        Log.i(TAG, "Notification listener started")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        shakeDetector?.stop()
        ttsManager.destroy()
        bluetoothMonitor.stopMonitoring()
        Log.i(TAG, "Notification listener stopped")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        if (!prefs.serviceEnabled) return

        // Ignore our own notifications
        if (sbn.packageName == packageName) return

        // Check Do Not Disturb
        if (prefs.respectDoNotDisturb && isDoNotDisturbActive()) return

        // Check quiet hours
        if (prefs.isInQuietHours()) return

        // Check screen-off only
        if (prefs.screenOffOnly && isScreenOn()) return

        // Check audio output
        if (!shouldSpeak()) return

        // Re-check shake settings (picks up changes without service restart)
        refreshShakeDetector()

        // Process notification
        val result = processor.process(sbn)

        if (result.shouldRead && result.text.isNotBlank()) {
            // Log as "queued" - will be updated with actual TTS result
            val logEntryId = System.currentTimeMillis()
            prefs.addLogEntry(
                NotificationLogEntry(
                    id = logEntryId,
                    appPackage = sbn.packageName,
                    appName = result.appName,
                    title = result.title,
                    text = result.content,
                    wasRead = true,
                    skipReason = null,
                    ttsStatus = "queued"
                )
            )

            Log.d(TAG, "Reading notification from ${result.appName}: ${result.text.take(50)}")
            ttsManager.enqueue(result.text, logEntryId) { success, errorMsg ->
                // Update the log entry with the actual TTS result
                val status = if (success) "played" else "failed"
                Log.d(TAG, "TTS result for ${result.appName}: $status${if (errorMsg != null) " ($errorMsg)" else ""}")
                prefs.updateLogEntryStatus(logEntryId, status, errorMsg)
            }
        } else {
            // Log skipped notification
            prefs.addLogEntry(
                NotificationLogEntry(
                    appPackage = sbn.packageName,
                    appName = result.appName,
                    title = result.title,
                    text = result.content,
                    wasRead = false,
                    skipReason = result.skipReason,
                    ttsStatus = "skipped"
                )
            )
            Log.d(TAG, "Skipped notification from ${result.appName}: ${result.skipReason}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Could optionally stop reading if notification was dismissed
    }

    private var shakeEnabled = false

    private fun startShakeDetector() {
        if (prefs.shakeToPause) {
            shakeEnabled = true
            shakeDetector = ShakeDetector(this).apply {
                setThresholdFromSensitivity(prefs.shakeSensitivity)
                start {
                    // Only react when audio is actively playing (not during API call or idle)
                    if (ttsManager.isAudioPlaying()) {
                        Log.d(TAG, "Shake detected - pausing TTS")
                        ttsManager.pause()
                        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                        vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                        android.os.Handler(mainLooper).postDelayed({
                            ttsManager.resume()
                        }, 5000)
                    }
                }
            }
        }
    }

    private fun refreshShakeDetector() {
        val shouldBeEnabled = prefs.shakeToPause
        val sensitivityChanged = shakeDetector != null && prefs.shakeSensitivity != prefs.shakeSensitivity
        if (shouldBeEnabled && !shakeEnabled) {
            // User enabled shake after service started
            shakeDetector?.stop()
            startShakeDetector()
        } else if (!shouldBeEnabled && shakeEnabled) {
            // User disabled shake
            shakeDetector?.stop()
            shakeDetector = null
            shakeEnabled = false
        } else if (shouldBeEnabled && shakeEnabled) {
            // Update sensitivity
            shakeDetector?.setThresholdFromSensitivity(prefs.shakeSensitivity)
        }
    }

    private fun shouldSpeak(): Boolean {
        val btConnected = bluetoothMonitor.isBluetoothAudioConnected()
        val wiredConnected = bluetoothMonitor.isWiredHeadphonesConnected()

        if (prefs.alsoSpeaker) return true
        if (prefs.onlyWhenBluetooth && btConnected) return true
        if (prefs.alsoWiredHeadphones && wiredConnected) return true
        if (!prefs.onlyWhenBluetooth && !prefs.alsoWiredHeadphones && !prefs.alsoSpeaker) {
            // If no output restrictions are set, default to bluetooth-only behavior
            return btConnected
        }
        return false
    }

    private fun isDoNotDisturbActive(): Boolean {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return try {
            notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        } catch (e: Exception) {
            false
        }
    }

    private fun isScreenOn(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isInteractive
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isRunning = true
        Log.i(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isRunning = false
        Log.i(TAG, "Notification listener disconnected")
    }
}
