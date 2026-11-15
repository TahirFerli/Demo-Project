package com.example.demo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.demo.ui.detail.PhotoDetailRoute
import com.example.demo.ui.list.PhotoListRoute
import kotlinx.serialization.Serializable

/** Type-safe routes: compile-time checked arguments instead of "detail/{id}" strings. */
@Serializable
data object PhotoListDestination

@Serializable
data class PhotoDetailDestination(val photoId: String) {
    companion object {
        /** Key under which Navigation stores [photoId] in the SavedStateHandle. */
        const val ARG_PHOTO_ID = "photoId"
    }
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = PhotoListDestination,
        modifier = modifier,
    ) {
        // Screens receive navigation as lambdas (state hoisting) and never see the NavController.
        composable<PhotoListDestination> {
            PhotoListRoute(
                onNavigateToDetail = { id -> navController.navigate(PhotoDetailDestination(id)) },
            )
        }
        composable<PhotoDetailDestination> {
            PhotoDetailRoute(onNavigateBack = { navController.navigateUp() })
        }
    }
}
