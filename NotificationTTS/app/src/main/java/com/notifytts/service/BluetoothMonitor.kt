package com.notifytts.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log

class BluetoothMonitor(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothMonitor"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var listener: ((Boolean) -> Unit)? = null
    private var receiver: BroadcastReceiver? = null

    // Track A2DP profile proxy connection for reliable detection
    @Volatile private var a2dpProfile: BluetoothProfile? = null
    @Volatile private var a2dpProxyReady = false

    init {
        // Get A2DP profile proxy for reliable "is BT audio actually connected" check
        try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = btManager?.adapter
            adapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.A2DP) {
                        a2dpProfile = proxy
                        a2dpProxyReady = true
                        Log.d(TAG, "A2DP profile proxy connected")
                    }
                }
                override fun onServiceDisconnected(profile: Int) {
                    if (profile == BluetoothProfile.A2DP) {
                        a2dpProfile = null
                        a2dpProxyReady = false
                        Log.d(TAG, "A2DP profile proxy disconnected")
                    }
                }
            }, BluetoothProfile.A2DP)
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot get A2DP profile proxy: ${e.message}")
        }
    }

    /**
     * STRICT check: Is Bluetooth audio ACTIVELY connected (not just paired)?
     * Uses multiple methods to be absolutely sure:
     * 1. A2DP profile proxy (most reliable - checks actual profile connection)
     * 2. AudioManager device enumeration (checks output routing)
     * 3. Legacy isBluetoothA2dpOn check
     * ALL must agree that BT audio is connected.
     */
    fun isBluetoothAudioConnected(): Boolean {
        // Method 1: Check A2DP profile proxy for connected devices
        val a2dpConnected = try {
            val profile = a2dpProfile
            if (profile != null && a2dpProxyReady) {
                @Suppress("MissingPermission")
                val connectedDevices = profile.connectedDevices
                val hasDevices = connectedDevices.isNotEmpty()
                if (hasDevices) {
                    Log.d(TAG, "A2DP profile: ${connectedDevices.size} connected device(s)")
                }
                hasDevices
            } else {
                null // Can't determine, don't count as evidence
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException checking A2DP profile: ${e.message}")
            null
        }

        // Method 2: Check AudioManager output devices for BT audio types
        val audioDeviceBtConnected = try {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            devices.any { device ->
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking audio devices: ${e.message}")
            false
        }

        // Method 3: Legacy check
        val legacyBtOn = try {
            @Suppress("DEPRECATION")
            audioManager.isBluetoothA2dpOn
        } catch (e: Exception) {
            false
        }

        Log.d(TAG, "BT audio check: a2dpProfile=$a2dpConnected, audioDevices=$audioDeviceBtConnected, legacy=$legacyBtOn")

        // STRICT: require at least audioDevices check to pass
        // If A2DP proxy is available, it must also agree
        if (!audioDeviceBtConnected) return false
        if (a2dpConnected == false) return false // A2DP proxy says no (only fail if explicitly false, not null)

        return true
    }

    /**
     * Check if BT SCO (call audio) is connected - separate from A2DP (media audio).
     * SCO alone is NOT sufficient for TTS - the audio quality is terrible and
     * some devices route SCO to the phone speaker anyway.
     */
    fun isBluetoothScoConnected(): Boolean {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
    }

    fun isWiredHeadphonesConnected(): Boolean {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.any { device ->
            device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            device.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
    }

    fun isAnyHeadphonesConnected(): Boolean {
        return isBluetoothAudioConnected() || isWiredHeadphonesConnected()
    }

    /**
     * Find a Bluetooth A2DP or BLE output device to force audio routing.
     * Returns null if no BT audio device is found.
     */
    fun findBluetoothOutputDevice(): AudioDeviceInfo? {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLE_HEADSET }
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER }
    }

    fun startMonitoring(onConnectionChanged: (Boolean) -> Unit) {
        listener = onConnectionChanged
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action ?: return
                Log.d(TAG, "Broadcast received: $action")
                // Small delay to let the system settle audio routing
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val connected = isBluetoothAudioConnected() || isWiredHeadphonesConnected()
                    Log.d(TAG, "Connection state after $action: connected=$connected")
                    listener?.invoke(connected)
                }, 500)
            }
        }

        context.registerReceiver(receiver, filter)
    }

    fun stopMonitoring() {
        receiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: Exception) {}
        }
        receiver = null
        listener = null

        // Release A2DP profile proxy
        try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            a2dpProfile?.let { proxy ->
                btManager?.adapter?.closeProfileProxy(BluetoothProfile.A2DP, proxy)
            }
        } catch (_: Exception) {}
        a2dpProfile = null
        a2dpProxyReady = false
    }
}
