package com.promptmanager.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.promptmanager.util.PlaceholderParser

/**
 * Dynamischer Dialog zum Ausfüllen von Platzhaltern.
 *
 * Funktionsweise:
 * 1. Parsed den promptContent und extrahiert alle Platzhalter
 * 2. Erstellt für jeden Platzhalter ein TextField (Duplikate werden nur 1x angezeigt)
 * 3. Defaults werden aus dem Platzhalter übernommen
 * 4. Bei Bestätigung: Platzhalter werden ersetzt und finaler Prompt wird zurückgegeben
 *
 * @param promptTitle Titel des Prompts (für Anzeige)
 * @param promptContent Der Prompt-Text mit [Platzhaltern]
 * @param onDismiss Callback beim Abbrechen
 * @param onConfirm Callback beim Bestätigen mit ausgefülltem Prompt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderDialog(
    promptTitle: String,
    promptContent: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // Platzhalter extrahieren
    val placeholders = remember(promptContent) {
        PlaceholderParser.extractPlaceholders(promptContent)
    }

    // State: Map von Key -> Benutzereingabe
    val values = remember(placeholders) {
        mutableStateMapOf<String, String>().apply {
            placeholders.forEach { this[it.key] = it.defaultValue }
        }
    }

    // Preview des ausgefüllten Prompts
    val preview by remember {
        derivedStateOf {
            PlaceholderParser.createPreview(promptContent, values)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // ========== HEADER ==========
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Parameter für Prompt",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = promptTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        // "Defaults wiederherstellen"-Button
                        IconButton(
                            onClick = {
                                placeholders.forEach { ph ->
                                    values[ph.key] = ph.defaultValue
                                }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, "Defaults wiederherstellen")
                        }
                    }
                )

                // ========== FORMULAR ==========
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (placeholders.isEmpty()) {
                        // Kein Platzhalter -> Direkt kopieren
                        Text(
                            text = "Dieser Prompt enthält keine Platzhalter und kann direkt verwendet werden.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Für jeden Platzhalter ein Eingabefeld
                        placeholders.forEach { placeholder ->
                            PlaceholderInputField(
                                label = placeholder.key,
                                value = values[placeholder.key] ?: "",
                                onValueChange = { values[placeholder.key] = it },
                                defaultValue = placeholder.defaultValue,
                                isMultiLine = placeholder.isMultiLine
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // ========== PREVIEW ==========
                        Text(
                            text = "Vorschau:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = preview,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 10,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // ========== BUTTONS ==========
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Abbrechen")
                    }
                    Button(
                        onClick = {
                            val filledPrompt = PlaceholderParser.fillPlaceholders(
                                promptContent,
                                values
                            )
                            onConfirm(filledPrompt)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fertig & kopieren")
                    }
                }
            }
        }
    }
}

/**
 * Einzelnes Eingabefeld für einen Platzhalter.
 */
@Composable
private fun PlaceholderInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    defaultValue: String,
    isMultiLine: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            minLines = if (isMultiLine) 3 else 1,
            maxLines = if (isMultiLine) 8 else 1,
            supportingText = if (defaultValue.isNotEmpty() && value != defaultValue) {
                { Text("Standard: $defaultValue", style = MaterialTheme.typography.labelSmall) }
            } else null
        )
    }
}

/**
 * Minimale Variante ohne Preview (für einfachere Use-Cases).
 */
@Composable
fun SimplePlaceholderDialog(
    placeholders: List<com.promptmanager.domain.model.Placeholder>,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit
) {
    val values = remember {
        mutableStateMapOf<String, String>().apply {
            placeholders.forEach { this[it.key] = it.defaultValue }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Parameter eingeben") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                placeholders.forEach { placeholder ->
                    OutlinedTextField(
                        value = values[placeholder.key] ?: "",
                        onValueChange = { values[placeholder.key] = it },
                        label = { Text(placeholder.key) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = !placeholder.isMultiLine
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(values) }) {
                Text("Fertig")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
