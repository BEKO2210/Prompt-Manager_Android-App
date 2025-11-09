package com.promptmanager.util

import com.promptmanager.domain.model.Placeholder

/**
 * Utility-Klasse zum Parsen und Ersetzen von Platzhaltern in Prompts.
 *
 * Format:
 * - [Label] -> Platzhalter ohne Default
 * - [Label=Standardwert] -> Platzhalter mit Default
 *
 * Beispiel:
 * "Schreibe einen Text über [Thema=KI] für [Zielgruppe]"
 * -> 2 Platzhalter: "Thema" (default: "KI"), "Zielgruppe" (kein default)
 */
object PlaceholderParser {

    // Regex zum Finden aller [...]-Blöcke
    // Verwendet non-greedy matching (.+?) um verschachtelte Klammern zu vermeiden
    private val PLACEHOLDER_REGEX = Regex("\\[(.+?)]")

    // Heuristik: Ab dieser Länge des Defaults -> MultiLine-TextField verwenden
    private const val MULTILINE_THRESHOLD = 60

    /**
     * Extrahiert alle eindeutigen Platzhalter aus einem Prompt-Text.
     *
     * Duplikate werden zusammengefasst:
     * - Wenn [Thema] mehrfach vorkommt -> nur ein Placeholder-Objekt
     * - Wenn [Thema=KI] und [Thema=AI] vorkommen -> erster Default gewinnt
     *
     * @param content Der Prompt-Text mit [Platzhaltern]
     * @return Liste eindeutiger Placeholder-Objekte, sortiert nach Vorkommen
     */
    fun extractPlaceholders(content: String): List<Placeholder> {
        val placeholderMap = linkedMapOf<String, Placeholder>()

        PLACEHOLDER_REGEX.findAll(content).forEach { match ->
            val inner = match.groupValues[1].trim()

            // Split bei '=' um Label und Default zu trennen
            val parts = inner.split("=", limit = 2)
            val key = parts[0].trim()

            // Leere Keys ignorieren
            if (key.isEmpty()) return@forEach

            // Nur hinzufügen wenn noch nicht vorhanden (erster Treffer gewinnt)
            if (!placeholderMap.containsKey(key)) {
                val defaultValue = if (parts.size == 2) parts[1].trim() else ""
                val isMultiLine = defaultValue.length > MULTILINE_THRESHOLD ||
                        defaultValue.contains('\n')

                placeholderMap[key] = Placeholder(
                    key = key,
                    defaultValue = defaultValue,
                    isMultiLine = isMultiLine
                )
            }
        }

        return placeholderMap.values.toList()
    }

    /**
     * Ersetzt alle Platzhalter im Prompt mit den eingegebenen Werten.
     *
     * Verhalten:
     * - Wenn Wert in `values` vorhanden und nicht leer -> verwenden
     * - Sonst: Default-Wert aus dem Platzhalter verwenden
     * - Wenn beides leer: Platzhalter wird durch leeren String ersetzt
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
            val defaultValue = if (parts.size == 2) parts[1].trim() else ""

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
}
