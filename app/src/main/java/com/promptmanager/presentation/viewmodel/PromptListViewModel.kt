package com.promptmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.promptmanager.data.database.PromptEntity
import com.promptmanager.data.repository.PromptRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel für die Prompt-Liste (Hauptbildschirm).
 *
 * Verwaltet:
 * - Anzeige aller Prompts mit Filter/Suche/Sortierung
 * - Favoriten-Toggle
 * - Löschen
 */
class PromptListViewModel(
    private val repository: PromptRepository
) : ViewModel() {

    // ============ UI STATE ============

    // Suchquery
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Aktiver Filter
    private val _filterMode = MutableStateFlow(FilterMode.ALL)
    val filterMode: StateFlow<FilterMode> = _filterMode.asStateFlow()

    // Sortierung
    private val _sortMode = MutableStateFlow(SortMode.UPDATED_DESC)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    // Ausgewählte Kategorie (null = alle)
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Verfügbare Kategorien
    val categories: StateFlow<List<String>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ============ PROMPTS DATA ============

    /**
     * Kombinierte Prompts mit allen Filtern und Sortierungen.
     */
    val prompts: StateFlow<List<PromptEntity>> = combine(
        _searchQuery,
        _filterMode,
        _sortMode,
        _selectedCategory
    ) { query, filter, sort, category ->
        QueryParams(query, filter, sort, category)
    }.flatMapLatest { params ->
        getPromptsFlow(params)
    }.map { prompts ->
        applySorting(prompts, _sortMode.value)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ============ ACTIONS ============

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterMode(mode: FilterMode) {
        _filterMode.value = mode
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun toggleFavorite(promptId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(promptId, !isFavorite)
        }
    }

    fun deletePrompt(prompt: PromptEntity) {
        viewModelScope.launch {
            repository.deletePrompt(prompt)
        }
    }

    fun duplicatePrompt(promptId: Long) {
        viewModelScope.launch {
            repository.duplicatePrompt(promptId)
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    // ============ PRIVATE HELPERS ============

    private fun getPromptsFlow(params: QueryParams): Flow<List<PromptEntity>> {
        // Basis-Flow je nach Filter
        val baseFlow = when (params.filter) {
            FilterMode.ALL -> repository.allPrompts
            FilterMode.FAVORITES -> repository.favoritePrompts
            FilterMode.BY_USAGE -> repository.promptsByUsage
        }

        // Kategorie-Filter anwenden
        val categoryFiltered = if (params.category != null) {
            baseFlow.map { prompts ->
                prompts.filter { it.category == params.category }
            }
        } else {
            baseFlow
        }

        // Suche anwenden
        return if (params.query.isNotBlank()) {
            categoryFiltered.map { prompts ->
                prompts.filter { prompt ->
                    prompt.title.contains(params.query, ignoreCase = true) ||
                            prompt.description?.contains(params.query, ignoreCase = true) == true ||
                            prompt.content.contains(params.query, ignoreCase = true)
                }
            }
        } else {
            categoryFiltered
        }
    }

    private fun applySorting(prompts: List<PromptEntity>, sortMode: SortMode): List<PromptEntity> {
        return when (sortMode) {
            SortMode.UPDATED_DESC -> prompts.sortedByDescending { it.updatedAt }
            SortMode.CREATED_DESC -> prompts.sortedByDescending { it.createdAt }
            SortMode.TITLE_ASC -> prompts.sortedBy { it.title.lowercase() }
            SortMode.USAGE_DESC -> prompts.sortedByDescending { it.usageCount }
        }
    }

    // ============ DATA CLASSES ============

    private data class QueryParams(
        val query: String,
        val filter: FilterMode,
        val sort: SortMode,
        val category: String?
    )

    enum class FilterMode {
        ALL,        // Alle Prompts
        FAVORITES,  // Nur Favoriten
        BY_USAGE    // Sortiert nach Nutzung
    }

    enum class SortMode {
        UPDATED_DESC,   // Zuletzt bearbeitet
        CREATED_DESC,   // Zuletzt erstellt
        TITLE_ASC,      // Alphabetisch
        USAGE_DESC      // Meistgenutzt
    }
}
