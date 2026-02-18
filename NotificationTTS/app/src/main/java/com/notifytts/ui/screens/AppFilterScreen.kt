package com.notifytts.ui.screens

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
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
import androidx.compose.ui.unit.dp
import com.notifytts.data.AppFilterEntry
import com.notifytts.data.FilterMode
import com.notifytts.data.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFilterScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var filterMode by remember { mutableStateOf(prefs.appFilterMode) }
    var filterList by remember { mutableStateOf(prefs.getAppFilterList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }

    // Load installed apps
    val installedApps = remember {
        val pm = context.packageManager
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                pm.getLaunchIntentForPackage(app.packageName) != null
            }
            .map { app ->
                AppFilterEntry(
                    packageName = app.packageName,
                    appName = pm.getApplicationLabel(app).toString(),
                    enabled = filterList[app.packageName]?.enabled ?: false
                )
            }
            .sortedBy { it.appName.lowercase() }
    }

    val filteredApps = remember(searchQuery, showSystemApps, installedApps) {
        installedApps.filter { app ->
            (searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter mode selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Filter Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterMode == FilterMode.BLACKLIST,
                        onClick = {
                            filterMode = FilterMode.BLACKLIST
                            prefs.appFilterMode = FilterMode.BLACKLIST
                        },
                        label = { Text("Blacklist") },
                        leadingIcon = if (filterMode == FilterMode.BLACKLIST) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = filterMode == FilterMode.WHITELIST,
                        onClick = {
                            filterMode = FilterMode.WHITELIST
                            prefs.appFilterMode = FilterMode.WHITELIST
                        },
                        label = { Text("Whitelist") },
                        leadingIcon = if (filterMode == FilterMode.WHITELIST) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (filterMode) {
                        FilterMode.BLACKLIST -> "Selected apps will be BLOCKED from reading"
                        FilterMode.WHITELIST -> "ONLY selected apps will be read aloud"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search apps...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = {
                val newList = filterList.toMutableMap()
                filteredApps.forEach { app ->
                    newList[app.packageName] = app.copy(enabled = true)
                }
                filterList = newList
                prefs.setAppFilterList(newList)
            }) {
                Text("Select All")
            }
            TextButton(onClick = {
                val newList = filterList.toMutableMap()
                filteredApps.forEach { app ->
                    newList[app.packageName] = app.copy(enabled = false)
                }
                filterList = newList
                prefs.setAppFilterList(newList)
            }) {
                Text("Deselect All")
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "${filterList.values.count { it.enabled }} selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }

        // App list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                val isSelected = filterList[app.packageName]?.enabled ?: false
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val newList = filterList.toMutableMap()
                        newList[app.packageName] = app.copy(enabled = !isSelected)
                        filterList = newList
                        prefs.setAppFilterList(newList)
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                val newList = filterList.toMutableMap()
                                newList[app.packageName] = app.copy(enabled = checked)
                                filterList = newList
                                prefs.setAppFilterList(newList)
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
