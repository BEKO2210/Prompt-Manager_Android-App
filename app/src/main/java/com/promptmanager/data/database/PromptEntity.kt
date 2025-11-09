package com.promptmanager.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity für Prompts.
 *
 * Speichert den kompletten Prompt-Text mit [Platzhaltern] im content-Feld.
 * Platzhalter werden dynamisch beim Nutzen geparst.
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
    val usageCount: Int = 0             // Wie oft wurde der Prompt genutzt?
)
