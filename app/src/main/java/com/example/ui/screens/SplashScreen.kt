package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.repository.WorkoutRepository
import com.example.ui.viewmodel.WorkoutViewModel
import com.example.util.JalaliDateUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull

@Composable
fun SplashScreen(
    repository: WorkoutRepository,
    workoutViewModel: WorkoutViewModel,
    onNavigateToNext: (String) -> Unit
) {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.85f) }

    LaunchedEffect(Unit) {
        // Check installed version vs stored last-seen version before any new plan is created
        workoutViewModel.checkLaunchStatusIfNeeded()

        // Subtle animate-in for logo and text
        alphaAnim.animateTo(1f, animationSpec = tween(700))
        scaleAnim.animateTo(1f, animationSpec = tween(700))

        // Wait ~2 seconds total for splash
        delay(1300)

        // Check if plan exists
        val days = repository.allDaysWithExercises.firstOrNull()
        val hasPlan = !days.isNullOrEmpty()

        val targetRoute = if (hasPlan) {
            com.example.ui.navigation.Screen.MainMenu.route
        } else {
            com.example.ui.navigation.Screen.PlanEditor.route
        }

        onNavigateToNext(targetRoute)
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("splash_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .alpha(alphaAnim.value)
                    .scale(scaleAnim.value)
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    tonalElevation = 4.dp
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.workout_icon),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier
                            .size(130.dp)
                            .clip(RoundedCornerShape(28.dp))
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "خوش آمدید",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "نسخه ${JalaliDateUtil.toPersianDigits(BuildConfig.VERSION_NAME)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.testTag("splash_version_text")
                )
            }
        }
    }
}
