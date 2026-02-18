package com.notifytts.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notifytts.data.PreferencesManager
import com.notifytts.service.BluetoothMonitor
import com.notifytts.service.NTTSNotificationListener
import com.notifytts.ui.components.SettingsCard
import com.notifytts.ui.components.StatusIndicator
import com.notifytts.ui.theme.StatusActive
import com.notifytts.ui.theme.StatusInactive
import com.notifytts.ui.theme.StatusWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTTSSettings: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val bluetoothMonitor = remember { BluetoothMonitor(context) }
    var serviceEnabled by remember { mutableStateOf(prefs.serviceEnabled) }
    var isListenerEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var isBtConnected by remember { mutableStateOf(bluetoothMonitor.isBluetoothAudioConnected()) }
    var hasApiKey by remember { mutableStateOf(prefs.elevenLabsApiKey.isNotBlank()) }
    var testText by remember { mutableStateOf("") }
    var showTestDialog by remember { mutableStateOf(false) }

    // Refresh state periodically
    LaunchedEffect(Unit) {
        while (true) {
            isListenerEnabled = isNotificationListenerEnabled(context)
            isBtConnected = bluetoothMonitor.isBluetoothAudioConnected()
            hasApiKey = prefs.elevenLabsApiKey.isNotBlank()
            serviceEnabled = prefs.serviceEnabled
            kotlinx.coroutines.delay(2000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        // Hero status card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (serviceEnabled && isListenerEnabled)
                    MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (serviceEnabled && isListenerEnabled)
                        Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = if (serviceEnabled && isListenerEnabled)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (serviceEnabled && isListenerEnabled) "Active" else "Inactive",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (!isListenerEnabled) "Notification access not granted"
                    else if (!serviceEnabled) "Service is disabled"
                    else if (!hasApiKey) "ElevenLabs API key not set"
                    else if (isBtConnected) "Listening via Bluetooth"
                    else "Waiting for Bluetooth connection",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Main toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Service",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = serviceEnabled,
                        onCheckedChange = {
                            serviceEnabled = it
                            prefs.serviceEnabled = it
                        }
                    )
                }
            }
        }

        // Status items
        SettingsCard {
            StatusRow(
                icon = Icons.Filled.Notifications,
                title = "Notification Access",
                isActive = isListenerEnabled,
                actionLabel = if (!isListenerEnabled) "Grant" else null,
                onAction = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            StatusRow(
                icon = Icons.Filled.Bluetooth,
                title = "Bluetooth Audio",
                isActive = isBtConnected,
                statusText = if (isBtConnected) "Connected" else "Disconnected"
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            StatusRow(
                icon = Icons.Filled.Key,
                title = "ElevenLabs API",
                isActive = hasApiKey,
                actionLabel = if (!hasApiKey) "Setup" else null,
                onAction = onNavigateToTTSSettings
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick stats
        val logEntries = remember { prefs.getNotificationLog() }
        val readCount = logEntries.count { it.wasRead }
        val totalCount = logEntries.size

        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = totalCount.toString(),
                    label = "Total"
                )
                StatItem(
                    value = readCount.toString(),
                    label = "Read"
                )
                StatItem(
                    value = (totalCount - readCount).toString(),
                    label = "Skipped"
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Test TTS button
        Button(
            onClick = { showTestDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Test TTS")
        }

        // Quick actions
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Security, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Notification Access Settings")
        }
    }

    // Test dialog
    if (showTestDialog) {
        AlertDialog(
            onDismissRequest = { showTestDialog = false },
            title = { Text("Test TTS") },
            text = {
                Column {
                    Text("Enter text to test (supports Hebrew):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = testText,
                        onValueChange = { testText = it },
                        placeholder = { Text("\u05E9\u05DC\u05D5\u05DD \u05E2\u05D5\u05DC\u05DD - Hello World") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (testText.isNotBlank()) {
                            val ttsManager = com.notifytts.service.TTSManager(context)
                            ttsManager.enqueue(testText)
                        }
                        showTestDialog = false
                    }
                ) {
                    Text("Speak")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTestDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isActive: Boolean,
    statusText: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) StatusActive else StatusInactive,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        } else {
            StatusIndicator(isActive = isActive)
            if (statusText != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun isNotificationListenerEnabled(context: Context): Boolean {
    val cn = ComponentName(context, com.notifytts.service.NTTSNotificationListener::class.java)
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(cn.flattenToString())
}
