package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.viewmodel.EditableSetRow
import com.example.ui.viewmodel.WorkoutViewModel
import com.example.util.JalaliDateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(
    sessionId: Long,
    viewModel: WorkoutViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReview: (Long) -> Unit
) {
    val state by viewModel.sessionState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.flushCurrentComment()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            if (event.startsWith("navigate_to_review/")) {
                val targetSessionId = event.removePrefix("navigate_to_review/").toLongOrNull() ?: sessionId
                onNavigateToReview(targetSessionId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.workout_session_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = state.jalaliDate.toFullFormattedString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (state.currentExerciseIndex > 0) {
                                viewModel.goToPreviousExercise()
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("workout_session_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isFirstExercise = state.currentExerciseIndex <= 0
                    val isLast = state.isLastExercise

                    // In RTL, first item sits on the right side (1/4 width of Next/Finish button)
                    OutlinedButton(
                        onClick = { viewModel.goToPreviousExercise() },
                        enabled = !isFirstExercise,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("workout_session_prev_exercise_button"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (!isFirstExercise) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            }
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.previous_exercise),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // In RTL, second item sits on the left side (weight 4f)
                    Button(
                        onClick = { viewModel.goToNextExerciseOrFinish() },
                        modifier = Modifier
                            .weight(4f)
                            .height(54.dp)
                            .testTag(if (isLast) "workout_session_finish_button" else "workout_session_next_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLast) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (isLast) stringResource(R.string.finish) else stringResource(R.string.next),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isLast) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val exercise = state.currentExercise
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Exercise Progress Bar
                if (state.exercises.isNotEmpty()) {
                    val progress = (state.currentExerciseIndex + 1).toFloat() / state.exercises.size
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // Exercise Header Section
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = exercise?.name ?: "",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (state.exercises.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${JalaliDateUtil.toPersianDigits(state.currentExerciseIndex + 1)} از ${JalaliDateUtil.toPersianDigits(state.exercises.size)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Subtitle: Plan summary e.g. "۳ ست × ۱۲ تکرار"
                        if (exercise != null) {
                            Text(
                                text = "برنامه: ${JalaliDateUtil.toPersianDigits(exercise.plannedSets)} ست × ${JalaliDateUtil.toPersianDigits(exercise.plannedRepsPerSet)} تکرار",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Planned set rows
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 8.dp,
                        bottom = 20.dp
                    )
                ) {
                    items(state.setRows, key = { it.setNumber }) { row ->
                        SetRowCard(
                            row = row,
                            onWeightChange = { viewModel.updateSetWeight(row.setNumber, it) },
                            onRepsChange = { viewModel.updateSetReps(row.setNumber, it) },
                            onToggleCompleted = { viewModel.toggleSetCompleted(row.setNumber) }
                        )
                    }

                    item(key = "exercise_comment_field") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.currentExerciseComment,
                            onValueChange = { viewModel.updateExerciseComment(it) },
                            label = { Text(text = "کامنت") },
                            placeholder = { Text(text = "یادداشت یا کامنت مربوط به این حرکت در این جلسه...") },
                            singleLine = false,
                            minLines = 3,
                            maxLines = Int.MAX_VALUE,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Default
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("exercise_comment_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SetRowCard(
    row: EditableSetRow,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onToggleCompleted: () -> Unit
) {
    val isCompleted = row.isCompleted
    val cardBackground = if (isCompleted) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isCompleted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("set_row_card_${row.setNumber}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Set number label on left
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = JalaliDateUtil.toPersianDigits(row.setNumber),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Weight input (kg, decimal allowed)
            OutlinedTextField(
                value = row.weightInput,
                onValueChange = onWeightChange,
                label = { Text(text = "وزن (kg)", style = MaterialTheme.typography.labelSmall) },
                placeholder = { Text("0") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .weight(1f)
                    .testTag("set_weight_input_${row.setNumber}"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            // Reps input
            OutlinedTextField(
                value = row.repsInput,
                onValueChange = onRepsChange,
                label = { Text(text = "تکرار", style = MaterialTheme.typography.labelSmall) },
                placeholder = { Text("0") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .testTag("set_reps_input_${row.setNumber}"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            // Completed Checkbox
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggleCompleted() },
                modifier = Modifier.testTag("set_completed_checkbox_${row.setNumber}"),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
