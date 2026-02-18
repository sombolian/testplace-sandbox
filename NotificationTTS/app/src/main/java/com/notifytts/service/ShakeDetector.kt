package com.notifytts.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var onShakeListener: (() -> Unit)? = null
    private var threshold = 15f
    private var lastShakeTime = 0L
    private val shakeCooldown = 1000L

    fun setThresholdFromSensitivity(sensitivity: Int) {
        // sensitivity 1-10: 1=hardest shake, 10=lightest shake
        threshold = 25f - (sensitivity.coerceIn(1, 10) * 2f)
    }

    fun start(onShake: () -> Unit) {
        onShakeListener = onShake
        accelerometer?.let {
            // Use SENSOR_DELAY_GAME for faster detection, especially important when screen is off
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        onShakeListener = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

        if (acceleration > threshold) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime > shakeCooldown) {
                lastShakeTime = now
                onShakeListener?.invoke()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
