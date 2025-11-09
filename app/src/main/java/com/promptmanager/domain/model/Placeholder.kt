package com.promptmanager.domain.model

/**
 * Represents a placeholder found in a prompt.
 *
 * Examples:
 * - [Thema] -> TEXT type, no default
 * - [Thema=KI] -> TEXT type with default
 * - [Sprache=Deutsch,Englisch,Französisch] -> DROPDOWN type with options
 * - [Nachricht=Langer Text...] -> MULTILINE_TEXT type
 */
data class Placeholder(
    val key: String,                        // Label/Name des Platzhalters (z.B. "Thema")
    val type: PlaceholderType,              // TEXT, DROPDOWN, oder MULTILINE_TEXT
    val defaultValue: String = "",          // Standardwert für TEXT/MULTILINE
    val options: List<String> = emptyList() // Optionen für DROPDOWN (inkl. empty option)
)

/**
 * Typ des Platzhalters bestimmt das UI-Element.
 */
enum class PlaceholderType {
    TEXT,           // Normales TextField
    MULTILINE_TEXT, // TextArea für längere Texte
    DROPDOWN        // Dropdown-Menü mit Optionen
}
