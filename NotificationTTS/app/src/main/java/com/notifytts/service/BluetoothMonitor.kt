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

class BluetoothMonitor(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var listener: ((Boolean) -> Unit)? = null
    private var receiver: BroadcastReceiver? = null

    fun isBluetoothAudioConnected(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                devices.any { device ->
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn
            }
        } catch (e: SecurityException) {
            false
        }
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

    fun startMonitoring(onConnectionChanged: (Boolean) -> Unit) {
        listener = onConnectionChanged
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            // Fires when audio output switches from private (headphones) to public (speaker)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val connected = isBluetoothAudioConnected() || isWiredHeadphonesConnected()
                listener?.invoke(connected)
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
    }
}
