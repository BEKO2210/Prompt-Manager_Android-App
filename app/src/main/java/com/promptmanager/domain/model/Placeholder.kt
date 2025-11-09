package com.promptmanager.domain.model

/**
 * Represents a placeholder found in a prompt.
 * Example: [Thema=Künstliche Intelligenz] -> key="Thema", defaultValue="Künstliche Intelligenz"
 */
data class Placeholder(
    val key: String,                    // Label/Name des Platzhalters (z.B. "Thema")
    val defaultValue: String = "",      // Standardwert falls vorhanden
    val isMultiLine: Boolean = false    // Heuristik: Lange Defaults -> TextArea
)
