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
import com.notifytts.data.ElevenLabsVoice
import com.notifytts.data.PreferencesManager
import com.notifytts.service.ElevenLabsAPI
import com.notifytts.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TTSSettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val api = remember { ElevenLabsAPI(context.cacheDir) }
    val scope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf(prefs.elevenLabsApiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var voiceId by remember { mutableStateOf(prefs.elevenLabsVoiceId) }
    var voiceName by remember { mutableStateOf(prefs.elevenLabsVoiceName) }
    var model by remember { mutableStateOf(prefs.elevenLabsModel) }
    var stability by remember { mutableFloatStateOf(prefs.ttsStability) }
    var similarity by remember { mutableFloatStateOf(prefs.ttsSimilarityBoost) }
    var style by remember { mutableFloatStateOf(prefs.ttsStyle) }
    var speed by remember { mutableFloatStateOf(prefs.ttsSpeed) }
    var fallbackTts by remember { mutableStateOf(prefs.fallbackToDeviceTts) }
    var useDeviceOnly by remember { mutableStateOf(prefs.useDeviceTtsOnly) }

    var voices by remember { mutableStateOf(prefs.getCachedVoices()) }
    var isLoadingVoices by remember { mutableStateOf(false) }
    var showVoicePicker by remember { mutableStateOf(false) }
    var apiStatus by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {
        // ElevenLabs API Key
        SectionHeader("ElevenLabs API")

        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        prefs.elevenLabsApiKey = it
                    },
                    label = { Text("API Key") },
                    placeholder = { Text("Enter your ElevenLabs API key") },
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

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (apiKey.isBlank()) return@OutlinedButton
                            scope.launch {
                                apiStatus = "Checking..."
                                val result = api.getSubscriptionInfo(apiKey)
                                result.fold(
                                    onSuccess = { (used, limit) ->
                                        apiStatus = "Valid! Characters: $used / $limit"
                                    },
                                    onFailure = {
                                        apiStatus = "Error: ${it.message}"
                                    }
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Verify")
                    }

                    OutlinedButton(
                        onClick = {
                            if (apiKey.isBlank()) return@OutlinedButton
                            isLoadingVoices = true
                            scope.launch {
                                try {
                                    val result = api.getVoices(apiKey)
                                    result.fold(
                                        onSuccess = { voiceList ->
                                            voices = voiceList
                                            try {
                                                prefs.setCachedVoices(voiceList)
                                            } catch (_: Exception) {}
                                            showVoicePicker = true
                                        },
                                        onFailure = {
                                            apiStatus = "Failed to load voices: ${it.message}"
                                        }
                                    )
                                } catch (e: Exception) {
                                    apiStatus = "Error: ${e.message}"
                                } finally {
                                    isLoadingVoices = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoadingVoices
                    ) {
                        if (isLoadingVoices) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Load Voices")
                    }
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
            SettingsClickable(
                title = "Voice",
                subtitle = voiceName,
                onClick = {
                    if (voices.isNotEmpty()) {
                        showVoicePicker = true
                    } else if (apiKey.isNotBlank()) {
                        isLoadingVoices = true
                        scope.launch {
                            try {
                                val result = api.getVoices(apiKey)
                                result.fold(
                                    onSuccess = { voiceList ->
                                        voices = voiceList
                                        try { prefs.setCachedVoices(voiceList) } catch (_: Exception) {}
                                        showVoicePicker = true
                                    },
                                    onFailure = {
                                        apiStatus = "Failed to load voices"
                                    }
                                )
                            } catch (e: Exception) {
                                apiStatus = "Error: ${e.message}"
                            } finally {
                                isLoadingVoices = false
                            }
                        }
                    }
                }
            )

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            // Model selector
            var showModelMenu by remember { mutableStateOf(false) }
            Box {
                SettingsClickable(
                    title = "Model",
                    subtitle = when (model) {
                        "eleven_multilingual_v2" -> "Multilingual v2 (Hebrew)"
                        "eleven_turbo_v2_5" -> "Turbo v2.5 (Fast, multilingual)"
                        "eleven_turbo_v2" -> "Turbo v2 (Fastest, English)"
                        "eleven_multilingual_v1" -> "Multilingual v1"
                        "eleven_monolingual_v1" -> "Monolingual v1 (English)"
                        else -> model
                    },
                    onClick = { showModelMenu = true }
                )
                DropdownMenu(expanded = showModelMenu, onDismissRequest = { showModelMenu = false }) {
                    listOf(
                        "eleven_multilingual_v2" to "Multilingual v2 (Best for Hebrew)",
                        "eleven_turbo_v2_5" to "Turbo v2.5 (Fast, multilingual)",
                        "eleven_turbo_v2" to "Turbo v2 (Fastest, English only)",
                    ).forEach { (id, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                model = id
                                prefs.elevenLabsModel = id
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
        SectionHeader("Voice Parameters")

        SettingsCard {
            SliderSetting(
                title = "Stability",
                value = stability,
                onValueChange = {
                    stability = it
                    prefs.ttsStability = it
                }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            SliderSetting(
                title = "Similarity Boost",
                value = similarity,
                onValueChange = {
                    similarity = it
                    prefs.ttsSimilarityBoost = it
                }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            SliderSetting(
                title = "Style",
                value = style,
                onValueChange = {
                    style = it
                    prefs.ttsStyle = it
                }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
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

        // Fallback settings
        SectionHeader("Fallback")

        SettingsCard {
            SettingsSwitch(
                title = "Use device TTS only",
                subtitle = "Skip ElevenLabs, use Android built-in TTS",
                checked = useDeviceOnly,
                onCheckedChange = {
                    useDeviceOnly = it
                    prefs.useDeviceTtsOnly = it
                }
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsSwitch(
                title = "Fallback to device TTS",
                subtitle = "Use device TTS if ElevenLabs fails",
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
            voices = voices,
            selectedVoiceId = voiceId,
            onSelect = { voice ->
                voiceId = voice.voiceId
                voiceName = voice.name
                prefs.elevenLabsVoiceId = voice.voiceId
                prefs.elevenLabsVoiceName = voice.name
                showVoicePicker = false
            },
            onDismiss = { showVoicePicker = false }
        )
    }
}

@Composable
private fun VoicePickerDialog(
    voices: List<ElevenLabsVoice>,
    selectedVoiceId: String,
    onSelect: (ElevenLabsVoice) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredVoices = remember(searchQuery, voices) {
        if (searchQuery.isBlank()) voices
        else voices.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Voice") },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search voices...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f, false)) {
                    items(filteredVoices.size) { index ->
                        val voice = filteredVoices[index]
                        Surface(
                            onClick = { onSelect(voice) },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (voice.voiceId == selectedVoiceId)
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
                                    if (voice.category.isNotBlank()) {
                                        Text(
                                            text = voice.category,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (voice.voiceId == selectedVoiceId) {
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
