package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.DayWithExercisesData
import com.example.data.repository.ExerciseData
import com.example.data.repository.WorkoutRepository
import com.example.util.JalaliDateUtil
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ExerciseEditorState(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val setsInput: String = "3",
    val repsInput: String = "10",
    val isNameError: Boolean = false,
    val isSetsError: Boolean = false,
    val isRepsError: Boolean = false
)

data class DayEditorState(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val exercises: List<ExerciseEditorState>
)

data class PlanEditorUiState(
    val days: List<DayEditorState> = emptyList(),
    val selectedDayIndex: Int = 0,
    val showDeleteConfirmDialog: Boolean = false,
    val showDeletePlanDialog: Boolean = false,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = true
)

class PlanEditorViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanEditorUiState())
    val uiState: StateFlow<PlanEditorUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<String>()
    val navigationEvents: SharedFlow<String> = _navigationEvents.asSharedFlow()

    init {
        loadInitialPlan()
    }

    fun loadInitialPlan() {
        viewModelScope.launch {
            val existing = repository.allDaysWithExercises.firstOrNull()
            if (!existing.isNullOrEmpty()) {
                val mappedDays = existing.mapIndexed { dayIdx, dayWithEx ->
                    DayEditorState(
                        id = dayWithEx.day.id.toString(),
                        name = dayWithEx.day.name.ifBlank { JalaliDateUtil.getPersianDayOrdinal(dayIdx + 1) },
                        exercises = if (dayWithEx.exercises.isNotEmpty()) {
                            dayWithEx.exercises.map { ex ->
                                ExerciseEditorState(
                                    id = ex.id.toString(),
                                    name = ex.name,
                                    setsInput = ex.plannedSets.toString(),
                                    repsInput = ex.plannedRepsPerSet.toString()
                                )
                            }
                        } else {
                            createDefaultExercisesForDay(dayIdx + 1)
                        }
                    )
                }
                _uiState.update {
                    it.copy(
                        days = mappedDays,
                        selectedDayIndex = 0,
                        isSaving = false,
                        isLoading = false
                    )
                }
            } else {
                // Default: 3 day boxes on first launch with 4 exercises each
                val defaultDays = listOf(
                    DayEditorState(
                        name = JalaliDateUtil.getPersianDayOrdinal(1),
                        exercises = listOf(
                            ExerciseEditorState(name = "پرس سینه هالتر", setsInput = "4", repsInput = "10"),
                            ExerciseEditorState(name = "بالا سینه دمبل", setsInput = "3", repsInput = "12"),
                            ExerciseEditorState(name = "پارالل", setsInput = "3", repsInput = "10"),
                            ExerciseEditorState(name = "شنا سوئدی", setsInput = "3", repsInput = "15")
                        )
                    ),
                    DayEditorState(
                        name = JalaliDateUtil.getPersianDayOrdinal(2),
                        exercises = listOf(
                            ExerciseEditorState(name = "اسکات هالتر", setsInput = "4", repsInput = "8"),
                            ExerciseEditorState(name = "پرس پا دستگاه", setsInput = "3", repsInput = "12"),
                            ExerciseEditorState(name = "جلو ران دستگاه", setsInput = "3", repsInput = "12"),
                            ExerciseEditorState(name = "ساق پا ایستاده", setsInput = "4", repsInput = "15")
                        )
                    ),
                    DayEditorState(
                        name = JalaliDateUtil.getPersianDayOrdinal(3),
                        exercises = listOf(
                            ExerciseEditorState(name = "بارفیکس", setsInput = "3", repsInput = "8"),
                            ExerciseEditorState(name = "زیربغل هالتر خم", setsInput = "4", repsInput = "10"),
                            ExerciseEditorState(name = "زیربغل قایقی", setsInput = "3", repsInput = "12"),
                            ExerciseEditorState(name = "فیله کمر", setsInput = "3", repsInput = "15")
                        )
                    )
                )
                _uiState.update {
                    it.copy(
                        days = defaultDays,
                        selectedDayIndex = 0,
                        isSaving = false,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun createDefaultExercisesForDay(dayNumber: Int): List<ExerciseEditorState> {
        return List(4) {
            ExerciseEditorState(
                name = "",
                setsInput = "3",
                repsInput = "10"
            )
        }
    }

    fun selectDay(index: Int) {
        if (index in _uiState.value.days.indices) {
            _uiState.update { it.copy(selectedDayIndex = index, errorMessage = null) }
        }
    }

    fun addNewDay() {
        val currentDays = _uiState.value.days
        val newDayNumber = currentDays.size + 1
        val newDay = DayEditorState(
            name = JalaliDateUtil.getPersianDayOrdinal(newDayNumber),
            exercises = createDefaultExercisesForDay(newDayNumber)
        )
        val updatedDays = currentDays + newDay
        _uiState.update {
            it.copy(
                days = updatedDays,
                selectedDayIndex = updatedDays.lastIndex,
                errorMessage = null
            )
        }
    }

    fun promptDeleteSelectedDay() {
        if (_uiState.value.days.isNotEmpty()) {
            _uiState.update { it.copy(showDeleteConfirmDialog = true) }
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    }

    fun confirmDeleteSelectedDay() {
        val currentDays = _uiState.value.days
        val currentIndex = _uiState.value.selectedDayIndex
        if (currentDays.isEmpty()) return

        val updatedDays = currentDays.toMutableList()
        updatedDays.removeAt(currentIndex)

        if (updatedDays.isEmpty()) {
            // Keep at least 1 day
            updatedDays.add(
                DayEditorState(
                    name = JalaliDateUtil.getPersianDayOrdinal(1),
                    exercises = createDefaultExercisesForDay(1)
                )
            )
        }

        val newIndex = currentIndex.coerceAtMost(updatedDays.lastIndex)
        _uiState.update {
            it.copy(
                days = updatedDays,
                selectedDayIndex = newIndex,
                showDeleteConfirmDialog = false,
                errorMessage = null
            )
        }
    }

    // === Delete Entire Plan ===
    fun promptDeletePlan() {
        _uiState.update { it.copy(showDeletePlanDialog = true) }
    }

    fun dismissDeletePlanDialog() {
        _uiState.update { it.copy(showDeletePlanDialog = false) }
    }

    fun confirmDeletePlan() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(showDeletePlanDialog = false, isLoading = true) }
                repository.deletePlan()

                // Reset to first-launch state with 3 default days and 4 default exercises
                val defaultDays = listOf(
                    DayEditorState(
                        name = JalaliDateUtil.getPersianDayOrdinal(1),
                        exercises = createDefaultExercisesForDay(1)
                    ),
                    DayEditorState(
                        name = JalaliDateUtil.getPersianDayOrdinal(2),
                        exercises = createDefaultExercisesForDay(2)
                    ),
                    DayEditorState(
                        name = JalaliDateUtil.getPersianDayOrdinal(3),
                        exercises = createDefaultExercisesForDay(3)
                    )
                )

                _uiState.update {
                    it.copy(
                        days = defaultDays,
                        selectedDayIndex = 0,
                        isLoading = false,
                        errorMessage = null,
                        snackbarMessage = "برنامه‌ی تمرینی با موفقیت حذف شد"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        snackbarMessage = "خطا در حذف برنامه: ${e.localizedMessage ?: "دوباره تلاش کنید"}"
                    )
                }
            }
        }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun addNewExercise() {
        val state = _uiState.value
        val dayIndex = state.selectedDayIndex
        if (dayIndex !in state.days.indices) return

        val targetDay = state.days[dayIndex]
        val updatedExercises = targetDay.exercises + ExerciseEditorState(
            name = "",
            setsInput = "3",
            repsInput = "10"
        )
        val updatedDays = state.days.toMutableList()
        updatedDays[dayIndex] = targetDay.copy(exercises = updatedExercises)

        _uiState.update { it.copy(days = updatedDays, errorMessage = null) }
    }

    fun deleteExercise(exerciseIndex: Int) {
        val state = _uiState.value
        val dayIndex = state.selectedDayIndex
        if (dayIndex !in state.days.indices) return

        val targetDay = state.days[dayIndex]
        if (exerciseIndex !in targetDay.exercises.indices) return

        val updatedExercises = targetDay.exercises.toMutableList()
        updatedExercises.removeAt(exerciseIndex)

        val updatedDays = state.days.toMutableList()
        updatedDays[dayIndex] = targetDay.copy(exercises = updatedExercises)

        _uiState.update { it.copy(days = updatedDays, errorMessage = null) }
    }

    fun updateExerciseName(exerciseIndex: Int, newName: String) {
        updateExercise(exerciseIndex) { it.copy(name = newName, isNameError = false) }
    }

    fun updateExerciseSets(exerciseIndex: Int, newSets: String) {
        val clean = newSets.filter { it.isDigit() }
        updateExercise(exerciseIndex) { it.copy(setsInput = clean, isSetsError = false) }
    }

    fun updateExerciseReps(exerciseIndex: Int, newReps: String) {
        val clean = newReps.filter { it.isDigit() }
        updateExercise(exerciseIndex) { it.copy(repsInput = clean, isRepsError = false) }
    }

    private fun updateExercise(
        exerciseIndex: Int,
        transform: (ExerciseEditorState) -> ExerciseEditorState
    ) {
        val state = _uiState.value
        val dayIndex = state.selectedDayIndex
        if (dayIndex !in state.days.indices) return

        val targetDay = state.days[dayIndex]
        if (exerciseIndex !in targetDay.exercises.indices) return

        val updatedExercises = targetDay.exercises.toMutableList()
        updatedExercises[exerciseIndex] = transform(updatedExercises[exerciseIndex])

        val updatedDays = state.days.toMutableList()
        updatedDays[dayIndex] = targetDay.copy(exercises = updatedExercises)

        _uiState.update { it.copy(days = updatedDays) }
    }

    fun savePlanAndFinish() {
        val state = _uiState.value
        if (state.days.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "حداقل یک روز تمرینی باید وجود داشته باشد") }
            return
        }

        var hasValidationError = false
        val validatedDays = state.days.map { day ->
            val validatedExercises = day.exercises.map { ex ->
                val nameValid = ex.name.isNotBlank()
                val sets = ex.setsInput.toIntOrNull() ?: 0
                val setsValid = sets > 0
                val reps = ex.repsInput.toIntOrNull() ?: 0
                val repsValid = reps > 0

                if (!nameValid || !setsValid || !repsValid) {
                    hasValidationError = true
                }
                ex.copy(
                    isNameError = !nameValid,
                    isSetsError = !setsValid,
                    isRepsError = !repsValid
                )
            }
            day.copy(exercises = validatedExercises)
        }

        if (hasValidationError) {
            _uiState.update {
                it.copy(
                    days = validatedDays,
                    errorMessage = "لطفاً نام همه حرکات و مقادیر مثبت ست و تکرار را کامل کنید"
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true, errorMessage = null) }
                val planToSave = state.days.map { day ->
                    DayWithExercisesData(
                        name = day.name,
                        exercises = day.exercises.map { ex ->
                            ExerciseData(
                                name = ex.name.trim(),
                                plannedSets = ex.setsInput.toIntOrNull() ?: 3,
                                plannedRepsPerSet = ex.repsInput.toIntOrNull() ?: 10
                            )
                        }
                    )
                }

                // Atomic transaction to replace the plan
                repository.saveFullPlan(planToSave)

                // Reset Plan Editor state from database after saving so it does not reference deleted IDs
                loadInitialPlan()

                _navigationEvents.emit("navigate_to_main_menu")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        snackbarMessage = "خطا در ذخیره برنامه تمرینی: ${e.localizedMessage ?: "لطفاً دوباره تلاش کنید"}"
                    )
                }
            }
        }
    }
}
