package com.promptmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.promptmanager.data.database.AppDatabase
import com.promptmanager.data.repository.PromptRepository
import com.promptmanager.presentation.screens.PromptApp
import com.promptmanager.presentation.theme.PromptManagerTheme

/**
 * Single Activity - alles läuft über Compose Navigation.
 */
class MainActivity : ComponentActivity() {

    private lateinit var repository: PromptRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Repository initialisieren
        val database = AppDatabase.getDatabase(applicationContext)
        repository = PromptRepository(database.promptDao())

        setContent {
            PromptManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PromptApp(repository = repository)
                }
            }
        }
    }
}
