package com.promptmanager.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Utility-Klasse für Clipboard- und Share-Operationen.
 */
object ClipboardHelper {

    /**
     * Kopiert Text in die Zwischenablage.
     *
     * @param context Android Context
     * @param label Label für den Clipboard-Eintrag
     * @param text Der zu kopierende Text
     * @param showToast Wenn true, wird ein Toast angezeigt
     */
    fun copyToClipboard(
        context: Context,
        label: String,
        text: String,
        showToast: Boolean = true
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)

        if (showToast) {
            Toast.makeText(context, "Prompt kopiert", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Öffnet den Android Share Sheet zum Teilen von Text.
     *
     * @param context Android Context
     * @param text Der zu teilende Text
     * @param title Titel des Share Dialogs
     */
    fun shareText(
        context: Context,
        text: String,
        title: String = "Prompt teilen"
    ) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    }

    /**
     * Kopiert Text in die Zwischenablage UND öffnet den Share Dialog.
     *
     * @param context Android Context
     * @param label Label für Clipboard
     * @param text Der Text
     */
    fun copyAndShare(
        context: Context,
        label: String,
        text: String
    ) {
        copyToClipboard(context, label, text, showToast = false)
        shareText(context, text)
    }

    /**
     * Liest den aktuellen Clipboard-Inhalt.
     *
     * @param context Android Context
     * @return Der aktuelle Clipboard-Text oder null
     */
    fun getClipboardText(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.primaryClip?.getItemAt(0)?.text?.toString()
    }

    /**
     * Prüft ob die Zwischenablage Text enthält.
     *
     * @param context Android Context
     * @return true wenn Text in der Zwischenablage ist
     */
    fun hasClipboardText(context: Context): Boolean {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.hasPrimaryClip() &&
                clipboard.primaryClipDescription?.hasMimeType("text/plain") == true
    }
}
