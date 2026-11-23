package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.database.AppDatabase
import com.example.data.preferences.DataStoreVersionPreferences
import com.example.data.repository.WorkoutRepository
import com.example.ui.navigation.Screen
import com.example.ui.screens.ExerciseAnalysisScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.PlanEditorScreen
import com.example.ui.screens.PlanHubScreen
import com.example.ui.screens.PlanManagementScreen
import com.example.ui.screens.SessionExercisesScreen
import com.example.ui.screens.SessionReviewScreen
import com.example.ui.screens.SessionSummaryScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StartNewDayScreen
import com.example.ui.screens.WorkoutSessionScreen
import com.example.ui.theme.WorkoutTheme
import com.example.ui.viewmodel.AnalysisViewModel
import com.example.ui.viewmodel.PlanEditorViewModel
import com.example.ui.viewmodel.WorkoutViewModel
import com.example.ui.viewmodel.WorkoutViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = WorkoutRepository(database.workoutDao())
        val versionPreferences = DataStoreVersionPreferences(applicationContext)
        val viewModelFactory = WorkoutViewModelFactory(repository, versionPreferences)

        setContent {
            WorkoutTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    val planEditorViewModel: PlanEditorViewModel = viewModel(factory = viewModelFactory)
                    val workoutViewModel: WorkoutViewModel = viewModel(factory = viewModelFactory)
                    val analysisViewModel: AnalysisViewModel = viewModel(factory = viewModelFactory)

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Splash.route
                    ) {
                        composable(Screen.Splash.route) {
                            SplashScreen(
                                repository = repository,
                                workoutViewModel = workoutViewModel,
                                onNavigateToNext = { targetRoute ->
                                    navController.navigate(targetRoute) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.MainMenu.route) {
                            MainMenuScreen(
                                viewModel = workoutViewModel,
                                onNavigateToPlanHub = {
                                    navController.navigate(Screen.PlanHub.route)
                                },
                                onNavigateToPlanEditor = {
                                    navController.navigate(Screen.PlanEditor.route)
                                },
                                onNavigateToStartNewDay = {
                                    navController.navigate(Screen.StartNewDay.route)
                                },
                                onNavigateToAnalysis = {
                                    navController.navigate(Screen.ExerciseAnalysis.route)
                                },
                                onResumeSession = { sessionId ->
                                    navController.navigate(Screen.WorkoutSession.createRoute(sessionId))
                                }
                            )
                        }

                        composable(Screen.PlanHub.route) {
                            PlanHubScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToPlanEditor = {
                                    navController.navigate(Screen.PlanEditor.route)
                                },
                                onNavigateToPlanManagement = {
                                    navController.navigate(Screen.PlanManagement.route)
                                }
                            )
                        }

                        composable(Screen.PlanManagement.route) {
                            PlanManagementScreen(
                                viewModel = workoutViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(Screen.PlanEditor.route) {
                            PlanEditorScreen(
                                viewModel = planEditorViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToMainMenu = {
                                    navController.navigate(Screen.MainMenu.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.StartNewDay.route) {
                            StartNewDayScreen(
                                viewModel = workoutViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToSession = { sessionId ->
                                    navController.navigate(Screen.WorkoutSession.createRoute(sessionId))
                                }
                            )
                        }

                        composable(
                            route = Screen.WorkoutSession.route,
                            arguments = listOf(
                                navArgument("sessionId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                            WorkoutSessionScreen(
                                sessionId = sessionId,
                                viewModel = workoutViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToReview = { sId ->
                                    navController.navigate(Screen.SessionReview.createRoute(sId))
                                }
                            )
                        }

                        composable(
                            route = Screen.SessionReview.route,
                            arguments = listOf(
                                navArgument("sessionId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                            SessionReviewScreen(
                                sessionId = sessionId,
                                viewModel = workoutViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToMainMenu = {
                                    navController.navigate(Screen.MainMenu.route) {
                                        popUpTo(Screen.MainMenu.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Level 1: Session list
                        composable(Screen.ExerciseAnalysis.route) {
                            ExerciseAnalysisScreen(
                                viewModel = analysisViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onSelectSession = { sessionId ->
                                    navController.navigate(Screen.SessionExercises.createRoute(sessionId))
                                }
                            )
                        }

                        // Level 2: Exercises of that session
                        composable(
                            route = Screen.SessionExercises.route,
                            arguments = listOf(
                                navArgument("sessionId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                            SessionExercisesScreen(
                                sessionId = sessionId,
                                viewModel = analysisViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToSummary = { sId ->
                                    navController.navigate(Screen.SessionSummary.createRoute(sId))
                                }
                            )
                        }

                        // Level 3: Session summary
                        composable(
                            route = Screen.SessionSummary.route,
                            arguments = listOf(
                                navArgument("sessionId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                            SessionSummaryScreen(
                                sessionId = sessionId,
                                viewModel = analysisViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
