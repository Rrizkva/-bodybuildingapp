package com.example.data.repository

import com.example.data.backup.ValidatedBackupData
import com.example.data.backup.WorkoutBackupManager
import com.example.data.dao.LoggedSetWithSession
import com.example.data.dao.WorkoutDao
import com.example.data.model.DayWithExercises
import com.example.data.model.Exercise
import com.example.data.model.ExerciseComment
import com.example.data.model.Session
import com.example.data.model.SessionWithDay
import com.example.data.model.SetLog
import com.example.data.model.WorkoutDay
import com.example.util.JalaliDate
import com.example.util.JalaliDateUtil
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val workoutDao: WorkoutDao) {

    val allDaysWithExercises: Flow<List<DayWithExercises>> =
        workoutDao.getAllDaysWithExercises()

    val allSessions: Flow<List<Session>> =
        workoutDao.getAllSessions()

    val allSessionsWithDay: Flow<List<SessionWithDay>> =
        workoutDao.getAllSessionsWithDay()

    val allLoggedSetsWithSession: Flow<List<LoggedSetWithSession>> =
        workoutDao.getAllLoggedSetsWithSession()

    fun getDayWithExercises(dayId: Long): Flow<DayWithExercises?> =
        workoutDao.getDayWithExercises(dayId)

    fun getExercisesForDay(dayId: Long): Flow<List<Exercise>> =
        workoutDao.getExercisesForDay(dayId)

    suspend fun getExercisesForDayDirect(dayId: Long): List<Exercise> =
        workoutDao.getExercisesForDayDirect(dayId)

    fun getSessionById(sessionId: Long): Flow<Session?> =
        workoutDao.getSessionById(sessionId)

    suspend fun getSessionByIdDirect(sessionId: Long): Session? =
        workoutDao.getSessionByIdDirect(sessionId)

    fun getSessionWithDay(sessionId: Long): Flow<SessionWithDay?> =
        workoutDao.getSessionWithDay(sessionId)

    suspend fun getSessionWithDayDirect(sessionId: Long): SessionWithDay? =
        workoutDao.getSessionWithDayDirect(sessionId)

    fun getSetLogsForSessionAndExercise(sessionId: Long, exerciseId: Long): Flow<List<SetLog>> =
        workoutDao.getSetLogsForSessionAndExercise(sessionId, exerciseId)

    suspend fun getSetLogsForSessionAndExerciseDirect(sessionId: Long, exerciseId: Long): List<SetLog> =
        workoutDao.getSetLogsForSessionAndExerciseDirect(sessionId, exerciseId)

    fun getSetLogsForSession(sessionId: Long): Flow<List<SetLog>> =
        workoutDao.getSetLogsForSession(sessionId)

    suspend fun getSetLogsForSessionDirect(sessionId: Long): List<SetLog> =
        workoutDao.getSetLogsForSessionDirect(sessionId)

    fun getLoggedSetsForExerciseName(exerciseName: String): Flow<List<LoggedSetWithSession>> =
        workoutDao.getLoggedSetsForExerciseName(exerciseName)

    suspend fun getPreviousExerciseReference(
        currentSessionId: Long,
        currentStartedAt: Long,
        exerciseName: String,
        plannedSets: Int
    ): PreviousExerciseReference? {
        val trimmedName = exerciseName.trim()
        if (trimmedName.isEmpty()) return null

        val previousSession = workoutDao.getPreviousSessionForExerciseDirect(
            currentSessionId = currentSessionId,
            currentStartedAt = currentStartedAt,
            exerciseName = trimmedName
        ) ?: return null

        val logs = workoutDao.getSetLogsForSessionAndExerciseNameDirect(
            sessionId = previousSession.id,
            exerciseName = trimmedName
        )
        val logsBySetNumber = logs.associateBy { it.setNumber }
        val maxSetNumber = maxOf(plannedSets, logs.maxOfOrNull { it.setNumber } ?: 0)
        val setItems = (1..maxSetNumber).map { setNumber ->
            val log = logsBySetNumber[setNumber]
            if (log != null && log.completed) {
                PreviousSetReferenceItem(
                    setNumber = setNumber,
                    weight = log.weight,
                    reps = log.reps,
                    isCompleted = true
                )
            } else {
                PreviousSetReferenceItem(
                    setNumber = setNumber,
                    weight = null,
                    reps = null,
                    isCompleted = false
                )
            }
        }

        return PreviousExerciseReference(
            sessionId = previousSession.id,
            jalaliDate = JalaliDateUtil.fromEpochMillis(previousSession.startedAt),
            sets = setItems
        )
    }

    suspend fun saveFullPlan(planData: List<DayWithExercisesData>) {
        val daysData = planData.mapIndexed { dayIndex, dayItem ->
            val day = WorkoutDay(
                name = dayItem.name,
                orderIndex = dayIndex + 1
            )
            val exercises = dayItem.exercises.mapIndexed { exIndex, exItem ->
                Exercise(
                    dayId = 0, // will be assigned in transaction
                    name = exItem.name.trim(),
                    orderIndex = exIndex + 1,
                    plannedSets = exItem.plannedSets.coerceAtLeast(1),
                    plannedRepsPerSet = exItem.plannedRepsPerSet.coerceAtLeast(1)
                )
            }
            Pair(day, exercises)
        }
        workoutDao.replacePlanTransaction(daysData)
    }

    suspend fun deletePlan() {
        workoutDao.clearPlanTransaction()
    }

    suspend fun startNewSession(dayId: Long): Long {
        val day = workoutDao.getDaysDirect().find { it.id == dayId }
        val dayName = day?.name ?: "روز اول"
        val session = Session(
            dayId = dayId,
            dayName = dayName,
            startedAt = System.currentTimeMillis(),
            pressureRating = 5,
            difficultyRating = 5,
            note = null,
            isCompleted = false,
            currentExerciseIndex = 0,
            isResumeDismissed = false
        )
        return workoutDao.insertSession(session)
    }

    suspend fun getLatestResumableSessionDirect(): Session? {
        val candidate = workoutDao.getLatestUnfinishedSessionDirect() ?: return null
        val exercises = workoutDao.getExercisesForDayDirect(candidate.dayId)
        if (exercises.isEmpty()) {
            workoutDao.dismissSessionResume(candidate.id)
            return null
        }
        return candidate
    }

    suspend fun updateSessionExerciseIndex(sessionId: Long, exerciseIndex: Int) {
        workoutDao.updateSessionExerciseIndex(sessionId, exerciseIndex.coerceAtLeast(0))
    }

    suspend fun dismissSessionResume(sessionId: Long) {
        workoutDao.dismissSessionResume(sessionId)
    }

    suspend fun hasAnyExistingData(): Boolean {
        return workoutDao.getDaysDirect().isNotEmpty() || workoutDao.getAllSessionsDirect().isNotEmpty()
    }

    suspend fun saveSetLog(setLog: SetLog): Long {
        return workoutDao.insertOrUpdateSetLog(setLog)
    }

    suspend fun saveSetLogs(setLogs: List<SetLog>) {
        workoutDao.insertSetLogs(setLogs)
    }

    suspend fun finishSession(
        sessionId: Long,
        pressureRating: Int,
        difficultyRating: Int,
        note: String?
    ) {
        val existing = workoutDao.getSessionByIdDirect(sessionId)
        if (existing != null) {
            workoutDao.updateSession(
                existing.copy(
                    pressureRating = pressureRating.coerceIn(1, 10),
                    difficultyRating = difficultyRating.coerceIn(1, 10),
                    note = note?.trim(),
                    isCompleted = true,
                    isResumeDismissed = true
                )
            )
        }
    }

    suspend fun deleteDay(dayId: Long) {
        workoutDao.deleteDayById(dayId)
    }

    suspend fun deleteSession(sessionId: Long) {
        workoutDao.deleteSessionTransaction(sessionId)
    }

    // === Exercise Comments (per session + exercise) ===
    suspend fun getExerciseCommentDirect(sessionId: Long, exerciseId: Long): ExerciseComment? =
        workoutDao.getExerciseCommentDirect(sessionId, exerciseId)

    suspend fun getExerciseCommentsForSessionDirect(sessionId: Long): List<ExerciseComment> =
        workoutDao.getExerciseCommentsForSessionDirect(sessionId)

    suspend fun saveExerciseComment(
        sessionId: Long,
        exerciseId: Long,
        exerciseName: String,
        commentText: String
    ) {
        val trimmed = commentText.trimEnd()
        if (trimmed.isBlank()) {
            workoutDao.deleteExerciseComment(sessionId, exerciseId)
        } else {
            val existing = workoutDao.getExerciseCommentDirect(sessionId, exerciseId)
            workoutDao.insertOrUpdateExerciseComment(
                ExerciseComment(
                    id = existing?.id ?: 0L,
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    comment = trimmed
                )
            )
        }
    }

    // === Backup Export & Merge Import ===
    suspend fun exportAllDataToJson(): String {
        val days = workoutDao.getDaysDirect()
        val exercises = workoutDao.getAllExercisesDirect()
        val sessions = workoutDao.getAllSessionsDirect()
        val setLogs = workoutDao.getAllSetLogsDirect()
        val comments = workoutDao.getAllExerciseCommentsDirect()
        return WorkoutBackupManager.serializeBackup(
            workoutDays = days,
            exercises = exercises,
            sessions = sessions,
            setLogs = setLogs,
            exerciseComments = comments
        )
    }

    fun validateBackupJson(jsonString: String): Result<ValidatedBackupData> {
        return WorkoutBackupManager.parseAndValidateBackup(jsonString)
    }

    suspend fun mergeBackupData(backup: ValidatedBackupData): Int {
        return workoutDao.mergeImportTransaction(
            days = backup.workoutDays,
            exercises = backup.exercises,
            sessions = backup.sessions,
            setLogs = backup.setLogs,
            exerciseComments = backup.exerciseComments
        )
    }
}

/**
 * Data structures used during plan editing in memory
 */
data class DayWithExercisesData(
    val id: Long = 0,
    val name: String,
    val exercises: List<ExerciseData>
)

data class ExerciseData(
    val id: Long = 0,
    val name: String,
    val plannedSets: Int = 3,
    val plannedRepsPerSet: Int = 10
)

data class PreviousSetReferenceItem(
    val setNumber: Int,
    val weight: Double?,
    val reps: Int?,
    val isCompleted: Boolean
)

data class PreviousExerciseReference(
    val sessionId: Long,
    val jalaliDate: JalaliDate,
    val sets: List<PreviousSetReferenceItem>
)
