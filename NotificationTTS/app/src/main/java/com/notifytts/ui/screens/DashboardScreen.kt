package com.notifytts.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notifytts.data.PreferencesManager
import com.notifytts.service.BluetoothMonitor
import com.notifytts.service.NTTSNotificationListener
import com.notifytts.service.TTSManager
import com.notifytts.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTTSSettings: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val bluetoothMonitor = remember { BluetoothMonitor(context) }
    val ttsManager = remember { TTSManager(context) }
    val scope = rememberCoroutineScope()
    var serviceEnabled by remember { mutableStateOf(prefs.serviceEnabled) }
    var isListenerEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var isBtConnected by remember { mutableStateOf(bluetoothMonitor.isBluetoothAudioConnected()) }
    var hasApiKey by remember { mutableStateOf(prefs.geminiApiKey.isNotBlank()) }
    var testText by remember { mutableStateOf("") }
    var showTestDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { ttsManager.destroy() }
    }

    LaunchedEffect(Unit) {
        while (true) {
            isListenerEnabled = isNotificationListenerEnabled(context)
            isBtConnected = bluetoothMonitor.isBluetoothAudioConnected()
            hasApiKey = prefs.geminiApiKey.isNotBlank()
            serviceEnabled = prefs.serviceEnabled
            kotlinx.coroutines.delay(2000)
        }
    }

    val isActive = serviceEnabled && isListenerEnabled
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        // Gradient Hero Card
        val gradientBrush = if (isActive) {
            Brush.linearGradient(
                colors = if (isDark) listOf(DarkGradientStart, DarkGradientEnd)
                else listOf(GradientStart, GradientEnd),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        } else {
            Brush.linearGradient(
                colors = if (isDark) listOf(Color(0xFF7F1D1D), Color(0xFF991B1B))
                else listOf(Color(0xFFEF4444), Color(0xFFDC2626)),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(gradientBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status icon in circle
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isActive) "Active" else "Inactive",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (!isListenerEnabled) "Notification access not granted"
                    else if (!serviceEnabled) "Service is disabled"
                    else if (!hasApiKey) "Gemini API key not set"
                    else if (isBtConnected) "Listening via Bluetooth"
                    else "Waiting for Bluetooth connection",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Service toggle - pill shape
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.2f),
                    onClick = {
                        serviceEnabled = !serviceEnabled
                        prefs.serviceEnabled = serviceEnabled
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Service",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = serviceEnabled,
                            onCheckedChange = {
                                serviceEnabled = it
                                prefs.serviceEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color.White.copy(alpha = 0.3f),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }

        // Status items
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            StatusRow(
                icon = Icons.Outlined.Notifications,
                title = "Notification Access",
                isActive = isListenerEnabled,
                actionLabel = if (!isListenerEnabled) "Grant" else null,
                onAction = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            )
            Divider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            StatusRow(
                icon = Icons.Outlined.Bluetooth,
                title = "Bluetooth Audio",
                isActive = isBtConnected,
                statusText = if (isBtConnected) "Connected" else "Disconnected"
            )
            Divider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            StatusRow(
                icon = Icons.Outlined.Key,
                title = "Gemini API",
                isActive = hasApiKey,
                actionLabel = if (!hasApiKey) "Setup" else null,
                onAction = onNavigateToTTSSettings
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick stats - three mini cards
        val logEntries = remember { prefs.getNotificationLog() }
        val readCount = logEntries.count { it.wasRead }
        val totalCount = logEntries.size

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MiniStatCard(
                value = totalCount.toString(),
                label = "Total",
                icon = Icons.Outlined.Notifications,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            MiniStatCard(
                value = readCount.toString(),
                label = "Read",
                icon = Icons.Outlined.VolumeUp,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            MiniStatCard(
                value = (totalCount - readCount).toString(),
                label = "Skipped",
                icon = Icons.Outlined.VolumeOff,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showTestDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test TTS", fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Permissions", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // Test dialog
    if (showTestDialog) {
        AlertDialog(
            onDismissRequest = { showTestDialog = false },
            title = { Text("Test TTS", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Enter text to test (any language):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = testText,
                        onValueChange = { testText = it },
                        placeholder = { Text("\u05E9\u05DC\u05D5\u05DD \u05E2\u05D5\u05DC\u05DD - Hello World") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (testText.isNotBlank()) {
                            ttsManager.enqueue(testText)
                            Toast.makeText(context, "Speaking...", Toast.LENGTH_SHORT).show()
                        }
                        showTestDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Speak")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTestDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun StatusRow(
    icon: ImageVector,
    title: String,
    isActive: Boolean,
    statusText: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isActive) StatusActive.copy(alpha = 0.12f)
                    else StatusInactive.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) StatusActive else StatusInactive,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onAction != null) {
            FilledTonalButton(
                onClick = onAction,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isActive) StatusActive else StatusInactive)
                )
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
}

@Composable
private fun MiniStatCard(
    value: String,
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun isNotificationListenerEnabled(context: Context): Boolean {
    val cn = ComponentName(context, NTTSNotificationListener::class.java)
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(cn.flattenToString())
}
