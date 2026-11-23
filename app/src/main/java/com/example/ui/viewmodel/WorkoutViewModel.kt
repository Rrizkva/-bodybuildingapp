package com.example.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.backup.ValidatedBackupData
import com.example.data.backup.WorkoutBackupManager
import com.example.data.model.DayWithExercises
import com.example.data.model.Exercise
import com.example.data.model.Session
import com.example.data.model.SetLog
import com.example.data.preferences.VersionComparator
import com.example.data.preferences.VersionPreferences
import com.example.data.repository.PreviousExerciseReference
import com.example.data.repository.WorkoutRepository
import com.example.util.JalaliDate
import com.example.util.JalaliDateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditableSetRow(
    val setNumber: Int,
    val weightInput: String = "",
    val repsInput: String = "",
    val isCompleted: Boolean = false
)

data class WorkoutSessionUiState(
    val sessionId: Long = 0,
    val session: Session? = null,
    val jalaliDate: JalaliDate = JalaliDateUtil.fromEpochMillis(System.currentTimeMillis()),
    val exercises: List<Exercise> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val currentExercise: Exercise? = null,
    val setRows: List<EditableSetRow> = emptyList(),
    val previousReference: PreviousExerciseReference? = null,
    val currentExerciseComment: String = "",
    val isLastExercise: Boolean = false,
    val isLoading: Boolean = true
)

data class SessionReviewUiState(
    val sessionId: Long = 0,
    val pressureRating: Int = 5,
    val difficultyRating: Int = 5,
    val note: String = "",
    val isSaving: Boolean = false,
    val showCompletionCelebration: Boolean = false
)

data class BackupUiState(
    val isProcessing: Boolean = false,
    val pendingImportData: ValidatedBackupData? = null,
    val pendingImportSessionCount: Int = 0,
    val snackbarMessage: String? = null
)

