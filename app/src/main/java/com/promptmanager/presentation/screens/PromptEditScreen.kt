package com.promptmanager.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.promptmanager.data.database.PromptEntity
import com.promptmanager.data.repository.PromptRepository
import com.promptmanager.presentation.components.PreviewColorScheme
import com.promptmanager.presentation.components.getPreviewColors
import com.promptmanager.util.PlaceholderParser
import kotlinx.coroutines.launch

/**
 * Edit-Screen für Prompts mit Live-Preview und Versionierungs-Dialog.
 *
 * Features:
 * - Bearbeiten aller Prompt-Felder (Title, Description, Content, Category)
 * - Live-Preview mit farblich markierten Platzhaltern
 * - Validierungs-Warnungen bei ungültigen Platzhaltern
 * - Speichern-Button öffnet VersionSelectionDialog mit 3 Optionen:
 *   1. Als Minor-Version speichern (z.B. 1.0 → 1.1)
 *   2. Als Major-Version speichern (z.B. 1.0 → 2.0)
 *   3. Als neuer Prompt (unabhängige Kopie)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptEditScreen(
    promptId: Long,
    repository: PromptRepository,
    onNavigateBack: () -> Unit,
    onSaveComplete: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // State für geladenen Prompt
    var originalPrompt by remember { mutableStateOf<PromptEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Editierbare Felder
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }

    // UI State
    var showVersionDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var selectedColorScheme by remember { mutableStateOf(PreviewColorScheme.default()) }
    var showColorPicker by remember { mutableStateOf(false) }

    // Validierung und Preview
    val validationWarnings by remember {
        derivedStateOf {
            PlaceholderParser.validatePlaceholders(content)
        }
    }

    val placeholders by remember {
        derivedStateOf {
            PlaceholderParser.extractPlaceholders(content)
        }
    }

    val previewSegments by remember {
        derivedStateOf {
            PlaceholderParser.createAnnotatedPreview(
                content,
                placeholders.associate { it.key to it.defaultValue }
            )
        }
    }

    // Prüfen ob neuer Prompt (promptId == 0) oder bestehender
    val isNewPrompt = promptId == 0L

    // Änderungs-Erkennung
    val hasChanges by remember {
        derivedStateOf {
            if (isNewPrompt) {
                // Bei neuem Prompt: Änderung wenn irgendwas ausgefüllt
                title.isNotBlank() || content.isNotBlank() || description.isNotBlank() || category.isNotBlank()
            } else {
                // Bei bestehendem Prompt: Änderung wenn unterschiedlich zum Original
                originalPrompt?.let {
                    title != it.title ||
                            description.trim() != (it.description ?: "").trim() ||
                            content != it.content ||
                            category.trim() != (it.category ?: "").trim()
                } ?: false
            }
        }
    }

    // Prompt laden (nur bei bestehendem Prompt)
    LaunchedEffect(promptId) {
        if (isNewPrompt) {
            // Neuer Prompt - keine Daten laden
            isLoading = false
        } else {
            scope.launch {
                val prompt = repository.getPromptById(promptId)
                if (prompt != null) {
                    originalPrompt = prompt
                    title = prompt.title
                    description = prompt.description ?: ""
                    content = prompt.content
                    category = prompt.category ?: ""
                    isLoading = false
                } else {
                    onNavigateBack() // Prompt nicht gefunden
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNewPrompt) "Neuer Prompt" else "Prompt bearbeiten") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges) {
                            showDiscardDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    if (!isNewPrompt && originalPrompt != null) {
                        // Version Badge nur bei bestehendem Prompt
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "v${originalPrompt!!.version}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isLoading && hasChanges) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (isNewPrompt) {
                            // Bei neuem Prompt: Direkt speichern ohne Version-Dialog
                            scope.launch {
                                isSaving = true
                                val newId = repository.insertPrompt(
                                    PromptEntity(
                                        title = title,
                                        description = description.ifBlank { null },
                                        content = content,
                                        category = category.ifBlank { null },
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis(),
                                        version = "1.0",
                                        parentId = null
                                    )
                                )
                                isSaving = false
                                onSaveComplete(newId)
                            }
                        } else {
                            // Bei bestehendem Prompt: Version-Dialog anzeigen
                            showVersionDialog = true
                        }
                    },
                    icon = { Icon(Icons.Default.Save, "Speichern") },
                    text = { Text("Speichern") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Titel
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = title.isBlank(),
                    supportingText = if (title.isBlank()) {
                        { Text("Titel darf nicht leer sein", color = MaterialTheme.colorScheme.error) }
                    } else null
                )

                // Beschreibung
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beschreibung (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                // Kategorie
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategorie (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("z.B. E-Mail, Entwicklung, Social Media") }
                )

                // Prompt-Inhalt
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Prompt-Inhalt") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 400.dp),
                    minLines = 8,
                    isError = content.isBlank() || validationWarnings.isNotEmpty(),
                    supportingText = {
                        Column {
                            if (content.isBlank()) {
                                Text("Inhalt darf nicht leer sein", color = MaterialTheme.colorScheme.error)
                            }
                            if (validationWarnings.isNotEmpty()) {
                                Text(
                                    text = "⚠ ${validationWarnings.joinToString(" • ")}",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    placeholder = {
                        Text(
                            """Beispiel:
Schreibe eine E-Mail an [Empfänger=Team] zum Thema [Thema].

Mit Dropdown:
Sprache: [Sprache=Deutsch,Englisch,Französisch]"""
                        )
                    }
                )

                // Platzhalter-Info Card
                if (placeholders.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Erkannte Platzhalter (${placeholders.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            placeholders.forEach { placeholder ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Type Badge
                                    Surface(
                                        color = when (placeholder.type) {
                                            com.promptmanager.domain.model.PlaceholderType.DROPDOWN ->
                                                MaterialTheme.colorScheme.primaryContainer
                                            com.promptmanager.domain.model.PlaceholderType.MULTILINE_TEXT ->
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Text(
                                            text = when (placeholder.type) {
                                                com.promptmanager.domain.model.PlaceholderType.DROPDOWN -> "▼"
                                                com.promptmanager.domain.model.PlaceholderType.MULTILINE_TEXT -> "≡"
                                                else -> "T"
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }

                                    Text(
                                        text = placeholder.key,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )

                                    if (placeholder.type == com.promptmanager.domain.model.PlaceholderType.DROPDOWN) {
                                        Text(
                                            text = "${placeholder.options.size - 1} Optionen",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        )
                                    } else if (placeholder.defaultValue.isNotBlank()) {
                                        Text(
                                            text = "\"${placeholder.defaultValue.take(20)}${if (placeholder.defaultValue.length > 20) "..." else ""}\"",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (content.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "ℹ Keine Platzhalter gefunden. Verwende [Name] oder [Name=Default] für dynamische Felder.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Live-Preview
                if (content.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Live-Vorschau",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    // Farbschema-Wechsler
                                    IconButton(
                                        onClick = { showColorPicker = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = "Farbschema ändern",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Legende mit aktuellen Farben
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        val emptyColors = getPreviewColors(false, selectedColorScheme)
                                        val filledColors = getPreviewColors(true, selectedColorScheme)

                                        Surface(
                                            color = emptyColors.background,
                                            shape = MaterialTheme.shapes.extraSmall
                                        ) {
                                            Text(
                                                text = "leer",
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = emptyColors.text
                                            )
                                        }
                                        Surface(
                                            color = filledColors.background,
                                            shape = MaterialTheme.shapes.extraSmall
                                        ) {
                                            Text(
                                                text = "gefüllt",
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = filledColors.text
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider()

                            ColoredPreviewText(
                                segments = previewSegments,
                                colorScheme = selectedColorScheme
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp)) // Platz für FAB
            }
        }
    }

    // Dialoge
    if (showVersionDialog && originalPrompt != null) {
        VersionSelectionDialog(
            currentVersion = originalPrompt!!.version,
            promptTitle = title,
            onDismiss = { showVersionDialog = false },
            onSaveAsMinorVersion = {
                scope.launch {
                    isSaving = true
                    val newId = repository.createNewVersion(
                        originalPromptId = promptId,
                        newTitle = title,
                        newDescription = description.ifBlank { null },
                        newContent = content,
                        newCategory = category.ifBlank { null },
                        incrementMinor = true
                    )
                    isSaving = false
                    showVersionDialog = false
                    if (newId != null) {
                        onSaveComplete(newId)
                    }
                }
            },
            onSaveAsMajorVersion = {
                scope.launch {
                    isSaving = true
                    val newId = repository.createNewVersion(
                        originalPromptId = promptId,
                        newTitle = title,
                        newDescription = description.ifBlank { null },
                        newContent = content,
                        newCategory = category.ifBlank { null },
                        incrementMinor = false
                    )
                    isSaving = false
                    showVersionDialog = false
                    if (newId != null) {
                        onSaveComplete(newId)
                    }
                }
            },
            onSaveAsNewPrompt = {
                scope.launch {
                    isSaving = true
                    val newId = repository.insertPrompt(
                        PromptEntity(
                            title = title,
                            description = description.ifBlank { null },
                            content = content,
                            category = category.ifBlank { null },
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            version = "1.0",
                            parentId = null // Unabhängiger neuer Prompt
                        )
                    )
                    isSaving = false
                    showVersionDialog = false
                    onSaveComplete(newId)
                }
            },
            isSaving = isSaving
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            icon = { Icon(Icons.Default.ArrowBack, null) },
            title = { Text("Änderungen verwerfen?") },
            text = { Text("Du hast ungespeicherte Änderungen. Möchtest du wirklich zurück?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Verwerfen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (showColorPicker) {
        ColorSchemePickerDialog(
            currentScheme = selectedColorScheme,
            onSchemeSelected = { scheme ->
                selectedColorScheme = scheme
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}

/**
 * Dialog zur Auswahl der Speicher-Option beim Bearbeiten eines Prompts.
 *
 * Bietet 3 Optionen:
 * 1. Als Minor-Version (z.B. 1.0 → 1.1)
 * 2. Als Major-Version (z.B. 1.0 → 2.0)
 * 3. Als neuer unabhängiger Prompt
 */
