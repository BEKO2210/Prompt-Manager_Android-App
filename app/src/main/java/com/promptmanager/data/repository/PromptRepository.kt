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
     * Erstellt eine neue Version eines bestehenden Prompts.
     *
     * @param originalPromptId ID des Original-Prompts
     * @param newContent Neuer Prompt-Inhalt
     * @param incrementMinor true = minor version (1.0 -> 1.1), false = major (1.0 -> 2.0)
     * @return Die ID der neuen Version
     */
    suspend fun createNewVersion(
        originalPromptId: Long,
        newTitle: String? = null,
        newDescription: String? = null,
        newContent: String,
        newCategory: String? = null,
        incrementMinor: Boolean = true
    ): Long? {
        val original = getPromptById(originalPromptId) ?: return null
        val now = System.currentTimeMillis()

        // Berechne neue Versionsnummer
        val currentVersion = original.version
        val newVersion = incrementVersion(currentVersion, incrementMinor)

        val newPrompt = original.copy(
            id = 0, // Neue ID
            title = newTitle ?: original.title,
            description = newDescription ?: original.description,
            content = newContent,
            category = newCategory ?: original.category,
            createdAt = now,
            updatedAt = now,
            usageCount = 0,
            version = newVersion,
            parentId = original.parentId ?: original.id // Falls Original schon eine Version ist
        )

        return insertPrompt(newPrompt)
    }

    /**
     * Incrementiert eine Versionsnummer.
     * Format: "major.minor"
     */
    private fun incrementVersion(currentVersion: String, minor: Boolean): String {
        val parts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val major = parts.getOrNull(0) ?: 1
        val minorNum = parts.getOrNull(1) ?: 0

        return if (minor) {
            "$major.${minorNum + 1}"
        } else {
            "${major + 1}.0"
        }
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
            usageCount = 0,
            version = "1.0",
            parentId = null
        )
    }

    // ============ VERSIONING OPERATIONS ============

    /**
     * Lädt alle Versionen eines Prompts.
     */
    fun getPromptVersions(promptId: Long): Flow<List<PromptEntity>> {
        return promptDao.getPromptVersions(promptId)
    }

    /**
     * Zählt Versionen eines Prompts.
     */
    suspend fun getVersionCount(promptId: Long): Int {
        return promptDao.getVersionCount(promptId)
    }

    /**
     * Lädt die neueste Version eines Prompts.
     */
    suspend fun getLatestVersion(promptId: Long): PromptEntity? {
        return promptDao.getLatestVersion(promptId)
    }
}
