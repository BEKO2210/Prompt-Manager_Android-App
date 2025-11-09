package com.promptmanager.util

import com.promptmanager.domain.model.Placeholder

/**
 * Utility-Klasse zum Parsen und Ersetzen von Platzhaltern in Prompts.
 *
 * Unterstützte Formate:
 * - [Label] -> TEXT ohne Default
 * - [Label=Standardwert] -> TEXT mit Default
 * - [Label=Option1,Option2,Option3] -> DROPDOWN mit Optionen (2+ durch Komma getrennt)
 * - [Label=Langer Text...] -> MULTILINE_TEXT (wenn > 60 Zeichen)
 *
 * Beispiele:
 * - "Schreibe auf [Sprache=Deutsch,Englisch,Französisch]" -> Dropdown
 * - "Über [Thema=KI]" -> TextField
 */
object PlaceholderParser {

    // Regex zum Finden aller [...]-Blöcke
    private val PLACEHOLDER_REGEX = Regex("\\[(.+?)]")

    // Heuristik: Ab dieser Länge -> MultiLine-TextField
    private const val MULTILINE_THRESHOLD = 60

    // Min. Optionen für Dropdown-Erkennung
    private const val MIN_DROPDOWN_OPTIONS = 2

    /**
     * Extrahiert alle eindeutigen Platzhalter aus einem Prompt-Text.
     *
     * Erkennt automatisch Typ:
     * - DROPDOWN: Wenn 2+ Optionen durch Komma getrennt
     * - MULTILINE_TEXT: Wenn Text > 60 Zeichen oder Zeilenumbrüche
     * - TEXT: Sonst
     *
     * @param content Der Prompt-Text mit [Platzhaltern]
     * @return Liste eindeutiger Placeholder-Objekte
     */
    fun extractPlaceholders(content: String): List<Placeholder> {
        val placeholderMap = linkedMapOf<String, Placeholder>()

        PLACEHOLDER_REGEX.findAll(content).forEach { match ->
            val inner = match.groupValues[1].trim()
            val parts = inner.split("=", limit = 2)
            val key = parts[0].trim()

            if (key.isEmpty()) return@forEach
            if (placeholderMap.containsKey(key)) return@forEach // Duplikat ignorieren

            val valuesPart = if (parts.size == 2) parts[1].trim() else ""

            // Dropdown-Erkennung: 2+ durch Komma getrennte Optionen
            val options = valuesPart.split(",").map { it.trim() }

            val placeholder = when {
                // DROPDOWN: Mindestens 2 Optionen vorhanden
                options.size >= MIN_DROPDOWN_OPTIONS && options.all { it.isNotEmpty() } -> {
                    Placeholder(
                        key = key,
                        type = com.promptmanager.domain.model.PlaceholderType.DROPDOWN,
                        defaultValue = "",
                        options = listOf("") + options // Leere Option am Anfang
                    )
                }
                // MULTILINE_TEXT: Langer Text oder Zeilenumbrüche
                valuesPart.length > MULTILINE_THRESHOLD || valuesPart.contains('\n') -> {
                    Placeholder(
                        key = key,
                        type = com.promptmanager.domain.model.PlaceholderType.MULTILINE_TEXT,
                        defaultValue = valuesPart,
                        options = emptyList()
                    )
                }
                // TEXT: Standard
                else -> {
                    Placeholder(
                        key = key,
                        type = com.promptmanager.domain.model.PlaceholderType.TEXT,
                        defaultValue = valuesPart,
                        options = emptyList()
                    )
                }
            }

            placeholderMap[key] = placeholder
        }

        return placeholderMap.values.toList()
    }

    /**
     * Ersetzt alle Platzhalter im Prompt mit den eingegebenen Werten.
     *
     * Unterstützt alle Platzhalter-Typen (TEXT, DROPDOWN, MULTILINE).
     *
     * @param content Der Original-Prompt mit [Platzhaltern]
     * @param values Map von Key -> Benutzereingabe
     * @return Der ausgefüllte Prompt-Text
     */
    fun fillPlaceholders(content: String, values: Map<String, String>): String {
        return PLACEHOLDER_REGEX.replace(content) { match ->
            val inner = match.groupValues[1].trim()
            val parts = inner.split("=", limit = 2)
            val key = parts[0].trim()

            // Für Dropdowns: Erste nicht-leere Option als Fallback
            val valuesPart = if (parts.size == 2) parts[1].trim() else ""
            val options = valuesPart.split(",").map { it.trim() }
            val isDropdown = options.size >= MIN_DROPDOWN_OPTIONS && options.all { it.isNotEmpty() }

            val defaultValue = if (isDropdown) "" else valuesPart

            // Priorisierung: Benutzereingabe > Default > Leer
            values[key]?.takeIf { it.isNotBlank() } ?: defaultValue
        }
    }