@Composable
private fun VersionSelectionDialog(
    currentVersion: String,
    promptTitle: String,
    onDismiss: () -> Unit,
    onSaveAsMinorVersion: () -> Unit,
    onSaveAsMajorVersion: () -> Unit,
    onSaveAsNewPrompt: () -> Unit,
    isSaving: Boolean
) {
    // Berechne nächste Versionen
    val nextMinorVersion = remember(currentVersion) {
        val parts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val major = parts.getOrNull(0) ?: 1
        val minor = parts.getOrNull(1) ?: 0
        "$major.${minor + 1}"
    }

    val nextMajorVersion = remember(currentVersion) {
        val parts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val major = parts.getOrNull(0) ?: 1
        "${major + 1}.0"
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        icon = { Icon(Icons.Default.Save, null) },
        title = { Text("Wie möchtest du speichern?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "\"$promptTitle\" (aktuell v$currentVersion)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                // Option 1: Minor Version
                OutlinedCard(
                    onClick = { if (!isSaving) onSaveAsMinorVersion() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Kleine Änderung",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = "v$nextMinorVersion",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Text(
                            text = "Verbesserungen, Bugfixes, kleine Anpassungen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Option 2: Major Version
                OutlinedCard(
                    onClick = { if (!isSaving) onSaveAsMajorVersion() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Große Änderung",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = "v$nextMajorVersion",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Text(
                            text = "Umfangreiche Änderungen, neuer Ansatz",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Option 3: Neuer Prompt
                OutlinedCard(
                    onClick = { if (!isSaving) onSaveAsNewPrompt() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Als neuer Prompt",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = "v1.0",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        Text(
                            text = "Unabhängige Kopie ohne Versions-Verknüpfung",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            if (!isSaving) {
                TextButton(onClick = onDismiss) {
                    Text("Abbrechen")
                }
            }
        }
    )

    if (isSaving) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

/**
 * Composable zur farblichen Darstellung der Live-Vorschau.
 * Markiert Platzhalter mit konfigurierbarem Farbschema.
 *
 * @param segments Die Preview-Segmente aus PlaceholderParser
 * @param colorScheme Das zu verwendende Farbschema
 */
@Composable
private fun ColoredPreviewText(
    segments: List<PlaceholderParser.PreviewSegment>,
    colorScheme: PreviewColorScheme = PreviewColorScheme.default()
) {
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
                append(segment.text)
            }
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Dialog zur Auswahl des Farbschemas für die Live-Vorschau.
 *
 * Zeigt alle verfügbaren Farbschemata mit Vorschau-Beispielen.
 */
@Composable
private fun ColorSchemePickerDialog(
    currentScheme: PreviewColorScheme,
    onSchemeSelected: (PreviewColorScheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Palette, null) },
        title = { Text("Farbschema wählen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Wähle ein Farbschema für die Platzhalter-Vorschau:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                PreviewColorScheme.values().forEach { scheme ->
                    OutlinedCard(
                        onClick = { onSchemeSelected(scheme) },
                        modifier = Modifier.fillMaxWidth(),
                        border = if (scheme == currentScheme) {
                            CardDefaults.outlinedCardBorder().copy(
                                width = 2.dp,
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        } else {
                            CardDefaults.outlinedCardBorder()
                        },
                        colors = if (scheme == currentScheme) {
                            CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        } else {
                            CardDefaults.outlinedCardColors()
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = scheme.displayName,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                // Vorschau-Beispiel
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val emptyColors = getPreviewColors(false, scheme)
                                    val filledColors = getPreviewColors(true, scheme)

                                    Surface(
                                        color = emptyColors.background,
                                        shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Text(
                                            text = "leer",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = emptyColors.text
                                        )
                                    }
                                    Surface(
                                        color = filledColors.background,
                                        shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Text(
                                            text = "gefüllt",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = filledColors.text
                                        )
                                    }
                                }
                            }

                            if (scheme == currentScheme) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Ausgewählt",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )
}
