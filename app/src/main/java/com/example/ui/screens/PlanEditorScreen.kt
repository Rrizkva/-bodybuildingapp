package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.viewmodel.ExerciseEditorState
import com.example.ui.viewmodel.PlanEditorViewModel
import com.example.util.JalaliDateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(
    viewModel: PlanEditorViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToMainMenu: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = androidx.compose.runtime.remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            if (event == "navigate_to_main_menu") {
                onNavigateToMainMenu()
            }
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.plan_editor_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.plan_editor_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("plan_editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (state.days.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.promptDeleteSelectedDay() },
                            modifier = Modifier.testTag("delete_selected_day_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_day),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (state.errorMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .background(
                                    MaterialTheme.colorScheme.errorContainer,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.savePlanAndFinish() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("finish_planning_button"),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !state.isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.finish_planning),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.promptDeletePlan() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("delete_plan_button"),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !state.isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حذف برنامه",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Top: Horizontal row of day boxes
                DaysTabBar(
                    days = state.days.map { it.name },
                    selectedIndex = state.selectedDayIndex,
                    onSelectDay = { viewModel.selectDay(it) },
                    onAddNewDay = { viewModel.addNewDay() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                val currentDay = state.days.getOrNull(state.selectedDayIndex)
                if (currentDay != null) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                    ) {
                        itemsIndexed(
                            items = currentDay.exercises,
                            key = { _, ex -> ex.id }
                        ) { exerciseIndex, exercise ->
                            ExerciseEditorCard(
                                exerciseIndex = exerciseIndex,
                                exercise = exercise,
                                onNameChange = { viewModel.updateExerciseName(exerciseIndex, it) },
                                onSetsChange = { viewModel.updateExerciseSets(exerciseIndex, it) },
                                onRepsChange = { viewModel.updateExerciseReps(exerciseIndex, it) },
                                onDelete = { viewModel.deleteExercise(exerciseIndex) }
                            )
                        }

                        item {
                            OutlinedButton(
                                onClick = { viewModel.addNewExercise() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("add_exercise_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.add_exercise),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showDeleteConfirmDialog) {
        val selectedDay = state.days.getOrNull(state.selectedDayIndex)
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = {
                Text(
                    text = stringResource(R.string.delete_day),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "${stringResource(R.string.confirm_delete_day)} (${selectedDay?.name ?: ""})",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteSelectedDay() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }

    if (state.showDeletePlanDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeletePlanDialog() },
            title = {
                Text(
                    text = "حذف برنامه",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "آیا می‌خواهید ادامه دهید؟ برنامه‌ی تمرینی فعلی حذف می‌شود. سابقه‌ی جلسات انجام‌شده در بخش آنالیز باقی می‌ماند.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeletePlan() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_plan_button")
                ) {
                    Text(text = "بله")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissDeletePlanDialog() },
                    modifier = Modifier.testTag("cancel_delete_plan_button")
                ) {
                    Text(text = "خیر")
                }
            }
        )
    }
}

@Composable
fun DaysTabBar(
    days: List<String>,
    selectedIndex: Int,
    onSelectDay: (Int) -> Unit,
    onAddNewDay: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            days.forEachIndexed { index, dayName ->
                val isSelected = index == selectedIndex
                DayChip(
                    name = dayName,
                    isSelected = isSelected,
                    onClick = { onSelectDay(index) },
                    testTag = "day_tab_$index"
                )
            }

            // Plus button to add a new day
            IconButton(
                onClick = onAddNewDay,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .testTag("add_day_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_day),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun DayChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (isSelected) 3.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
fun ExerciseEditorCard(
    exerciseIndex: Int,
    exercise: ExerciseEditorState,
    onNameChange: (String) -> Unit,
    onSetsChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exercise_editor_card_$exerciseIndex"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "حرکت ${JalaliDateUtil.toPersianDigits(exerciseIndex + 1)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_exercise_button_$exerciseIndex")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_exercise),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Exercise name field
            OutlinedTextField(
                value = exercise.name,
                onValueChange = onNameChange,
                label = { Text(text = stringResource(R.string.exercise_name_label)) },
                placeholder = { Text(text = stringResource(R.string.exercise_name_hint)) },
                isError = exercise.isNameError,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("exercise_name_input_$exerciseIndex"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Sets and Reps inputs side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = exercise.setsInput,
                    onValueChange = onSetsChange,
                    label = { Text(text = stringResource(R.string.planned_sets_label)) },
                    isError = exercise.isSetsError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("exercise_sets_input_$exerciseIndex"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = exercise.repsInput,
                    onValueChange = onRepsChange,
                    label = { Text(text = stringResource(R.string.planned_reps_label)) },
                    isError = exercise.isRepsError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("exercise_reps_input_$exerciseIndex"),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}
