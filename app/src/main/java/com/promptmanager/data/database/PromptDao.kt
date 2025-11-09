package com.promptmanager.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object für Prompt-Operationen.
 * Alle Operationen sind als Flow oder suspend functions für Coroutine-Support.
 */
@Dao
interface PromptDao {

    // ============ READ OPERATIONS ============

    /**
     * Alle Prompts als Flow (LiveData-Alternative).
     * Automatische Updates bei DB-Änderungen.
     */
    @Query("SELECT * FROM prompts ORDER BY updatedAt DESC")
    fun getAllPrompts(): Flow<List<PromptEntity>>

    /**
     * Prompts filtern nach Kategorie.
     */
    @Query("SELECT * FROM prompts WHERE category = :category ORDER BY updatedAt DESC")
    fun getPromptsByCategory(category: String): Flow<List<PromptEntity>>

    /**
     * Nur Favoriten.
     */
    @Query("SELECT * FROM prompts WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoritePrompts(): Flow<List<PromptEntity>>

    /**
     * Prompts sortiert nach Nutzung.
     */
    @Query("SELECT * FROM prompts ORDER BY usageCount DESC, updatedAt DESC")
    fun getPromptsByUsage(): Flow<List<PromptEntity>>

    /**
     * Volltextsuche in Titel, Beschreibung und Content.
     */
    @Query("""
        SELECT * FROM prompts
        WHERE title LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
           OR content LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchPrompts(query: String): Flow<List<PromptEntity>>

    /**
     * Einzelnen Prompt per ID laden.
     */
    @Query("SELECT * FROM prompts WHERE id = :id")
    suspend fun getPromptById(id: Long): PromptEntity?

    /**
     * Einzelnen Prompt als Flow (für Detail-Screen mit Auto-Update).
     */
    @Query("SELECT * FROM prompts WHERE id = :id")
    fun getPromptByIdFlow(id: Long): Flow<PromptEntity?>

    // ============ WRITE OPERATIONS ============

    /**
     * Neuen Prompt einfügen.
     * @return Die ID des eingefügten Prompts
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompt(prompt: PromptEntity): Long

    /**
     * Prompt aktualisieren.
     */
    @Update
    suspend fun updatePrompt(prompt: PromptEntity)

    /**
     * Prompt löschen.
     */
    @Delete
    suspend fun deletePrompt(prompt: PromptEntity)

    /**
     * Prompt per ID löschen.
     */
    @Query("DELETE FROM prompts WHERE id = :id")
    suspend fun deletePromptById(id: Long)

    // ============ UTILITY OPERATIONS ============

    /**
     * Favoriten-Status togglen.
     */
    @Query("UPDATE prompts SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    /**
     * Usage-Counter incrementieren (wird beim "Nutzen" aufgerufen).
     */
    @Query("UPDATE prompts SET usageCount = usageCount + 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun incrementUsageCount(id: Long, timestamp: Long)

    /**
     * Alle Prompts löschen (z.B. für Reset).
     */
    @Query("DELETE FROM prompts")
    suspend fun deleteAllPrompts()
}
