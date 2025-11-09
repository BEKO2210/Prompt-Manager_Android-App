package com.promptmanager.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity für Prompts mit Versionierungs-Support.
 *
 * Speichert den kompletten Prompt-Text mit [Platzhaltern] im content-Feld.
 * Versionierung: parentId verknüpft Versionen eines Prompts.
 */
@Entity(tableName = "prompts")
data class PromptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,                  // z.B. "Blog-Artikel Zusammenfassung"
    val description: String? = null,    // Optional: Hilfetext, Notes
    val content: String,                // Der Prompt-Text mit [Platzhaltern]
    val category: String? = null,       // Optional: "E-Mail", "Dev", "Social", etc.
    val createdAt: Long,                // Timestamp
    val updatedAt: Long,                // Timestamp
    val isFavorite: Boolean = false,    // Schnellzugriff-Flag
    val usageCount: Int = 0,            // Wie oft wurde der Prompt genutzt?

    // Versionierungs-System (seit DB Version 2)
    val version: String = "1.0",        // Versionsnummer (z.B. "1.0", "1.1", "2.0")
    val parentId: Long? = null          // ID des Original-Prompts (null = Original)
)