class WorkoutViewModel(
    private val repository: WorkoutRepository,
    private val versionPreferences: VersionPreferences? = null
) : ViewModel() {

    val allDaysWithExercises: StateFlow<List<DayWithExercises>> =
        repository.allDaysWithExercises.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedDayIdForNewSession = MutableStateFlow<Long?>(null)
    val selectedDayIdForNewSession: StateFlow<Long?> = _selectedDayIdForNewSession.asStateFlow()

    private val _sessionState = MutableStateFlow(WorkoutSessionUiState())
    val sessionState: StateFlow<WorkoutSessionUiState> = _sessionState.asStateFlow()

    private val _reviewState = MutableStateFlow(SessionReviewUiState())
    val reviewState: StateFlow<SessionReviewUiState> = _reviewState.asStateFlow()

    private val _backupState = MutableStateFlow(BackupUiState())
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    private val _unfinishedSessionPrompt = MutableStateFlow<Session?>(null)
    val unfinishedSessionPrompt: StateFlow<Session?> = _unfinishedSessionPrompt.asStateFlow()

    private val _showWhatsNewDialog = MutableStateFlow(false)
    val showWhatsNewDialog: StateFlow<Boolean> = _showWhatsNewDialog.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<String>()
    val navigationEvents: SharedFlow<String> = _navigationEvents.asSharedFlow()

    private var commentDebounceJob: Job? = null
    private var launchStatusChecked = false

    fun checkLaunchStatusIfNeeded(currentVersionName: String = BuildConfig.VERSION_NAME) {
        if (launchStatusChecked) return
        launchStatusChecked = true
        viewModelScope.launch {
            if (versionPreferences != null) {
                val storedVersion = versionPreferences.getLastSeenVersion()
                if (storedVersion == null) {
                    val hasExistingData = repository.hasAnyExistingData()
                    if (hasExistingData) {
                        _showWhatsNewDialog.value = true
                    } else {
                        // Fresh install: do not show What's New dialog; store current version as seen
                        versionPreferences.setLastSeenVersion(currentVersionName)
                    }
                } else if (VersionComparator.isVersionNewer(currentVersionName, storedVersion)) {
                    _showWhatsNewDialog.value = true
                }
            }

            val resumable = repository.getLatestResumableSessionDirect()
            _unfinishedSessionPrompt.value = resumable
        }
    }

    fun checkLaunchUnfinishedSessionIfNeeded() {
        checkLaunchStatusIfNeeded()
    }

    fun dismissWhatsNewDialog(currentVersionName: String = BuildConfig.VERSION_NAME) {
        _showWhatsNewDialog.value = false
        viewModelScope.launch {
            versionPreferences?.setLastSeenVersion(currentVersionName)
        }
    }

    fun resumeUnfinishedSession() {
        val session = _unfinishedSessionPrompt.value ?: return
        _unfinishedSessionPrompt.value = null
        viewModelScope.launch {
            _navigationEvents.emit("resume_session/${session.id}")
        }
    }

    fun startFreshFromPrompt() {
        val session = _unfinishedSessionPrompt.value ?: return
        _unfinishedSessionPrompt.value = null
        viewModelScope.launch {
            repository.dismissSessionResume(session.id)
            _navigationEvents.emit("navigate_to_start_new_day_from_prompt")
        }
    }

    fun selectDayForNewSession(dayId: Long) {
        _selectedDayIdForNewSession.value = dayId
    }

    fun startSessionForSelectedDay() {
        val dayId = _selectedDayIdForNewSession.value ?: return
        viewModelScope.launch {
            val sessionId = repository.startNewSession(dayId)
            _navigationEvents.emit("navigate_to_session/$sessionId")
        }
    }

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            _sessionState.update { it.copy(isLoading = true, sessionId = sessionId) }
            val session = repository.getSessionByIdDirect(sessionId) ?: return@launch
            val exercises = repository.getExercisesForDayDirect(session.dayId)
            val date = JalaliDateUtil.fromEpochMillis(session.startedAt)
            val startIndex = if (exercises.isNotEmpty()) {
                session.currentExerciseIndex.coerceIn(0, exercises.lastIndex)
            } else {
                0
            }
            val startExercise = exercises.getOrNull(startIndex)

            _sessionState.update {
                it.copy(
                    session = session,
                    jalaliDate = date,
                    exercises = exercises,
                    currentExerciseIndex = startIndex,
                    currentExercise = startExercise,
                    previousReference = null,
                    currentExerciseComment = "",
                    isLastExercise = exercises.isEmpty() || startIndex == exercises.lastIndex,
                    isLoading = false
                )
            }

            if (startExercise != null) {
                loadSetsAndCommentForExercise(sessionId, session.startedAt, startExercise)
            }
        }
    }

    private suspend fun loadSetsAndCommentForExercise(
        sessionId: Long,
        sessionStartedAt: Long,
        exercise: Exercise
    ) {
        val existingLogs = repository.getSetLogsForSessionAndExerciseDirect(sessionId, exercise.id)
            .associateBy { it.setNumber }
        val existingComment = repository.getExerciseCommentDirect(sessionId, exercise.id)
        val previousReference = repository.getPreviousExerciseReference(
            currentSessionId = sessionId,
            currentStartedAt = sessionStartedAt,
            exerciseName = exercise.name,
            plannedSets = exercise.plannedSets
        )

        val rows = (1..exercise.plannedSets).map { setNumber ->
            val existing = existingLogs[setNumber]
            if (existing != null) {
                EditableSetRow(
                    setNumber = setNumber,
                    weightInput = if (existing.weight > 0) {
                        if (existing.weight % 1.0 == 0.0) existing.weight.toLong().toString()
                        else existing.weight.toString()
                    } else "",
                    repsInput = existing.reps.toString(),
                    isCompleted = existing.completed
                )
            } else {
                EditableSetRow(
                    setNumber = setNumber,
                    weightInput = "",
                    repsInput = exercise.plannedRepsPerSet.toString(),
                    isCompleted = false
                )
            }
        }

        _sessionState.update {
            it.copy(
                setRows = rows,
                previousReference = previousReference,
                currentExerciseComment = existingComment?.comment ?: ""
            )
        }
    }

    fun updateSetWeight(setNumber: Int, newWeight: String) {
        val filtered = newWeight.filter { it.isDigit() || it == '.' }
        updateSetRow(setNumber) { it.copy(weightInput = filtered) }
    }

    fun updateSetReps(setNumber: Int, newReps: String) {
        val filtered = newReps.filter { it.isDigit() }
        updateSetRow(setNumber) { it.copy(repsInput = filtered) }
    }

    fun toggleSetCompleted(setNumber: Int) {
        updateSetRow(setNumber) { it.copy(isCompleted = !it.isCompleted) }
    }

    private fun updateSetRow(
        setNumber: Int,
        transform: (EditableSetRow) -> EditableSetRow
    ) {
        val state = _sessionState.value
        val exercise = state.currentExercise ?: return
        val currentRows = state.setRows.toMutableList()
        val index = currentRows.indexOfFirst { it.setNumber == setNumber }
        if (index == -1) return

        val updatedRow = transform(currentRows[index])
        currentRows[index] = updatedRow
        _sessionState.update { it.copy(setRows = currentRows) }

        // Autosave to Room immediately
        viewModelScope.launch {
            val weight = updatedRow.weightInput.toDoubleOrNull() ?: 0.0
            val reps = updatedRow.repsInput.toIntOrNull() ?: exercise.plannedRepsPerSet
            repository.saveSetLog(
                SetLog(
                    sessionId = state.sessionId,
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    setNumber = setNumber,
                    weight = weight,
                    reps = reps,
                    completed = updatedRow.isCompleted
                )
            )
        }
    }

    fun updateExerciseComment(newComment: String) {
        _sessionState.update { it.copy(currentExerciseComment = newComment) }
        val state = _sessionState.value
        val exercise = state.currentExercise ?: return
        val sessionId = state.sessionId
        if (sessionId <= 0L) return

        commentDebounceJob?.cancel()
        commentDebounceJob = viewModelScope.launch {
            delay(500L)
            repository.saveExerciseComment(
                sessionId = sessionId,
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                commentText = newComment
            )
        }
    }

    fun flushCurrentComment() {
        commentDebounceJob?.cancel()
        val state = _sessionState.value
        val exercise = state.currentExercise ?: return
        val sessionId = state.sessionId
        if (sessionId <= 0L) return
        val commentSnapshot = state.currentExerciseComment

        viewModelScope.launch(NonCancellable) {
            repository.saveExerciseComment(
                sessionId = sessionId,
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                commentText = commentSnapshot
            )
        }
    }

    fun goToNextExerciseOrFinish() {
        flushCurrentComment()
        val state = _sessionState.value
        val nextIndex = state.currentExerciseIndex + 1

        if (nextIndex < state.exercises.size) {
            val nextExercise = state.exercises[nextIndex]
            val sessionStartedAt = state.session?.startedAt ?: System.currentTimeMillis()
            _sessionState.update {
                it.copy(
                    currentExerciseIndex = nextIndex,
                    currentExercise = nextExercise,
                    previousReference = null,
                    currentExerciseComment = "",
                    isLastExercise = (nextIndex == state.exercises.lastIndex)
                )
            }
            viewModelScope.launch {
                repository.updateSessionExerciseIndex(state.sessionId, nextIndex)
                loadSetsAndCommentForExercise(state.sessionId, sessionStartedAt, nextExercise)
            }
        } else {
            // Last exercise reached, navigate to Screen 5 (Session Review)
            viewModelScope.launch {
                _navigationEvents.emit("navigate_to_review/${state.sessionId}")
            }
        }
    }

    fun goToPreviousExercise() {
        flushCurrentComment()
        val state = _sessionState.value
        val prevIndex = state.currentExerciseIndex - 1
        if (prevIndex >= 0) {
            val prevExercise = state.exercises[prevIndex]
            val sessionStartedAt = state.session?.startedAt ?: System.currentTimeMillis()
            _sessionState.update {
                it.copy(
                    currentExerciseIndex = prevIndex,
                    currentExercise = prevExercise,
                    previousReference = null,
                    currentExerciseComment = "",
                    isLastExercise = (prevIndex == state.exercises.lastIndex)
                )
            }
            viewModelScope.launch {
                repository.updateSessionExerciseIndex(state.sessionId, prevIndex)
                loadSetsAndCommentForExercise(state.sessionId, sessionStartedAt, prevExercise)
            }
        }
    }

    // === Session Review ===
    fun loadReviewSession(sessionId: Long) {
        viewModelScope.launch {
            val session = repository.getSessionByIdDirect(sessionId)
            _reviewState.update {
                it.copy(
                    sessionId = sessionId,
                    pressureRating = session?.pressureRating ?: 5,
                    difficultyRating = session?.difficultyRating ?: 5,
                    note = session?.note ?: "",
                    isSaving = false,
                    showCompletionCelebration = false
                )
            }
        }
    }

    fun updatePressureRating(rating: Int) {
        _reviewState.update { it.copy(pressureRating = rating.coerceIn(1, 10)) }
    }

    fun updateDifficultyRating(rating: Int) {
        _reviewState.update { it.copy(difficultyRating = rating.coerceIn(1, 10)) }
    }

    fun updateReviewNote(note: String) {
        _reviewState.update { it.copy(note = note) }
    }

    fun saveReviewAndFinish() {
        val state = _reviewState.value
        viewModelScope.launch {
            _reviewState.update { it.copy(isSaving = true) }
            repository.finishSession(
                sessionId = state.sessionId,
                pressureRating = state.pressureRating,
                difficultyRating = state.difficultyRating,
                note = state.note
            )

            // Show message "خسته نباشی! تمرین تموم شد" for exactly 2 seconds
            _reviewState.update { it.copy(showCompletionCelebration = true, isSaving = false) }
            delay(2000)
            _navigationEvents.emit("navigate_to_main_menu_after_session")
        }
    }

    // === Backup Export & Import ===
    fun generateDefaultBackupFileName(): String = WorkoutBackupManager.generateBackupFileName()

    fun exportBackupToUri(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            _backupState.update { it.copy(isProcessing = true) }
            try {
                val json = withContext(Dispatchers.IO) {
                    repository.exportAllDataToJson()
                }
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                        stream.flush()
                    } ?: throw IllegalStateException("خطا در باز کردن مسیر ذخیره‌سازی")
                }
                _backupState.update {
                    it.copy(
                        isProcessing = false,
                        snackbarMessage = "فایل پشتیبان با موفقیت ذخیره شد"
                    )
                }
            } catch (e: Exception) {
                _backupState.update {
                    it.copy(
                        isProcessing = false,
                        snackbarMessage = "خطا در خروجی گرفتن: ${e.localizedMessage ?: "لطفاً دوباره تلاش کنید"}"
                    )
                }
            }
        }
    }

    fun prepareImportFromUri(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            _backupState.update { it.copy(isProcessing = true) }
            try {
                val json = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    } ?: throw IllegalStateException("فایل قابل خواندن نیست")
                }

                val validationResult = repository.validateBackupJson(json)
                validationResult.fold(
                    onSuccess = { validated ->
                        _backupState.update {
                            it.copy(
                                isProcessing = false,
                                pendingImportData = validated,
                                pendingImportSessionCount = validated.sessions.size
                            )
                        }
                    },
                    onFailure = { error ->
                        _backupState.update {
                            it.copy(
                                isProcessing = false,
                                snackbarMessage = "فایل پشتیبان نامعتبر است: ${error.localizedMessage ?: "فرمت فایل پشتیبانی نمی‌شود"}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _backupState.update {
                    it.copy(
                        isProcessing = false,
                        snackbarMessage = "خطا در خواندن فایل پشتیبان: ${e.localizedMessage ?: "فایل نامعتبر است"}"
                    )
                }
            }
        }
    }

    fun dismissImportConfirmDialog() {
        _backupState.update { it.copy(pendingImportData = null, pendingImportSessionCount = 0) }
    }

    fun confirmImport() {
        val backup = _backupState.value.pendingImportData ?: return
        viewModelScope.launch {
            _backupState.update {
                it.copy(
                    isProcessing = true,
                    pendingImportData = null,
                    pendingImportSessionCount = 0
                )
            }
            try {
                val importedSessions = withContext(Dispatchers.IO) {
                    repository.mergeBackupData(backup)
                }
                _backupState.update {
                    it.copy(
                        isProcessing = false,
                        snackbarMessage = "ورود اطلاعات با موفقیت انجام شد. ${JalaliDateUtil.toPersianDigits(importedSessions)} جلسه جدید اضافه شد."
                    )
                }
            } catch (e: Exception) {
                _backupState.update {
                    it.copy(
                        isProcessing = false,
                        snackbarMessage = "خطا در ورود اطلاعات: ${e.localizedMessage ?: "لطفاً دوباره تلاش کنید"}"
                    )
                }
            }
        }
    }

    fun clearBackupSnackbar() {
        _backupState.update { it.copy(snackbarMessage = null) }
    }
}
