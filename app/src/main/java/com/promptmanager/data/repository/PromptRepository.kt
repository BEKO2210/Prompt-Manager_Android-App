package com.promptmanager.data.repository

import com.promptmanager.data.database.PromptDao
import com.promptmanager.data.database.PromptEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository für Prompt-Verwaltung.
 *
 * Kapselt den Zugriff auf die Datenbank und bietet eine saubere API
 * für die ViewModels. Alle DB-Operationen laufen über dieses Repository.
 */
class PromptRepository(private val promptDao: PromptDao) {

    // ============ READ OPERATIONS ============

    /**
     * Alle Prompts als Flow.
     */
    val allPrompts: Flow<List<PromptEntity>> = promptDao.getAllPrompts()

    /**
     * Favoriten als Flow.
     */
    val favoritePrompts: Flow<List<PromptEntity>> = promptDao.getFavoritePrompts()

    /**
     * Prompts sortiert nach Nutzung.
     */
    val promptsByUsage: Flow<List<PromptEntity>> = promptDao.getPromptsByUsage()

    /**
     * Prompts nach Kategorie filtern.
     */
    fun getPromptsByCategory(category: String): Flow<List<PromptEntity>> {
        return promptDao.getPromptsByCategory(category)
    }

    /**
     * Einzelnen Prompt per ID laden.
     */
    suspend fun getPromptById(id: Long): PromptEntity? {
        return promptDao.getPromptById(id)
    }

    /**
     * Einzelnen Prompt als Flow (für Detail-Screen mit Auto-Updates).
     */
    fun getPromptByIdFlow(id: Long): Flow<PromptEntity?> {
        return promptDao.getPromptByIdFlow(id)
    }

    /**
     * Volltextsuche.
     */
    fun searchPrompts(query: String): Flow<List<PromptEntity>> {
        return promptDao.searchPrompts(query)
    }

    /**
     * Eindeutige Kategorien aus allen Prompts extrahieren (für Filter).
     */
    fun getAllCategories(): Flow<List<String>> {
        return allPrompts.map { prompts ->
            prompts.mapNotNull { it.category }
                .distinct()
                .sorted()
        }
    }

    // ============ WRITE OPERATIONS ============

    /**
     * Neuen Prompt erstellen.
     *
     * @return Die ID des eingefügten Prompts
     */
    suspend fun insertPrompt(prompt: PromptEntity): Long {
        return promptDao.insertPrompt(prompt)
    }

    /**
     * Prompt aktualisieren.
     * Setzt automatisch updatedAt auf aktuelle Zeit.
     */
    suspend fun updatePrompt(prompt: PromptEntity) {
        val updatedPrompt = prompt.copy(updatedAt = System.currentTimeMillis())
        promptDao.updatePrompt(updatedPrompt)
    }

    /**
     * Prompt löschen.
     */
    suspend fun deletePrompt(prompt: PromptEntity) {
        promptDao.deletePrompt(prompt)
    }

    /**
     * Prompt per ID löschen.
     */
    suspend fun deletePromptById(id: Long) {
        promptDao.deletePromptById(id)
    }

    /**
     * Prompt duplizieren.
     * Erstellt eine Kopie mit "(Kopie)" im Titel.
     *
     * @return Die ID des duplizierten Prompts
     */
    suspend fun duplicatePrompt(promptId: Long): Long? {
        val original = getPromptById(promptId) ?: return null
        val now = System.currentTimeMillis()

        val duplicate = original.copy(
            id = 0, // Neue ID wird von Room generiert
            title = "${original.title} (Kopie)",
            createdAt = now,
            updatedAt = now,
            usageCount = 0,
            isFavorite = false
        )

        return insertPrompt(duplicate)
    }

    // ============ UTILITY OPERATIONS ============

    /**
     * Favoriten-Status togglen.
     */
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        promptDao.updateFavoriteStatus(id, isFavorite)
    }

    /**
     * Wird aufgerufen wenn ein Prompt verwendet wird.
     * Incrementiert den Usage-Counter und aktualisiert updatedAt.
     */
    suspend fun markPromptAsUsed(id: Long) {
        promptDao.incrementUsageCount(id, System.currentTimeMillis())
    }

    /**
     * Alle Prompts löschen (für Reset / Testing).
     */
    suspend fun deleteAllPrompts() {
        promptDao.deleteAllPrompts()
    }

    // ============ BUSINESS LOGIC HELPERS ============

    /**
     * Erstellt einen neuen Prompt mit automatischen Timestamps.
     */
    fun createNewPrompt(
        title: String,
        content: String,
        description: String? = null,
        category: String? = null,
        isFavorite: Boolean = false
    ): PromptEntity {
        val now = System.currentTimeMillis()
        return PromptEntity(
            title = title,
            description = description,
            content = content,
            category = category,
            createdAt = now,
            updatedAt = now,
            isFavorite = isFavorite,
            usageCount = 0
        )
    }
}
