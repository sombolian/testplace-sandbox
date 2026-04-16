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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.notifytts.data.GeminiVoice
import com.notifytts.data.PreferencesManager
import com.notifytts.service.GeminiTTSAPI
import com.notifytts.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TTSSettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var apiKey by remember { mutableStateOf(prefs.geminiApiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var voiceName by remember { mutableStateOf(prefs.geminiVoiceName) }
    var model by remember { mutableStateOf(prefs.geminiModel) }
    var speed by remember { mutableFloatStateOf(prefs.ttsSpeed) }
    var fallbackTts by remember { mutableStateOf(prefs.fallbackToDeviceTts) }
    var useDeviceOnly by remember { mutableStateOf(prefs.useDeviceTtsOnly) }

    var ttsTone by remember { mutableStateOf(prefs.ttsTone) }
    var customToneInstruction by remember { mutableStateOf(prefs.customToneInstruction) }

    var showVoicePicker by remember { mutableStateOf(false) }
    var apiStatus by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {
        // Gemini API Key
        SectionHeader("Gemini API")

        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        prefs.geminiApiKey = it
                    },
                    label = { Text("API Key") },
                    placeholder = { Text("Enter your Google AI Studio API key") },
                    visualTransformation = if (showApiKey) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle visibility"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Get your free API key from Google AI Studio",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        if (apiKey.isBlank()) return@OutlinedButton
                        isVerifying = true
                        apiStatus = "Verifying..."
                        Thread {
                            try {
                                val url = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models/${prefs.geminiModel}?key=$apiKey")
                                val conn = url.openConnection() as java.net.HttpURLConnection
                                conn.requestMethod = "GET"
                                conn.connectTimeout = 10_000
                                conn.readTimeout = 10_000
                                val code = conn.responseCode
                                conn.disconnect()
                                val msg = when (code) {
                                    200 -> "Valid! Gemini TTS is ready"
                                    401 -> "Error: Invalid API key"
                                    403 -> "Error: API key not authorized"
                                    404 -> "Error: TTS model not found"
                                    429 -> "Error: Rate limited - try later"
                                    else -> "Error: HTTP $code"
                                }
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    apiStatus = msg
                                    isVerifying = false
                                }
                            } catch (e: Exception) {
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    apiStatus = "Error: ${e.message ?: "Connection failed"}"
                                    isVerifying = false
                                }
                            }
                        }.start()
                    },
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isVerifying && apiKey.isNotBlank()
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Verify")
                }

                if (apiStatus.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = apiStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (apiStatus.startsWith("Valid")) MaterialTheme.colorScheme.primary
                        else if (apiStatus.startsWith("Error")) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Voice selection
        SectionHeader("Voice")

        SettingsCard {
            val selectedVoice = GeminiTTSAPI.VOICES.find { it.name == voiceName }
            SettingsClickable(
                title = "Voice",
                subtitle = if (selectedVoice != null) {
                    "${selectedVoice.name} - ${selectedVoice.description} (${selectedVoice.gender})"
                } else {
                    voiceName
                },
                onClick = { showVoicePicker = true }
            )

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            // Model selector
            var showModelMenu by remember { mutableStateOf(false) }
            Box {
                SettingsClickable(
                    title = "Model",
                    subtitle = when (model) {
                        "gemini-3.1-flash-tts-preview" -> "3.1 Flash (Latest, best control)"
                        "gemini-2.5-pro-preview-tts" -> "2.5 Pro (High fidelity)"
                        "gemini-2.5-flash-preview-tts" -> "2.5 Flash (Fast)"
                        else -> model
                    },
                    onClick = { showModelMenu = true }
                )
                DropdownMenu(expanded = showModelMenu, onDismissRequest = { showModelMenu = false }) {
                    listOf(
                        "gemini-3.1-flash-tts-preview" to "3.1 Flash (Latest, best control)",
                        "gemini-2.5-flash-preview-tts" to "2.5 Flash (Fast)",
                        "gemini-2.5-pro-preview-tts" to "2.5 Pro (High fidelity)",
                    ).forEach { (id, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                model = id
                                prefs.geminiModel = id
                                showModelMenu = false
                            },
                            trailingIcon = {
                                if (model == id) Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Voice parameters
        SectionHeader("Playback")

        SettingsCard {
            SliderSetting(
                title = "Speed",
                value = speed,
                onValueChange = {
                    speed = it
                    prefs.ttsSpeed = it
                },
                valueRange = 0.5f..2.0f,
                valueLabel = "%.1fx".format(speed)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tone / Mood
        SectionHeader("Tone & Mood")

        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Voice Tone",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Controls how the voice sounds. Ensures consistent delivery instead of random mood changes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Tone preset chips - 2 rows
                val toneEntries = GeminiTTSAPI.TONE_LABELS.entries.toList()
                for (row in toneEntries.chunked(3)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for ((key, label) in row) {
                            FilterChip(
                                selected = ttsTone == key,
                                onClick = {
                                    ttsTone = key
                                    prefs.ttsTone = key
                                },
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space if row has fewer than 3 items
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Custom instruction text field
                if (ttsTone == "custom") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customToneInstruction,
                        onValueChange = {
                            customToneInstruction = it
                            prefs.customToneInstruction = it
                        },
                        label = { Text("Custom tone instruction") },
                        placeholder = { Text("e.g., Speak in a cheerful, consistent tone...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Language info
        SectionHeader("Languages")

        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Automatic language detection",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Gemini TTS automatically detects the language of your text and speaks in that language. Supports 72+ languages including Hebrew, Arabic, English, French, German, Spanish, Japanese, Korean, Chinese, and many more.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Fallback settings
        SectionHeader("Fallback")

        SettingsCard {
            SettingsSwitch(
                title = "Use device TTS only",
                subtitle = "Skip Gemini, use Android built-in TTS",
                checked = useDeviceOnly,
                onCheckedChange = {
                    useDeviceOnly = it
                    prefs.useDeviceTtsOnly = it
                }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsSwitch(
                title = "Fallback to device TTS",
                subtitle = "Use device TTS if Gemini fails",
                checked = fallbackTts,
                onCheckedChange = {
                    fallbackTts = it
                    prefs.fallbackToDeviceTts = it
                },
                enabled = !useDeviceOnly
            )
        }
    }

    // Voice picker dialog
    if (showVoicePicker) {
        VoicePickerDialog(
            voices = GeminiTTSAPI.VOICES,
            selectedVoiceName = voiceName,
            onSelect = { voice ->
                voiceName = voice.name
                prefs.geminiVoiceName = voice.name
                showVoicePicker = false
            },
            onDismiss = { showVoicePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoicePickerDialog(
    voices: List<GeminiVoice>,
    selectedVoiceName: String,
    onSelect: (GeminiVoice) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterGender by remember { mutableStateOf("All") }
    val filteredVoices = remember(searchQuery, filterGender, voices) {
        voices.filter { voice ->
            val matchesSearch = searchQuery.isBlank() ||
                    voice.name.contains(searchQuery, ignoreCase = true) ||
                    voice.description.contains(searchQuery, ignoreCase = true)
            val matchesGender = filterGender == "All" || voice.gender == filterGender
            matchesSearch && matchesGender
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Voice") },
        text = {
            Column(modifier = Modifier.heightIn(max = 450.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search voices...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Gender filter chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Female", "Male").forEach { gender ->
                        FilterChip(
                            selected = filterGender == gender,
                            onClick = { filterGender = gender },
                            label = { Text(gender) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f, false)) {
                    items(filteredVoices.size) { index ->
                        val voice = filteredVoices[index]
                        Surface(
                            onClick = { onSelect(voice) },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (voice.name == selectedVoiceName)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = voice.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${voice.gender} - ${voice.description}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (voice.name == selectedVoiceName) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LazyColumn(
    modifier: Modifier = Modifier,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier,
        content = content
    )
}
