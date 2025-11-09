package com.promptmanager.presentation.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.promptmanager.data.repository.PromptRepository
import com.promptmanager.presentation.viewmodel.PromptDetailViewModel
import com.promptmanager.presentation.viewmodel.PromptListViewModel

/**
 * Root Composable für die App-Navigation.
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
                    navController.navigate("prompt_detail/$promptId")
                },
                onNavigateToNew = {
                    navController.navigate("prompt_new")
                }
            )
        }

        // ========== DETAIL-SCREEN (Bearbeiten) ==========
        composable(
            route = "prompt_detail/{promptId}",
            arguments = listOf(navArgument("promptId") { type = NavType.LongType })
        ) { backStackEntry ->
            val promptId = backStackEntry.arguments?.getLong("promptId")
            val viewModel = PromptDetailViewModel(repository, promptId)
            PromptDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ========== NEU ERSTELLEN ==========
        composable("prompt_new") {
            val viewModel = PromptDetailViewModel(repository, promptId = null)
            PromptDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
