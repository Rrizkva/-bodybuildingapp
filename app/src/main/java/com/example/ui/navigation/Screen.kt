package com.example.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object MainMenu : Screen("main_menu")
    data object PlanHub : Screen("plan_hub")
    data object PlanEditor : Screen("plan_editor")
    data object PlanManagement : Screen("plan_management")
    data object StartNewDay : Screen("start_new_day")
    data object WorkoutSession : Screen("workout_session/{sessionId}") {
        fun createRoute(sessionId: Long) = "workout_session/$sessionId"
    }
    data object SessionReview : Screen("session_review/{sessionId}") {
        fun createRoute(sessionId: Long) = "session_review/$sessionId"
    }
    // Screen 6: 3-Level Exercise Analysis Flow
    data object ExerciseAnalysis : Screen("exercise_analysis") // Level 1: Session list
    data object SessionExercises : Screen("session_exercises/{sessionId}") { // Level 2: Exercises of that session
        fun createRoute(sessionId: Long) = "session_exercises/$sessionId"
    }
    data object SessionSummary : Screen("session_summary/{sessionId}") { // Level 3: Session summary
        fun createRoute(sessionId: Long) = "session_summary/$sessionId"
    }
}
