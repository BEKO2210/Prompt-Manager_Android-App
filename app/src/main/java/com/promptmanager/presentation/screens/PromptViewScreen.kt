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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.promptmanager.data.database.PromptEntity
import com.promptmanager.data.repository.PromptRepository
import com.promptmanager.presentation.components.PlaceholderDialog
import com.promptmanager.presentation.components.VersionHistoryBottomSheet
import com.promptmanager.util.PlaceholderParser
import kotlinx.coroutines.launch

/**
 * ReadOnly-Ansicht eines Prompts.
 *
 * Features:
 * - Zeigt alle Prompt-Informationen an (nicht editierbar)
 * - "Bearbeiten"-FAB um in Edit-Mode zu wechseln
 * - "Prompt nutzen"-Button
 * - Versions-Badge mit History-Zugriff
 * - Favoriten-Toggle
 * - Duplizieren mit Bestätigung
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptViewScreen(
    promptId: Long,
    repository: PromptRepository,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var prompt by remember { mutableStateOf<PromptEntity?>(null) }
    var versionCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    var showPlaceholderDialog by remember { mutableStateOf(false) }
    var showVersionHistory by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Prompt laden
    LaunchedEffect(promptId) {
        scope.launch {
            prompt = repository.getPromptById(promptId)
            versionCount = repository.getVersionCount(promptId)
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = prompt?.title ?: "Laden...",
                            style = MaterialTheme.typography.titleLarge
                        )
                        // Version Badge
                        if (prompt != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "v${prompt!!.version}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (versionCount > 0) {
                                    TextButton(
                                        onClick = { showVersionHistory = true },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = "$versionCount Versionen",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    // Favorit Toggle
                    if (prompt != null) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    repository.toggleFavorite(promptId, prompt!!.isFavorite)
                                    prompt = prompt!!.copy(isFavorite = !prompt!!.isFavorite)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (prompt!!.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorit",
                                tint = if (prompt!!.isFavorite) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Menü
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
                                showDuplicateDialog = true
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
            )
        },
        floatingActionButton = {
            // Bearbeiten-FAB
            FloatingActionButton(
                onClick = { onNavigateToEdit(promptId) }
            ) {
                Icon(Icons.Default.Edit, "Bearbeiten")
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (prompt == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Prompt nicht gefunden")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Kategorie
                if (prompt!!.category != null) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(prompt!!.category!!) }
                    )
                }

                // Beschreibung
                if (!prompt!!.description.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = prompt!!.description!!,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Prompt Content
                Text(
                    text = "Prompt-Text:",
                    style = MaterialTheme.typography.titleSmall
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = prompt!!.content,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Platzhalter-Info
                val placeholders = PlaceholderParser.extractPlaceholders(prompt!!.content)
                if (placeholders.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Enthält ${placeholders.size} Platzhalter:",
                                style = MaterialTheme.typography.labelMedium
                            )
                            placeholders.forEach { placeholder ->
                                Text(
                                    text = "• ${placeholder.key}${if (placeholder.type.name == "DROPDOWN") " (Dropdown)" else ""}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Nutzungsstatistik
                Text(
                    text = "${prompt!!.usageCount}× verwendet",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Prompt nutzen Button
                Button(
                    onClick = {
                        if (PlaceholderParser.hasPlaceholders(prompt!!.content)) {
                            showPlaceholderDialog = true
                        } else {
                            copyToClipboard(context, prompt!!.title, prompt!!.content)
                            scope.launch { repository.markPromptAsUsed(promptId) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Prompt nutzen")
                }
            }
        }
    }

    // Dialoge
    if (showPlaceholderDialog && prompt != null) {
        PlaceholderDialog(
            promptTitle = prompt!!.title,
            promptContent = prompt!!.content,
            onDismiss = { showPlaceholderDialog = false },
            onConfirm = { filledPrompt ->
                copyToClipboard(context, prompt!!.title, filledPrompt)
                scope.launch { repository.markPromptAsUsed(promptId) }
                showPlaceholderDialog = false
            }
        )
    }

    if (showVersionHistory && prompt != null) {
        VersionHistoryBottomSheet(
            promptId = promptId,
            repository = repository,
            onDismiss = { showVersionHistory = false },
            onVersionSelected = { versionId ->
                // Navigate to version view
                showVersionHistory = false
                // TODO: Navigate to selected version
            }
        )
    }

    if (showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            title = { Text("Prompt duplizieren?") },
            text = { Text("Möchtest du \"${prompt?.title}\" duplizieren? Es wird eine unabhängige Kopie erstellt.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.duplicatePrompt(promptId)
                            showDuplicateDialog = false
                            onNavigateBack()
                        }
                    }
                ) {
                    Text("Duplizieren")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Prompt löschen?") },
            text = { Text("Möchtest du \"${prompt?.title}\" wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.deletePromptById(promptId)
                            showDeleteDialog = false
                            onNavigateBack()
                        }
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

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}
