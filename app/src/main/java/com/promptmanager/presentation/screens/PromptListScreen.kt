package com.promptmanager.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.promptmanager.data.database.PromptEntity
import com.promptmanager.presentation.components.PlaceholderDialog
import com.promptmanager.presentation.viewmodel.PromptListViewModel
import com.promptmanager.util.PlaceholderParser

/**
 * Hauptbildschirm: Liste aller Prompts mit Suche und Filter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptListScreen(
    viewModel: PromptListViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToNew: () -> Unit
) {
    val prompts by viewModel.prompts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterMode by viewModel.filterMode.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val context = LocalContext.current

    // Dialog State für "Prompt nutzen"
    var showPlaceholderDialog by remember { mutableStateOf(false) }
    var selectedPrompt by remember { mutableStateOf<PromptEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prompt Manager") },
                actions = {
                    // Sortierung
                    var showSortMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, "Sortieren")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Zuletzt bearbeitet") },
                            onClick = {
                                viewModel.setSortMode(PromptListViewModel.SortMode.UPDATED_DESC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Alphabetisch") },
                            onClick = {
                                viewModel.setSortMode(PromptListViewModel.SortMode.TITLE_ASC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Meistgenutzt") },
                            onClick = {
                                viewModel.setSortMode(PromptListViewModel.SortMode.USAGE_DESC)
                                showSortMenu = false
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToNew) {
                Icon(Icons.Default.Add, "Neuer Prompt")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Suchfeld
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                onClear = { viewModel.clearSearch() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filter-Chips
            FilterChips(
                filterMode = filterMode,
                onFilterChange = { viewModel.setFilterMode(it) },
                categories = categories,
                selectedCategory = selectedCategory,
                onCategoryChange = { viewModel.setCategory(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Prompt-Liste
            if (prompts.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = prompts,
                        key = { it.id }
                    ) { prompt ->
                        PromptCard(
                            prompt = prompt,
                            onCardClick = { onNavigateToDetail(prompt.id) },
                            onUseClick = {
                                selectedPrompt = prompt
                                if (PlaceholderParser.hasPlaceholders(prompt.content)) {
                                    showPlaceholderDialog = true
                                } else {
                                    copyToClipboard(context, prompt.title, prompt.content)
                                }
                            },
                            onFavoriteClick = {
                                viewModel.toggleFavorite(prompt.id, prompt.isFavorite)
                            },
                            onDeleteClick = { viewModel.deletePrompt(prompt) },
                            onDuplicateClick = { viewModel.duplicatePrompt(prompt.id) }
                        )
                    }
                }
            }
        }
    }

    // Platzhalter-Dialog
    if (showPlaceholderDialog && selectedPrompt != null) {
        PlaceholderDialog(
            promptTitle = selectedPrompt!!.title,
            promptContent = selectedPrompt!!.content,
            onDismiss = {
                showPlaceholderDialog = false
                selectedPrompt = null
            },
            onConfirm = { filledPrompt ->
                copyToClipboard(context, selectedPrompt!!.title, filledPrompt)
                showPlaceholderDialog = false
                selectedPrompt = null
            }
        )
    }
}

/**
 * Suchfeld mit Clear-Button.
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Prompts durchsuchen...") },
        leadingIcon = { Icon(Icons.Default.Search, "Suche") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, "Löschen")
                }
            }
        },
        singleLine = true
    )
}

/**
 * Filter-Chips für Alle/Favoriten/Kategorien.
 */
@Composable
private fun FilterChips(
    filterMode: PromptListViewModel.FilterMode,
    onFilterChange: (PromptListViewModel.FilterMode) -> Unit,
    categories: List<String>,
    selectedCategory: String?,
    onCategoryChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Alle / Favoriten
        item {
            FilterChip(
                selected = filterMode == PromptListViewModel.FilterMode.ALL && selectedCategory == null,
                onClick = {
                    onFilterChange(PromptListViewModel.FilterMode.ALL)
                    onCategoryChange(null)
                },
                label = { Text("Alle") },
                leadingIcon = { Icon(Icons.Default.List, null, Modifier.size(18.dp)) }
            )
        }
        item {
            FilterChip(
                selected = filterMode == PromptListViewModel.FilterMode.FAVORITES,
                onClick = {
                    onFilterChange(PromptListViewModel.FilterMode.FAVORITES)
                    onCategoryChange(null)
                },
                label = { Text("Favoriten") },
                leadingIcon = { Icon(Icons.Default.Star, null, Modifier.size(18.dp)) }
            )
        }

        // Kategorien
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = {
                    if (selectedCategory == category) {
                        onCategoryChange(null)
                    } else {
                        onFilterChange(PromptListViewModel.FilterMode.ALL)
                        onCategoryChange(category)
                    }
                },
                label = { Text(category) }
            )
        }
    }
}

/**
 * Prompt-Card mit allen Actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptCard(
    prompt: PromptEntity,
    onCardClick: () -> Unit,
    onUseClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDuplicateClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onCardClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Titel + Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = prompt.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (prompt.category != null) {
                        Text(
                            text = prompt.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row {
                    // Favoriten-Icon
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            imageVector = if (prompt.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorit",
                            tint = if (prompt.isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Menü
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Menü")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Bearbeiten") },
                                onClick = {
                                    onCardClick()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplizieren") },
                                onClick = {
                                    onDuplicateClick()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.FileCopy, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Löschen") },
                                onClick = {
                                    onDeleteClick()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Prompt-Vorschau
            Text(
                text = prompt.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // "Nutzen"-Button
            Button(
                onClick = onUseClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Prompt nutzen")
            }
        }
    }
}

/**
 * Empty State wenn keine Prompts vorhanden.
 */
@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Keine Prompts gefunden",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Helper: Text in Zwischenablage kopieren + Toast anzeigen.
 */
private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)

    // TODO: Snackbar oder Toast anzeigen
    // Für jetzt: stille Copy
}
