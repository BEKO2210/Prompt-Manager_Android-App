package com.promptmanager.presentation.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.promptmanager.data.repository.PromptRepository
import com.promptmanager.presentation.viewmodel.PromptListViewModel

/**
 * Root Composable für die App-Navigation.
 *
 * Navigation Flow:
 * - Liste → View (ReadOnly) → Edit (mit Versionierungs-Dialog)
 * - Liste → New (direkt Edit-Mode für neue Prompts)
 */
@Composable
fun PromptApp(repository: PromptRepository) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "prompt_list"
    ) {
        // ========== HAUPTBILDSCHIRM (Liste) ==========
        composable("prompt_list") {
            val viewModel = PromptListViewModel(repository)
            PromptListScreen(
                viewModel = viewModel,
                onNavigateToDetail = { promptId ->
                    navController.navigate("prompt_view/$promptId")
                },
                onNavigateToNew = {
                    navController.navigate("prompt_new")
                }
            )
        }

        // ========== VIEW-SCREEN (ReadOnly Ansicht) ==========
        composable(
            route = "prompt_view/{promptId}",
            arguments = listOf(navArgument("promptId") { type = NavType.LongType })
        ) { backStackEntry ->
            val promptId = backStackEntry.arguments?.getLong("promptId") ?: return@composable
            PromptViewScreen(
                promptId = promptId,
                repository = repository,
                onNavigateToEdit = { id ->
                    navController.navigate("prompt_edit/$id")
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ========== EDIT-SCREEN (Bearbeiten mit Versionierung) ==========
        composable(
            route = "prompt_edit/{promptId}",
            arguments = listOf(navArgument("promptId") { type = NavType.LongType })
        ) { backStackEntry ->
            val promptId = backStackEntry.arguments?.getLong("promptId") ?: return@composable
            PromptEditScreen(
                promptId = promptId,
                repository = repository,
                onNavigateBack = { navController.popBackStack() },
                onSaveComplete = { savedId ->
                    // Nach Speichern zur View-Screen der neuen/aktualisierten Version
                    navController.navigate("prompt_view/$savedId") {
                        popUpTo("prompt_view/$promptId") { inclusive = true }
                    }
                }
            )
        }

        // ========== NEU ERSTELLEN ==========
        composable("prompt_new") {
            PromptEditScreen(
                promptId = 0L, // Dummy ID für neuen Prompt
                repository = repository,
                onNavigateBack = { navController.popBackStack() },
                onSaveComplete = { savedId ->
                    // Nach Erstellen zur View-Screen des neuen Prompts
                    navController.navigate("prompt_view/$savedId") {
                        popUpTo("prompt_list")
                    }
                }
            )
        }
    }
}
