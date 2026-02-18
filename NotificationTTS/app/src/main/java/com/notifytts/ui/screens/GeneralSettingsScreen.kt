package com.notifytts.ui.screens

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
import androidx.compose.ui.unit.dp
import com.notifytts.data.PreferencesManager
import com.notifytts.data.QuietHours
import com.notifytts.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onNavigateToTTSSettings: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }

    var onlyBluetooth by remember { mutableStateOf(prefs.onlyWhenBluetooth) }
    var alsoWired by remember { mutableStateOf(prefs.alsoWiredHeadphones) }
    var alsoSpeaker by remember { mutableStateOf(prefs.alsoSpeaker) }
    var respectDnd by remember { mutableStateOf(prefs.respectDoNotDisturb) }
    var screenOffOnly by remember { mutableStateOf(prefs.screenOffOnly) }
    var shakeToPause by remember { mutableStateOf(prefs.shakeToPause) }

    var ignoreOngoing by remember { mutableStateOf(prefs.ignoreOngoing) }
    var ignoreGroupSummary by remember { mutableStateOf(prefs.ignoreGroupSummary) }
    var ignoreSilent by remember { mutableStateOf(prefs.ignoreSilent) }
    var ignoreEmpty by remember { mutableStateOf(prefs.ignoreEmpty) }
    var readAppName by remember { mutableStateOf(prefs.readAppName) }
    var readTitle by remember { mutableStateOf(prefs.readTitle) }
    var readContent by remember { mutableStateOf(prefs.readContent) }

    var maxTextLength by remember { mutableIntStateOf(prefs.maxTextLength) }
    var duplicateTimeout by remember { mutableLongStateOf(prefs.duplicateTimeout) }
    var maxQueueSize by remember { mutableIntStateOf(prefs.maxQueueSize) }
    var messageFormat by remember { mutableStateOf(prefs.messageFormat) }

    var quietHours by remember { mutableStateOf(prefs.quietHours) }
    var logEnabled by remember { mutableStateOf(prefs.logEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {
        // Audio output
        SectionHeader("Audio Output")

        SettingsCard {
            SettingsSwitch(
                title = "Bluetooth headphones only",
                subtitle = "Only read when Bluetooth audio is connected",
                checked = onlyBluetooth,
                onCheckedChange = {
                    onlyBluetooth = it
                    prefs.onlyWhenBluetooth = it
                }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsSwitch(
                title = "Also wired headphones",
                subtitle = "Read when wired headphones/USB headset are connected",
                checked = alsoWired,
                onCheckedChange = {
                    alsoWired = it
                    prefs.alsoWiredHeadphones = it
                }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsSwitch(
                title = "Also speaker",
                subtitle = "Read even without headphones (speaker mode)",
                checked = alsoSpeaker,
                onCheckedChange = {
                    alsoSpeaker = it
                    prefs.alsoSpeaker = it
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Behavior
        SectionHeader("Behavior")

        SettingsCard {
            SettingsSwitch(
                title = "Respect Do Not Disturb",
                subtitle = "Don't read when DND is active",
                checked = respectDnd,
                onCheckedChange = {
                    respectDnd = it
                    prefs.respectDoNotDisturb = it
                }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsSwitch(
                title = "Screen off only",
                subtitle = "Only read when screen is off",
                checked = screenOffOnly,
                onCheckedChange = {
                    screenOffOnly = it
                    prefs.screenOffOnly = it
                }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsSwitch(
                title = "Shake to pause",
                subtitle = "Shake device to pause current readout",
                checked = shakeToPause,
                onCheckedChange = {
                    shakeToPause = it
                    prefs.shakeToPause = it
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Notification filtering
        SectionHeader("Notification Filtering")

        SettingsCard {
            SettingsSwitch(
                title = "Ignore ongoing notifications",
                subtitle = "Skip persistent/ongoing notifications",
                checked = ignoreOngoing,
                onCheckedChange = {
                    ignoreOngoing = it
                    prefs.ignoreOngoing = it
                }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsSwitch(
                title = "Ignore group summaries",
                subtitle = "Skip notification group summary entries",
                checked = ignoreGroupSummary,
                onCheckedChange = {
                    ignoreGroupSummary = it
                    prefs.ignoreGroupSummary = it
                }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsSwitch(
                title = "Ignore silent notifications",
                subtitle = "Skip notifications with no sound",
                checked = ignoreSilent,
                onCheckedChange = {
                    ignoreSilent = it
                    prefs.ignoreSilent = it
                }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsSwitch(
                title = "Ignore empty notifications",
                subtitle = "Skip notifications with no text content",
                checked = ignoreEmpty,
                onCheckedChange = {
                    ignoreEmpty = it
                    prefs.ignoreEmpty = it
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Message format
        SectionHeader("Message Format")

        SettingsCard {
            SettingsSwitch(
                title = "Read app name",
                checked = readAppName,
                onCheckedChange = {
                    readAppName = it
                    prefs.readAppName = it
                }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsSwitch(
                title = "Read title",
                checked = readTitle,
                onCheckedChange = {
                    readTitle = it
                    prefs.readTitle = it
                }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsSwitch(
                title = "Read content",
                checked = readContent,
                onCheckedChange = {
                    readContent = it
                    prefs.readContent = it
                }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            Column(modifier = Modifier.padding(16.dp)) {
                Text("Message template", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Variables: {app}, {title}, {text}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = messageFormat,
                    onValueChange = {
                        messageFormat = it
                        prefs.messageFormat = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Limits
        SectionHeader("Limits")

        SettingsCard {
            SliderSetting(
                title = "Max text length",
                value = maxTextLength.toFloat(),
                onValueChange = {
                    maxTextLength = it.toInt()
                    prefs.maxTextLength = it.toInt()
                },
                valueRange = 50f..2000f,
                valueLabel = "$maxTextLength chars"
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            SliderSetting(
                title = "Duplicate timeout",
                value = (duplicateTimeout / 1000f),
                onValueChange = {
                    duplicateTimeout = (it * 1000).toLong()
                    prefs.duplicateTimeout = duplicateTimeout
                },
                valueRange = 0f..120f,
                valueLabel = "${duplicateTimeout / 1000}s"
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            SliderSetting(
                title = "Max queue size",
                value = maxQueueSize.toFloat(),
                onValueChange = {
                    maxQueueSize = it.toInt()
                    prefs.maxQueueSize = it.toInt()
                },
                valueRange = 1f..50f,
                valueLabel = "$maxQueueSize"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quiet hours
        SectionHeader("Quiet Hours")

        SettingsCard {
            SettingsSwitch(
                title = "Enable quiet hours",
                subtitle = if (quietHours.enabled)
                    "Silent from ${"%02d:%02d".format(quietHours.startHour, quietHours.startMinute)} to ${"%02d:%02d".format(quietHours.endHour, quietHours.endMinute)}"
                else "Disabled",
                checked = quietHours.enabled,
                onCheckedChange = {
                    quietHours = quietHours.copy(enabled = it)
                    prefs.quietHours = quietHours
                }
            )

            if (quietHours.enabled) {
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TimePickerButton(
                        label = "Start",
                        hour = quietHours.startHour,
                        minute = quietHours.startMinute,
                        onTimeSelected = { h, m ->
                            quietHours = quietHours.copy(startHour = h, startMinute = m)
                            prefs.quietHours = quietHours
                        }
                    )
                    TimePickerButton(
                        label = "End",
                        hour = quietHours.endHour,
                        minute = quietHours.endMinute,
                        onTimeSelected = { h, m ->
                            quietHours = quietHours.copy(endHour = h, endMinute = m)
                            prefs.quietHours = quietHours
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Logging
        SectionHeader("Logging")

        SettingsCard {
            SettingsSwitch(
                title = "Enable notification log",
                subtitle = "Keep a history of processed notifications",
                checked = logEnabled,
                onCheckedChange = {
                    logEnabled = it
                    prefs.logEnabled = it
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // TTS Settings link
        SettingsCard {
            SettingsClickable(
                title = "TTS Engine Settings",
                subtitle = "Configure ElevenLabs voice and parameters",
                onClick = onNavigateToTTSSettings,
                trailing = {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Notification TTS",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "v1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Powered by ElevenLabs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TimePickerButton(
    label: String,
    hour: Int,
    minute: Int,
    onTimeSelected: (Int, Int) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableIntStateOf(hour) }
    var selectedMinute by remember { mutableIntStateOf(minute) }

    OutlinedButton(
        onClick = { showPicker = true },
        shape = RoundedCornerShape(8.dp)
    ) {
        Text("$label: ${"%02d:%02d".format(hour, minute)}")
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("$label Time") },
            text = {
                Column {
                    // Simple hour/minute sliders
                    Text("Hour: ${"%02d".format(selectedHour)}")
                    Slider(
                        value = selectedHour.toFloat(),
                        onValueChange = { selectedHour = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 22
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Minute: ${"%02d".format(selectedMinute)}")
                    Slider(
                        value = selectedMinute.toFloat(),
                        onValueChange = { selectedMinute = it.toInt() },
                        valueRange = 0f..59f,
                        steps = 58
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onTimeSelected(selectedHour, selectedMinute)
                    showPicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