    /**
     * Prüft ob ein Prompt überhaupt Platzhalter enthält.
     *
     * @param content Der zu prüfende Text
     * @return true wenn mindestens ein [...] gefunden wurde
     */
    fun hasPlaceholders(content: String): Boolean {
        return PLACEHOLDER_REGEX.containsMatchIn(content)
    }

    /**
     * Zählt die Anzahl der Platzhalter-Vorkommen im Text.
     * (Inklusive Duplikate - für Analytics oder Warnungen)
     *
     * @param content Der zu prüfende Text
     * @return Anzahl der gefundenen [...]-Blöcke
     */
    fun countPlaceholders(content: String): Int {
        return PLACEHOLDER_REGEX.findAll(content).count()
    }

    /**
     * Validiert Platzhalter-Syntax und gibt Warnungen zurück.
     *
     * Mögliche Probleme:
     * - Unbalancierte Klammern
     * - Leere Labels: []
     * - Mehrere = im Platzhalter
     *
     * @param content Der zu validierende Text
     * @return Liste von Warnmeldungen (leer wenn alles OK)
     */
    fun validatePlaceholders(content: String): List<String> {
        val warnings = mutableListOf<String>()

        // Prüfe auf unbalancierte Klammern
        val openBrackets = content.count { it == '[' }
        val closeBrackets = content.count { it == ']' }
        if (openBrackets != closeBrackets) {
            warnings.add("Unbalancierte Klammern: $openBrackets × '[' vs. $closeBrackets × ']'")
        }

        // Prüfe auf leere Platzhalter oder problematische Patterns
        PLACEHOLDER_REGEX.findAll(content).forEach { match ->
            val inner = match.groupValues[1].trim()

            if (inner.isEmpty()) {
                warnings.add("Leerer Platzhalter gefunden: []")
            } else if (inner.startsWith("=")) {
                warnings.add("Platzhalter ohne Label: [${match.value}]")
            }
        }

        return warnings
    }

    /**
     * Erstellt eine Preview des ausgefüllten Prompts für die UI.
     * Zeigt Platzhalter-Keys in Anführungszeichen wenn kein Wert vorhanden.
     *
     * @param content Der Original-Prompt
     * @param values Teilweise ausgefüllte Werte
     * @return Preview-String mit sichtbaren fehlenden Platzhaltern
     */
    fun createPreview(content: String, values: Map<String, String>): String {
        return PLACEHOLDER_REGEX.replace(content) { match ->
            val inner = match.groupValues[1].trim()
            val parts = inner.split("=", limit = 2)
            val key = parts[0].trim()
            val defaultValue = if (parts.size == 2) parts[1].trim() else ""

            val value = values[key]?.takeIf { it.isNotBlank() } ?: defaultValue

            if (value.isBlank()) {
                "\"$key\"" // Fehlender Wert -> in Quotes anzeigen
            } else {
                value
            }
        }
    }

    /**
     * Erstellt eine strukturierte Preview mit Metadaten für farbliche Markierung.
     *
     * @param content Der Original-Prompt
     * @param values Teilweise ausgefüllte Werte
     * @return Paar aus (Preview-Text, Liste der Placeholder-Status)
     */
    data class PreviewSegment(
        val text: String,
        val isPlaceholder: Boolean,
        val isFilled: Boolean // true=grün, false=rot (nur relevant wenn isPlaceholder=true)
    )

    fun createAnnotatedPreview(content: String, values: Map<String, String>): List<PreviewSegment> {
        val segments = mutableListOf<PreviewSegment>()
        var lastIndex = 0

        PLACEHOLDER_REGEX.findAll(content).forEach { match ->
            // Text vor dem Platzhalter
            if (match.range.first > lastIndex) {
                segments.add(PreviewSegment(
                    text = content.substring(lastIndex, match.range.first),
                    isPlaceholder = false,
                    isFilled = false
                ))
            }

            // Platzhalter-Wert ermitteln
            val inner = match.groupValues[1].trim()
            val parts = inner.split("=", limit = 2)
            val key = parts[0].trim()
            val defaultValue = if (parts.size == 2) parts[1].trim() else ""

            val value = values[key]?.takeIf { it.isNotBlank() } ?: defaultValue
            val isFilled = value.isNotBlank()

            segments.add(PreviewSegment(
                text = if (isFilled) value else "[${key}]",
                isPlaceholder = true,
                isFilled = isFilled
            ))

            lastIndex = match.range.last + 1
        }

        // Rest-Text nach letztem Platzhalter
        if (lastIndex < content.length) {
            segments.add(PreviewSegment(
                text = content.substring(lastIndex),
                isPlaceholder = false,
                isFilled = false
            ))
        }

        return segments
    }
}
