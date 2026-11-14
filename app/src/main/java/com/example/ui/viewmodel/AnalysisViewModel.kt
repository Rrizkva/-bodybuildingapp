package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.WorkoutRepository
import com.example.util.JalaliDate
import com.example.util.JalaliDateUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Level 1 Model
data class SessionListItem(
    val sessionId: Long,
    val dayName: String,
    val startedAtEpoch: Long,
    val jalaliDate: JalaliDate,
    val isCompleted: Boolean
)

// Level 2 & 2b Models
data class SessionSetDisplayItem(
    val setNumber: Int,
    val weight: Double?,
    val reps: Int?,
    val isCompleted: Boolean
)

data class SessionExerciseDetail(
    val exerciseId: Long,
    val name: String,
    val orderIndex: Int,
    val plannedSets: Int,
    val plannedRepsPerSet: Int,
    val sets: List<SessionSetDisplayItem>,
    val comment: String? = null,
    val isExpanded: Boolean = false
)

data class SessionExercisesUiState(
    val sessionId: Long = 0L,
    val dayName: String = "",
    val jalaliDate: JalaliDate = JalaliDateUtil.fromEpochMillis(System.currentTimeMillis()),
    val exercises: List<SessionExerciseDetail> = emptyList(),
    val expandedExerciseIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true
)

// Level 3 Model
data class SessionSummaryUiState(
    val sessionId: Long = 0L,
    val dayName: String = "",
    val jalaliDate: JalaliDate = JalaliDateUtil.fromEpochMillis(System.currentTimeMillis()),
    val pressureRating: Int = 5,
    val difficultyRating: Int = 5,
    val note: String? = null,
    val isLoading: Boolean = true
)

class AnalysisViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    // === Level 1: Session list (most recent first) ===
    val sessionsList: StateFlow<List<SessionListItem>> =
        repository.allSessionsWithDay.map { list ->
            list.map { item ->
                val fallbackName = item.session.dayName.ifBlank { JalaliDateUtil.getPersianDayOrdinal(1) }
                SessionListItem(
                    sessionId = item.session.id,
                    dayName = item.day?.name ?: fallbackName,
                    startedAtEpoch = item.session.startedAt,
                    jalaliDate = JalaliDateUtil.fromEpochMillis(item.session.startedAt),
                    isCompleted = item.session.isCompleted
                )
            }.sortedByDescending { it.startedAtEpoch }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _sessionToDelete = MutableStateFlow<Long?>(null)
    val sessionToDelete: StateFlow<Long?> = _sessionToDelete.asStateFlow()

    fun promptDeleteSession(sessionId: Long) {
        _sessionToDelete.value = sessionId
    }

    fun dismissDeleteSessionDialog() {
        _sessionToDelete.value = null
    }

    fun confirmDeleteSession() {
        val id = _sessionToDelete.value ?: return
        viewModelScope.launch {
            repository.deleteSession(id)
            _sessionToDelete.value = null
        }
    }

    // === Level 2 & 2b: Exercises of that session ===
    private val _sessionExercisesState = MutableStateFlow(SessionExercisesUiState())
    val sessionExercisesState: StateFlow<SessionExercisesUiState> = _sessionExercisesState.asStateFlow()

    fun loadSessionExercises(sessionId: Long) {
        viewModelScope.launch {
            _sessionExercisesState.update { it.copy(isLoading = true, sessionId = sessionId) }

            val sessionWithDay = repository.getSessionWithDayDirect(sessionId) ?: return@launch
            val fallbackName = sessionWithDay.session.dayName.ifBlank { JalaliDateUtil.getPersianDayOrdinal(1) }
            val dayName = sessionWithDay.day?.name ?: fallbackName
            val jalaliDate = JalaliDateUtil.fromEpochMillis(sessionWithDay.session.startedAt)

            val dayExercises = if (sessionWithDay.session.dayId != 0L) {
                repository.getExercisesForDayDirect(sessionWithDay.session.dayId)
            } else emptyList()
            val sessionSetLogs = repository.getSetLogsForSessionDirect(sessionId)
                .groupBy { it.exerciseId }
            val sessionComments = repository.getExerciseCommentsForSessionDirect(sessionId)
                .associateBy { it.exerciseId }

            val exerciseDetails = if (dayExercises.isNotEmpty()) {
                dayExercises.map { ex ->
                    val logsForEx = sessionSetLogs[ex.id]?.associateBy { it.setNumber } ?: emptyMap()
                    val setRows = (1..ex.plannedSets).map { setNumber ->
                        val log = logsForEx[setNumber]
                        if (log != null && log.completed) {
                            SessionSetDisplayItem(
                                setNumber = setNumber,
                                weight = log.weight,
                                reps = log.reps,
                                isCompleted = true
                            )
                        } else {
                            SessionSetDisplayItem(
                                setNumber = setNumber,
                                weight = null,
                                reps = null,
                                isCompleted = false
                            )
                        }
                    }

                    SessionExerciseDetail(
                        exerciseId = ex.id,
                        name = ex.name,
                        orderIndex = ex.orderIndex,
                        plannedSets = ex.plannedSets,
                        plannedRepsPerSet = ex.plannedRepsPerSet,
                        sets = setRows,
                        comment = sessionComments[ex.id]?.comment?.takeIf { it.isNotBlank() },
                        isExpanded = false
                    )
                }
            } else {
                // Reconstruct from logged sets & comments when original plan was deleted or replaced
                val allExerciseIds = (sessionSetLogs.keys + sessionComments.keys).toList()
                allExerciseIds.mapIndexed { idx, exId ->
                    val logs = sessionSetLogs[exId].orEmpty()
                    val commentEntity = sessionComments[exId]
                    val exName = logs.firstOrNull()?.exerciseName?.takeIf { it.isNotBlank() }
                        ?: commentEntity?.exerciseName?.takeIf { it.isNotBlank() }
                        ?: "حرکت ${JalaliDateUtil.toPersianDigits(idx + 1)}"
                    val maxSetNum = maxOf(logs.size, logs.maxOfOrNull { it.setNumber } ?: 1)
                    val logsForEx = logs.associateBy { it.setNumber }
                    val setRows = (1..maxSetNum).map { setNumber ->
                        val log = logsForEx[setNumber]
                        if (log != null && log.completed) {
                            SessionSetDisplayItem(
                                setNumber = setNumber,
                                weight = log.weight,
                                reps = log.reps,
                                isCompleted = true
                            )
                        } else {
                            SessionSetDisplayItem(
                                setNumber = setNumber,
                                weight = null,
                                reps = null,
                                isCompleted = false
                            )
                        }
                    }
                    SessionExerciseDetail(
                        exerciseId = exId,
                        name = exName,
                        orderIndex = idx + 1,
                        plannedSets = maxSetNum,
                        plannedRepsPerSet = logs.firstOrNull()?.reps ?: 10,
                        sets = setRows,
                        comment = commentEntity?.comment?.takeIf { it.isNotBlank() },
                        isExpanded = false
                    )
                }
            }

            _sessionExercisesState.update {
                it.copy(
                    sessionId = sessionId,
                    dayName = dayName,
                    jalaliDate = jalaliDate,
                    exercises = exerciseDetails,
                    expandedExerciseIds = emptySet(),
                    isLoading = false
                )
            }
        }
    }

    fun toggleExerciseExpanded(exerciseId: Long) {
        _sessionExercisesState.update { state ->
            val newExpanded = if (exerciseId in state.expandedExerciseIds) {
                state.expandedExerciseIds - exerciseId
            } else {
                state.expandedExerciseIds + exerciseId
            }
            val updatedList = state.exercises.map { ex ->
                ex.copy(isExpanded = ex.exerciseId in newExpanded)
            }
            state.copy(
                expandedExerciseIds = newExpanded,
                exercises = updatedList
            )
        }
    }

    // === Level 3: Session summary ===
    private val _sessionSummaryState = MutableStateFlow(SessionSummaryUiState())
    val sessionSummaryState: StateFlow<SessionSummaryUiState> = _sessionSummaryState.asStateFlow()

    fun loadSessionSummary(sessionId: Long) {
        viewModelScope.launch {
            _sessionSummaryState.update { it.copy(isLoading = true, sessionId = sessionId) }

            val sessionWithDay = repository.getSessionWithDayDirect(sessionId) ?: return@launch
            val fallbackName = sessionWithDay.session.dayName.ifBlank { JalaliDateUtil.getPersianDayOrdinal(1) }
            val dayName = sessionWithDay.day?.name ?: fallbackName
            val jalaliDate = JalaliDateUtil.fromEpochMillis(sessionWithDay.session.startedAt)

            _sessionSummaryState.update {
                it.copy(
                    sessionId = sessionId,
                    dayName = dayName,
                    jalaliDate = jalaliDate,
                    pressureRating = sessionWithDay.session.pressureRating,
                    difficultyRating = sessionWithDay.session.difficultyRating,
                    note = sessionWithDay.session.note,
                    isLoading = false
                )
            }
        }
    }
}
