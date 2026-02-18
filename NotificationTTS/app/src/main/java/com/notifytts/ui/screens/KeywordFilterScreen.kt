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
import androidx.compose.ui.unit.dp
import com.notifytts.data.KeywordAction
import com.notifytts.data.KeywordRule
import com.notifytts.data.PreferencesManager
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeywordFilterScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var rules by remember { mutableStateOf(prefs.getKeywordRules()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<KeywordRule?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Keyword & Regex Rules",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Create rules to filter notifications by content. Supports plain text and regular expressions. Hebrew text is fully supported.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Rule count and add button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${rules.size} rule${if (rules.size != 1) "s" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Rule")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Rules list
        if (rules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.FilterAlt,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No rules yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Tap + to add a keyword or regex rule",
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
                items(rules, key = { it.id }) { rule ->
                    RuleItem(
                        rule = rule,
                        onEdit = { editingRule = rule; showAddDialog = true },
                        onDelete = {
                            rules = rules.filter { it.id != rule.id }
                            prefs.setKeywordRules(rules)
                        }
                    )
                }
            }
        }
    }

    // Add/Edit dialog
    if (showAddDialog) {
        RuleDialog(
            existingRule = editingRule,
            onDismiss = {
                showAddDialog = false
                editingRule = null
            },
            onSave = { rule ->
                rules = if (editingRule != null) {
                    rules.map { if (it.id == editingRule!!.id) rule else it }
                } else {
                    rules + rule
                }
                prefs.setKeywordRules(rules)
                showAddDialog = false
                editingRule = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleItem(
    rule: KeywordRule,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Action icon
            val (icon, color) = when (rule.action) {
                KeywordAction.BLOCK -> Icons.Filled.Block to MaterialTheme.colorScheme.error
                KeywordAction.REQUIRE -> Icons.Filled.CheckCircle to MaterialTheme.colorScheme.primary
                KeywordAction.REPLACE -> Icons.Filled.FindReplace to MaterialTheme.colorScheme.tertiary
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = rule.pattern,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (rule.isRegex) {
                        Spacer(modifier = Modifier.width(8.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Regex", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
                Text(
                    text = when (rule.action) {
                        KeywordAction.BLOCK -> "Block if matched"
                        KeywordAction.REQUIRE -> "Required to match"
                        KeywordAction.REPLACE -> "Remove matched text"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Rule") },
            text = { Text("Delete rule \"${rule.pattern}\"?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleDialog(
    existingRule: KeywordRule?,
    onDismiss: () -> Unit,
    onSave: (KeywordRule) -> Unit
) {
    var pattern by remember { mutableStateOf(existingRule?.pattern ?: "") }
    var isRegex by remember { mutableStateOf(existingRule?.isRegex ?: false) }
    var action by remember { mutableStateOf(existingRule?.action ?: KeywordAction.BLOCK) }
    var regexError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingRule != null) "Edit Rule" else "Add Rule") },
        text = {
            Column {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = {
                        pattern = it
                        regexError = null
                    },
                    label = { Text("Pattern") },
                    placeholder = { Text("keyword or regex...") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = regexError != null,
                    supportingText = regexError?.let { { Text(it) } },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isRegex, onCheckedChange = { isRegex = it })
                    Text("Regular expression", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Action",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                KeywordAction.entries.forEach { actionOption ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = action == actionOption,
                            onClick = { action = actionOption }
                        )
                        Column {
                            Text(
                                text = when (actionOption) {
                                    KeywordAction.BLOCK -> "Block"
                                    KeywordAction.REQUIRE -> "Require"
                                    KeywordAction.REPLACE -> "Remove text"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = when (actionOption) {
                                    KeywordAction.BLOCK -> "Skip notification if pattern is found"
                                    KeywordAction.REQUIRE -> "Only read if pattern is found"
                                    KeywordAction.REPLACE -> "Remove matched text before reading"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pattern.isBlank()) return@Button
                    if (isRegex) {
                        try {
                            Regex(pattern)
                        } catch (e: Exception) {
                            regexError = "Invalid regex: ${e.message}"
                            return@Button
                        }
                    }
                    onSave(
                        KeywordRule(
                            id = existingRule?.id ?: UUID.randomUUID().toString(),
                            pattern = pattern,
                            isRegex = isRegex,
                            action = action
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
