package com.promptmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.promptmanager.data.database.PromptEntity
import com.promptmanager.data.repository.PromptRepository
import com.promptmanager.domain.model.Placeholder
import com.promptmanager.util.PlaceholderParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel für Prompt-Details (Bearbeiten/Neu erstellen).
 *
 * Verwaltet:
 * - Laden/Speichern eines Prompts
 * - Platzhalter-Extraktion und -Vorschau
 * - Duplizieren
 * - Löschen
 */
class PromptDetailViewModel(
    private val repository: PromptRepository,
    private val promptId: Long? = null // null = Neuer Prompt
) : ViewModel() {

    // ============ UI STATE ============

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Formular-Felder
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _category = MutableStateFlow<String?>(null)
    val category: StateFlow<String?> = _category.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    // Extrahierte Platzhalter (Live-Update aus content)
    val placeholders: StateFlow<List<Placeholder>> = _content.map { text ->
        PlaceholderParser.extractPlaceholders(text)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Validierungs-Warnungen
    val validationWarnings: StateFlow<List<String>> = _content.map { text ->
        PlaceholderParser.validatePlaceholders(text)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Original-Prompt (für Änderungserkennung)
    private var originalPrompt: PromptEntity? = null

    init {
        loadPrompt()
    }

    // ============ ACTIONS ============

    fun setTitle(value: String) {
        _title.value = value
    }

    fun setDescription(value: String) {
        _description.value = value
    }

    fun setContent(value: String) {
        _content.value = value
    }

    fun setCategory(value: String?) {
        _category.value = value
    }

    fun toggleFavorite() {
        _isFavorite.value = !_isFavorite.value
    }

    /**
     * Speichert den Prompt (neu oder aktualisiert).
     */
    fun savePrompt(onSuccess: (Long) -> Unit = {}) {
        if (_title.value.isBlank() || _content.value.isBlank()) {
            _uiState.value = UiState.Error("Titel und Prompt-Text sind Pflichtfelder")
            return
        }

        viewModelScope.launch {
            try {
                val promptId = if (promptId == null) {
                    // Neuen Prompt erstellen
                    val newPrompt = repository.createNewPrompt(
                        title = _title.value,
                        content = _content.value,
                        description = _description.value.takeIf { it.isNotBlank() },
                        category = _category.value,
                        isFavorite = _isFavorite.value
                    )
                    repository.insertPrompt(newPrompt)
                } else {
                    // Bestehenden Prompt aktualisieren
                    originalPrompt?.let { original ->
                        val updated = original.copy(
                            title = _title.value,
                            description = _description.value.takeIf { it.isNotBlank() },
                            content = _content.value,
                            category = _category.value,
                            isFavorite = _isFavorite.value
                        )
                        repository.updatePrompt(updated)
                    }
                    promptId
                }
                _uiState.value = UiState.Success
                onSuccess(promptId)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Fehler beim Speichern: ${e.message}")
            }
        }
    }

    /**
     * Prompt löschen.
     */
    fun deletePrompt(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            originalPrompt?.let {
                repository.deletePrompt(it)
                onSuccess()
            }
        }
    }

    /**
     * Prompt duplizieren.
     */
    fun duplicatePrompt(onSuccess: (Long) -> Unit = {}) {
        viewModelScope.launch {
            promptId?.let { id ->
                val newId = repository.duplicatePrompt(id)
                newId?.let { onSuccess(it) }
            }
        }
    }

    /**
     * Placeholder-Werte in einem Dialog abfragen und finalen Prompt erstellen.
     * Diese Methode gibt die Placeholder zurück - das Ausfüllen passiert in der UI.
     */
    fun getPlaceholdersForDialog(): List<Placeholder> {
        return PlaceholderParser.extractPlaceholders(_content.value)
    }

    /**
     * Erstellt den finalen ausgefüllten Prompt.
     * Wird nach dem Placeholder-Dialog aufgerufen.
     *
     * @param values Map von Placeholder-Key -> Benutzereingabe
     * @return Der ausgefüllte Prompt-Text
     */
    fun createFilledPrompt(values: Map<String, String>): String {
        return PlaceholderParser.fillPlaceholders(_content.value, values)
    }

    /**
     * Markiert den Prompt als "verwendet" (incrementiert Usage-Counter).
     */
    fun markAsUsed() {
        viewModelScope.launch {
            promptId?.let {
                repository.markPromptAsUsed(it)
            }
        }
    }

    // ============ PRIVATE HELPERS ============

    private fun loadPrompt() {
        if (promptId == null) {
            // Neuer Prompt
            _uiState.value = UiState.Editing
            return
        }

        viewModelScope.launch {
            try {
                val prompt = repository.getPromptById(promptId)
                if (prompt != null) {
                    originalPrompt = prompt
                    _title.value = prompt.title
                    _description.value = prompt.description ?: ""
                    _content.value = prompt.content
                    _category.value = prompt.category
                    _isFavorite.value = prompt.isFavorite
                    _uiState.value = UiState.Editing
                } else {
                    _uiState.value = UiState.Error("Prompt nicht gefunden")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Fehler beim Laden: ${e.message}")
            }
        }
    }

    // ============ UI STATE SEALED CLASS ============

    sealed class UiState {
        object Loading : UiState()
        object Editing : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
}
