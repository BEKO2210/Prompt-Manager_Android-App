package com.promptmanager.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.promptmanager.domain.model.Placeholder
import com.promptmanager.domain.model.PlaceholderType
import com.promptmanager.util.PlaceholderParser

/**
 * Dynamischer Dialog zum Ausfüllen von Platzhaltern.
 *
 * Neue Features:
 * - Dropdown-Support für kommagetrennte Optionen
 * - Farbliche Markierung in Preview (rot=leer, grün=gefüllt)
 * - Vollständig scrollbare Preview
 *
 * @param promptTitle Titel des Prompts
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
            placeholders.forEach {
                this[it.key] = when (it.type) {
                    PlaceholderType.DROPDOWN -> "" // Leere Auswahl
                    else -> it.defaultValue
                }
            }
        }
    }

    // Preview-Segmente mit Farbmarkierung
    val previewSegments by remember {
        derivedStateOf {
            PlaceholderParser.createAnnotatedPreview(promptContent, values)
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
                        IconButton(
                            onClick = {
                                placeholders.forEach { ph ->
                                    values[ph.key] = when (ph.type) {
                                        PlaceholderType.DROPDOWN -> ""
                                        else -> ph.defaultValue
                                    }
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
                        Text(
                            text = "Dieser Prompt enthält keine Platzhalter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        placeholders.forEach { placeholder ->
                            PlaceholderInputField(
                                placeholder = placeholder,
                                value = values[placeholder.key] ?: "",
                                onValueChange = { values[placeholder.key] = it }
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // ========== FARBLICHE PREVIEW ==========
                        Text(
                            text = "Live-Vorschau:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp, max = 300.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp)
                            ) {
                                ColoredPreviewText(segments = previewSegments)
                            }
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
 * Eingabefeld für einen Platzhalter - unterstützt TEXT, MULTILINE_TEXT und DROPDOWN.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderInputField(
    placeholder: Placeholder,
    value: String,
    onValueChange: (String) -> Unit
) {
    when (placeholder.type) {
        PlaceholderType.DROPDOWN -> {
            // Dropdown-Menü
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = {},
                    label = { Text(placeholder.key) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = if (value.isBlank())
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    placeholder.options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.ifBlank { "(leer)" }) },
                            onClick = {
                                onValueChange(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        PlaceholderType.MULTILINE_TEXT -> {
            // Multiline TextField
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(placeholder.key) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8,
                supportingText = if (placeholder.defaultValue.isNotEmpty() && value != placeholder.defaultValue) {
                    { Text("Standard: ${placeholder.defaultValue.take(50)}...", style = MaterialTheme.typography.labelSmall) }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = if (value.isBlank())
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    else
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                )
            )
        }

        PlaceholderType.TEXT -> {
            // Normales TextField
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(placeholder.key) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = if (placeholder.defaultValue.isNotEmpty() && value != placeholder.defaultValue) {
                    { Text("Standard: ${placeholder.defaultValue}", style = MaterialTheme.typography.labelSmall) }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = if (value.isBlank())
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    else
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                )
            )
        }
    }
}

/**
 * Zeigt Preview-Text mit farblicher Markierung der Platzhalter.
 * Verwendet das Standard-Farbschema (rot=leer, grün=gefüllt).
 */
@Composable
private fun ColoredPreviewText(segments: List<PlaceholderParser.PreviewSegment>) {
    // Nutzt das Standard-Farbschema
    val colorScheme = PreviewColorScheme.default()

    val annotatedString = buildAnnotatedString {
        segments.forEach { segment ->
            if (segment.isPlaceholder) {
                val colors = getPreviewColors(segment.isFilled, colorScheme)
                withStyle(
                    style = SpanStyle(
                        background = colors.background,
                        color = colors.text
                    )
                ) {
                    append(segment.text)
                }
            } else {
                // Normaler Text
                append(segment.text)
            }
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodySmall
    )
}
