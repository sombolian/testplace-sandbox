package com.notifytts.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notifytts.data.NotificationLogEntry
import com.notifytts.data.PreferencesManager
import com.notifytts.ui.theme.StatusActive
import com.notifytts.ui.theme.StatusInactive
import com.notifytts.ui.theme.StatusWarning
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationLogScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var logEntries by remember { mutableStateOf(prefs.getNotificationLog()) }
    var filterRead by remember { mutableStateOf<Boolean?>(null) } // null = show all
    var showClearDialog by remember { mutableStateOf(false) }

    val filteredEntries = remember(logEntries, filterRead) {
        when (filterRead) {
            true -> logEntries.filter { it.wasRead }
            false -> logEntries.filter { !it.wasRead }
            null -> logEntries
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filters and actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = filterRead == null,
                onClick = { filterRead = null },
                label = { Text("All (${logEntries.size})") }
            )
            FilterChip(
                selected = filterRead == true,
                onClick = { filterRead = if (filterRead == true) null else true },
                label = { Text("Read (${logEntries.count { it.wasRead }})") }
            )
            FilterChip(
                selected = filterRead == false,
                onClick = { filterRead = if (filterRead == false) null else false },
                label = { Text("Skipped (${logEntries.count { !it.wasRead }})") }
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = {
                logEntries = prefs.getNotificationLog()
            }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }

            IconButton(onClick = { showClearDialog = true }) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear log")
            }
        }

        if (filteredEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No entries",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Notification log will appear here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredEntries) { entry ->
                    LogEntryItem(entry = entry)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Log") },
            text = { Text("Delete all log entries? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    prefs.clearLog()
                    logEntries = emptyList()
                    showClearDialog = false
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogEntryItem(entry: NotificationLogEntry) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    var expanded by remember { mutableStateOf(false) }

    // Determine icon and color based on actual TTS status
    val (statusIcon, statusColor, statusLabel) = when (entry.ttsStatus) {
        "played" -> Triple(Icons.Filled.VolumeUp, StatusActive, "Played")
        "failed" -> Triple(Icons.Filled.ErrorOutline, StatusInactive, "Failed")
        "queued" -> Triple(Icons.Filled.HourglassTop, StatusWarning, "Queued")
        "skipped" -> Triple(Icons.Filled.VolumeOff, StatusInactive, "Skipped")
        else -> {
            // Legacy entries without ttsStatus
            if (entry.wasRead) Triple(Icons.Filled.VolumeUp, StatusActive, "Read")
            else Triple(Icons.Filled.VolumeOff, StatusInactive, "Skipped")
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = when (entry.ttsStatus) {
                "played" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                "failed" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                "queued" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                else -> {
                    if (entry.wasRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                }
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = statusLabel,
                    modifier = Modifier.size(18.dp),
                    tint = statusColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = entry.appName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                // Show TTS status badge
                if (entry.ttsStatus != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = dateFormat.format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (entry.title.isNotBlank()) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 26.dp)
                )
            }

            if (entry.text.isNotBlank()) {
                Text(
                    text = entry.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 26.dp)
                )
            }

            // Show skip reason for skipped entries
            if (!entry.wasRead && entry.skipReason != null) {
                Text(
                    text = "Skipped: ${entry.skipReason}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 26.dp, top = 2.dp)
                )
            }

            // Show TTS error for failed entries
            if (entry.ttsStatus == "failed" && entry.ttsError != null) {
                Text(
                    text = "TTS Error: ${entry.ttsError}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 26.dp, top = 2.dp)
                )
            }

            // Show info note when Gemini failed but fallback was used
            if (entry.ttsStatus == "played" && entry.ttsError != null) {
                Text(
                    text = "Note: ${entry.ttsError}",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusWarning,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 26.dp, top = 2.dp)
                )
            }
        }
    }
}
