package com.promptmanager.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.promptmanager.presentation.components.PlaceholderDialog
import com.promptmanager.presentation.viewmodel.PromptDetailViewModel
import com.promptmanager.util.PlaceholderParser

/**
 * Detail-Screen: Prompt bearbeiten oder neu erstellen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptDetailScreen(
    viewModel: PromptDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val title by viewModel.title.collectAsState()
    val description by viewModel.description.collectAsState()
    val content by viewModel.content.collectAsState()
    val category by viewModel.category.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val placeholders by viewModel.placeholders.collectAsState()
    val warnings by viewModel.validationWarnings.collectAsState()

    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPlaceholderDialog by remember { mutableStateOf(false) }

    // Erfolgsmeldung -> zurück navigieren
    LaunchedEffect(uiState) {
        if (uiState is PromptDetailViewModel.UiState.Success) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (title.isEmpty()) "Neuer Prompt" else title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorit",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (uiState is PromptDetailViewModel.UiState.Editing) {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Menü")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Duplizieren") },
                                onClick = {
                                    viewModel.duplicatePrompt { newId ->
                                        // TODO: Navigate to new prompt
                                    }
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.FileCopy, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Löschen") },
                                onClick = {
                                    showDeleteDialog = true
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null) }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ========== FORMULAR ==========

            // Titel
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.setTitle(it) },
                label = { Text("Titel *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = title.isBlank()
            )

            // Beschreibung
            OutlinedTextField(
                value = description,
                onValueChange = { viewModel.setDescription(it) },
                label = { Text("Beschreibung (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            // Kategorie
            var expandedCategories by remember { mutableStateOf(false) }
            val predefinedCategories = listOf(
                "E-Mail", "Entwicklung", "Social Media", "Business", "Bildung", "Sonstiges"
            )
            ExposedDropdownMenuBox(
                expanded = expandedCategories,
                onExpandedChange = { expandedCategories = it }
            ) {
                OutlinedTextField(
                    value = category ?: "",
                    onValueChange = { viewModel.setCategory(it.takeIf { it.isNotBlank() }) },
                    label = { Text("Kategorie (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategories) },
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = expandedCategories,
                    onDismissRequest = { expandedCategories = false }
                ) {
                    predefinedCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                viewModel.setCategory(cat)
                                expandedCategories = false
                            }
                        )
                    }
                }
            }

            // Prompt-Content
            OutlinedTextField(
                value = content,
                onValueChange = { viewModel.setContent(it) },
                label = { Text("Prompt-Text *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 8,
                maxLines = 20,
                isError = content.isBlank(),
                supportingText = {
                    Text("Verwende [Label=Default] für Platzhalter")
                }
            )

            // ========== PLATZHALTER-PREVIEW ==========
            if (placeholders.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Gefundene Platzhalter:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        placeholders.forEach { placeholder ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• ${placeholder.key}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (placeholder.defaultValue.isNotEmpty()) {
                                    Text(
                                        text = "= \"${placeholder.defaultValue.take(30)}${if (placeholder.defaultValue.length > 30) "..." else ""}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ========== WARNUNGEN ==========
            if (warnings.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "⚠️ Warnungen:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        warnings.forEach { warning ->
                            Text(
                                text = "• $warning",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // ========== BUTTONS ==========
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Speichern
                Button(
                    onClick = { viewModel.savePrompt() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = title.isNotBlank() && content.isNotBlank()
                ) {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Speichern")
                }

                // Testen & kopieren
                OutlinedButton(
                    onClick = { showPlaceholderDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = content.isNotBlank()
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Testen & kopieren")
                }
            }

            // Error-Meldung
            if (uiState is PromptDetailViewModel.UiState.Error) {
                Text(
                    text = (uiState as PromptDetailViewModel.UiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    // ========== DIALOGE ==========

    // Platzhalter-Dialog
    if (showPlaceholderDialog) {
        PlaceholderDialog(
            promptTitle = title.ifBlank { "Prompt-Test" },
            promptContent = content,
            onDismiss = { showPlaceholderDialog = false },
            onConfirm = { filledPrompt ->
                copyToClipboard(context, title, filledPrompt)
                viewModel.markAsUsed()
                showPlaceholderDialog = false
            }
        )
    }

    // Lösch-Bestätigung
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Prompt löschen?") },
            text = { Text("Möchtest du \"$title\" wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePrompt {
                            onNavigateBack()
                        }
                        showDeleteDialog = false
                    }
                ) {
                    Text("Löschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

/**
 * Helper: Text in Zwischenablage kopieren.
 */
private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    // TODO: Snackbar anzeigen
}
