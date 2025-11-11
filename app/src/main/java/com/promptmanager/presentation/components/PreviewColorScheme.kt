package com.promptmanager.presentation.components

import androidx.compose.ui.graphics.Color

/**
 * Farbschema für die Live-Vorschau von Platzhaltern.
 *
 * Bietet verschiedene Farbmodi:
 * - RED_GREEN: Klassisch (rot=leer, grün=gefüllt)
 * - BLACK_WHITE: Monochrom (grau=leer, schwarz=gefüllt)
 * - WHITE_BLACK: Invertiert (hellgrau=leer, weiß=gefüllt, für Dark Mode)
 * - BLUE_ORANGE: Alternativ (blau=leer, orange=gefüllt)
 */
enum class PreviewColorScheme(
    val emptyBackground: Color,
    val emptyText: Color,
    val filledBackground: Color,
    val filledText: Color,
    val displayName: String
) {
    /**
     * Klassisches Farbschema: Rot für leer, Grün für gefüllt
     */
    RED_GREEN(
        emptyBackground = Color(0x40F44336),   // 25% transparentes Rot
        emptyText = Color(0xFFF44336),         // Rot
        filledBackground = Color(0x4000C853),  // 25% transparentes Grün
        filledText = Color(0xFF00C853),        // Grün
        displayName = "Rot/Grün"
    ),

    /**
     * Monochrom-Schema: Grau für leer, Schwarz für gefüllt
     * Gut für Farbenblinde oder professionelle Darstellung
     */
    BLACK_WHITE(
        emptyBackground = Color(0x40757575),   // 25% transparentes Grau
        emptyText = Color(0xFF757575),         // Grau
        filledBackground = Color(0x40212121),  // 25% transparentes Schwarz
        filledText = Color(0xFF212121),        // Schwarz
        displayName = "Schwarz/Weiß"
    ),

    /**
     * Invertiertes Schema: Hellgrau für leer, Weiß für gefüllt
     * Ideal für Dark Mode
     */
    WHITE_BLACK(
        emptyBackground = Color(0x40BDBDBD),   // 25% transparentes Hellgrau
        emptyText = Color(0xFFBDBDBD),         // Hellgrau
        filledBackground = Color(0x40FFFFFF),  // 25% transparentes Weiß
        filledText = Color(0xFFFFFFFF),        // Weiß
        displayName = "Weiß/Schwarz (Invertiert)"
    ),

    /**
     * Alternatives Farbschema: Blau für leer, Orange für gefüllt
     * Weniger aufdringlich als Rot/Grün
     */
    BLUE_ORANGE(
        emptyBackground = Color(0x402196F3),   // 25% transparentes Blau
        emptyText = Color(0xFF2196F3),         // Blau
        filledBackground = Color(0x40FF9800),  // 25% transparentes Orange
        filledText = Color(0xFFFF9800),        // Orange
        displayName = "Blau/Orange"
    );

    companion object {
        /**
         * Gibt das Standard-Farbschema zurück (RED_GREEN).
         */
        fun default() = RED_GREEN

        /**
         * Findet ein Farbschema anhand des Namens.
         * Gibt null zurück wenn nicht gefunden.
         */
        fun fromName(name: String): PreviewColorScheme? {
            return values().find { it.name == name }
        }
    }
}

/**
 * Data class für Farbinformationen eines Platzhalters.
 * Wird von createStyledPreview() zurückgegeben.
 */
data class PreviewColors(
    val background: Color,
    val text: Color
)

/**
 * Ermittelt die Farben für einen Platzhalter basierend auf dem gewählten Schema.
 *
 * @param isFilled Ob der Platzhalter ausgefüllt ist
 * @param colorScheme Das zu verwendende Farbschema
 * @return PreviewColors mit background und text Farben
 */
fun getPreviewColors(
    isFilled: Boolean,
    colorScheme: PreviewColorScheme = PreviewColorScheme.default()
): PreviewColors {
    return if (isFilled) {
        PreviewColors(
            background = colorScheme.filledBackground,
            text = colorScheme.filledText
        )
    } else {
        PreviewColors(
            background = colorScheme.emptyBackground,
            text = colorScheme.emptyText
        )
    }
}
