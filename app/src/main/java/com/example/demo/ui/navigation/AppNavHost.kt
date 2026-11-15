package com.example.demo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.demo.ui.detail.ArtworkDetailRoute
import com.example.demo.ui.list.ArtworkListRoute
import kotlinx.serialization.Serializable

/** Type-safe routes: compile-time checked arguments instead of "detail/{id}" strings. */
@Serializable
data object ArtworkListDestination

@Serializable
data class ArtworkDetailDestination(val artworkId: Int) {
    companion object {
        /** Key under which Navigation stores [artworkId] in the SavedStateHandle. */
        const val ARG_ARTWORK_ID = "artworkId"
    }
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = ArtworkListDestination,
        modifier = modifier,
    ) {
        // Screens receive navigation as lambdas (state hoisting) and never see the NavController.
        composable<ArtworkListDestination> {
            ArtworkListRoute(
                onNavigateToDetail = { id -> navController.navigate(ArtworkDetailDestination(id)) },
            )
        }
        composable<ArtworkDetailDestination> {
            ArtworkDetailRoute(onNavigateBack = { navController.navigateUp() })
        }
    }
}
