package com.example.data.repository

import com.example.data.dao.LoggedSetWithSession
import com.example.data.dao.WorkoutDao
import com.example.data.model.DayWithExercises
import com.example.data.model.Exercise
import com.example.data.model.ExerciseComment
import com.example.data.model.Session
import com.example.data.model.SessionWithDay
import com.example.data.model.SetLog
import com.example.data.model.WorkoutDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeWorkoutDao : WorkoutDao {

    val days = mutableListOf<WorkoutDay>()
    val exercises = mutableListOf<Exercise>()
    val sessions = mutableListOf<Session>()
    val setLogs = mutableListOf<SetLog>()
    val exerciseComments = mutableListOf<ExerciseComment>()

    private val daysFlow = MutableStateFlow<List<WorkoutDay>>(emptyList())
    private val sessionsFlow = MutableStateFlow<List<Session>>(emptyList())
    private val setLogsFlow = MutableStateFlow<List<SetLog>>(emptyList())

    private fun notifyChanges() {
        daysFlow.value = days.toList()
        sessionsFlow.value = sessions.toList()
        setLogsFlow.value = setLogs.toList()
    }

    override fun getAllDaysWithExercises(): Flow<List<DayWithExercises>> {
        return daysFlow.map { dayList ->
            dayList.sortedBy { it.orderIndex }.map { day ->
                DayWithExercises(
                    day = day,
                    exercises = exercises.filter { it.dayId == day.id }.sortedBy { it.orderIndex }
                )
            }
        }
    }

    override fun getDayWithExercises(dayId: Long): Flow<DayWithExercises?> {
        return daysFlow.map { dayList ->
            val day = dayList.find { it.id == dayId } ?: return@map null
            DayWithExercises(
                day = day,
                exercises = exercises.filter { it.dayId == day.id }.sortedBy { it.orderIndex }
            )
        }
    }

    override suspend fun getDaysDirect(): List<WorkoutDay> {
        return days.sortedBy { it.orderIndex }
    }

    override suspend fun getExercisesForDayDirect(dayId: Long): List<Exercise> {
        return exercises.filter { it.dayId == dayId }.sortedBy { it.orderIndex }
    }

    override fun getExercisesForDay(dayId: Long): Flow<List<Exercise>> {
        return daysFlow.map {
            exercises.filter { it.dayId == dayId }.sortedBy { it.orderIndex }
        }
    }

    private var nextDayId = 1L
    override suspend fun insertDay(day: WorkoutDay): Long {
        val id = if (day.id == 0L) nextDayId++ else day.id
        val newDay = day.copy(id = id)
        days.removeAll { it.id == id }
        days.add(newDay)
        notifyChanges()
        return id
    }

    override suspend fun insertDays(days: List<WorkoutDay>): List<Long> {
        return days.map { insertDay(it) }
    }

    override suspend fun deleteDayById(dayId: Long) {
        days.removeAll { it.id == dayId }
        exercises.removeAll { it.dayId == dayId }
        notifyChanges()
    }

    override suspend fun deleteExerciseById(exerciseId: Long) {
        exercises.removeAll { it.id == exerciseId }
        notifyChanges()
    }

    private var nextExerciseId = 1L
    override suspend fun insertExercises(exercises: List<Exercise>): List<Long> {
        val ids = mutableListOf<Long>()
        exercises.forEach { ex ->
            val id = if (ex.id == 0L) nextExerciseId++ else ex.id
            this.exercises.removeAll { it.id == id }
            this.exercises.add(ex.copy(id = id))
            ids.add(id)
        }
        notifyChanges()
        return ids
    }

    override suspend fun clearDays() {
        days.clear()
        notifyChanges()
    }

    override suspend fun clearExercises() {
        exercises.clear()
        notifyChanges()
    }

    private var nextSessionId = 1L
    override suspend fun insertSession(session: Session): Long {
        val id = if (session.id == 0L) nextSessionId++ else session.id
        val newSession = session.copy(id = id)
        sessions.removeAll { it.id == id }
        sessions.add(newSession)
        notifyChanges()
        return id
    }

    override suspend fun updateSession(session: Session) {
        val idx = sessions.indexOfFirst { it.id == session.id }
        if (idx != -1) {
            sessions[idx] = session
            notifyChanges()
        }
    }

    override fun getSessionById(sessionId: Long): Flow<Session?> {
        return sessionsFlow.map { list -> list.find { it.id == sessionId } }
    }

    override suspend fun getSessionByIdDirect(sessionId: Long): Session? {
        return sessions.find { it.id == sessionId }
    }

    override suspend fun getLatestUnfinishedSessionDirect(): Session? {
        return sessions
            .filter { !it.isCompleted && !it.isResumeDismissed }
            .maxByOrNull { it.startedAt }
    }

    override suspend fun updateSessionExerciseIndex(sessionId: Long, exerciseIndex: Int) {
        val idx = sessions.indexOfFirst { it.id == sessionId }
        if (idx != -1) {
            sessions[idx] = sessions[idx].copy(currentExerciseIndex = exerciseIndex)
            notifyChanges()
        }
    }

    override suspend fun dismissSessionResume(sessionId: Long) {
        val idx = sessions.indexOfFirst { it.id == sessionId }
        if (idx != -1) {
            sessions[idx] = sessions[idx].copy(isResumeDismissed = true)
            notifyChanges()
        }
    }

    override fun getAllSessions(): Flow<List<Session>> {
        return sessionsFlow.map { list -> list.sortedByDescending { it.startedAt } }
    }

    override fun getAllSessionsWithDay(): Flow<List<SessionWithDay>> {
        return sessionsFlow.map { list ->
            list.sortedByDescending { it.startedAt }.map { s ->
                SessionWithDay(
                    session = s,
                    day = days.find { it.id == s.dayId }
                )
            }
        }
    }

    override fun getSessionWithDay(sessionId: Long): Flow<SessionWithDay?> {
        return sessionsFlow.map { list ->
            val s = list.find { it.id == sessionId } ?: return@map null
            SessionWithDay(session = s, day = days.find { it.id == s.dayId })
        }
    }

    override suspend fun getSessionWithDayDirect(sessionId: Long): SessionWithDay? {
        val s = sessions.find { it.id == sessionId } ?: return null
        return SessionWithDay(session = s, day = days.find { it.id == s.dayId })
    }

    override suspend fun deleteSessionById(sessionId: Long) {
        sessions.removeAll { it.id == sessionId }
        exerciseComments.removeAll { it.sessionId == sessionId }
        notifyChanges()
    }

    override suspend fun deleteSetLogsForSession(sessionId: Long) {
        setLogs.removeAll { it.sessionId == sessionId }
        notifyChanges()
    }

    private var nextSetLogId = 1L
    override suspend fun insertOrUpdateSetLog(setLog: SetLog): Long {
        val id = if (setLog.id == 0L) nextSetLogId++ else setLog.id
        val newLog = setLog.copy(id = id)
        setLogs.removeAll { it.sessionId == setLog.sessionId && it.exerciseId == setLog.exerciseId && it.setNumber == setLog.setNumber }
        setLogs.add(newLog)
        notifyChanges()
        return id
    }

    override suspend fun insertSetLogs(setLogs: List<SetLog>) {
        setLogs.forEach { insertOrUpdateSetLog(it) }
    }

    override fun getSetLogsForSessionAndExercise(sessionId: Long, exerciseId: Long): Flow<List<SetLog>> {
        return setLogsFlow.map { list ->
            list.filter { it.sessionId == sessionId && it.exerciseId == exerciseId }.sortedBy { it.setNumber }
        }
    }

    override suspend fun getSetLogsForSessionAndExerciseDirect(sessionId: Long, exerciseId: Long): List<SetLog> {
        return setLogs.filter { it.sessionId == sessionId && it.exerciseId == exerciseId }.sortedBy { it.setNumber }
    }

    override fun getSetLogsForSession(sessionId: Long): Flow<List<SetLog>> {
        return setLogsFlow.map { list ->
            list.filter { it.sessionId == sessionId }.sortedBy { it.setNumber }
        }
    }

    override suspend fun getSetLogsForSessionDirect(sessionId: Long): List<SetLog> {
        return setLogs.filter { it.sessionId == sessionId }.sortedBy { it.setNumber }
    }

    override suspend fun getPreviousSessionForExerciseDirect(
        currentSessionId: Long,
        currentStartedAt: Long,
        exerciseName: String
    ): Session? {
        val targetName = exerciseName.trim()
        val matchingSessionIds = setLogs.mapNotNull { sl ->
            val resolvedName = sl.exerciseName.ifBlank {
                exercises.find { it.id == sl.exerciseId }?.name.orEmpty()
            }.trim()
            if (resolvedName == targetName) sl.sessionId else null
        }.toSet()

        return sessions
            .filter { s ->
                s.id != currentSessionId &&
                    s.id in matchingSessionIds &&
                    (s.startedAt < currentStartedAt || (s.startedAt == currentStartedAt && s.id < currentSessionId))
            }
            .sortedWith(compareByDescending<Session> { it.startedAt }.thenByDescending { it.id })
            .firstOrNull()
    }

    override suspend fun getSetLogsForSessionAndExerciseNameDirect(
        sessionId: Long,
        exerciseName: String
    ): List<SetLog> {
        val targetName = exerciseName.trim()
        return setLogs.filter { sl ->
            val resolvedName = sl.exerciseName.ifBlank {
                exercises.find { it.id == sl.exerciseId }?.name.orEmpty()
            }.trim()
            sl.sessionId == sessionId && resolvedName == targetName
        }.sortedBy { it.setNumber }
    }

    private var nextCommentId = 1L
    override suspend fun insertOrUpdateExerciseComment(comment: ExerciseComment): Long {
        val existing = exerciseComments.find { it.sessionId == comment.sessionId && it.exerciseId == comment.exerciseId }
        val id = if (comment.id != 0L) comment.id else existing?.id ?: nextCommentId++
        val saved = comment.copy(id = id)
        exerciseComments.removeAll { it.sessionId == comment.sessionId && it.exerciseId == comment.exerciseId }
        exerciseComments.add(saved)
        return id
    }

    override suspend fun getExerciseCommentDirect(sessionId: Long, exerciseId: Long): ExerciseComment? {
        return exerciseComments.find { it.sessionId == sessionId && it.exerciseId == exerciseId }
    }

    override suspend fun getExerciseCommentsForSessionDirect(sessionId: Long): List<ExerciseComment> {
        return exerciseComments.filter { it.sessionId == sessionId }
    }

    override suspend fun deleteExerciseComment(sessionId: Long, exerciseId: Long) {
        exerciseComments.removeAll { it.sessionId == sessionId && it.exerciseId == exerciseId }
    }

    override suspend fun getAllExercisesDirect(): List<Exercise> {
        return exercises.sortedWith(compareBy({ it.dayId }, { it.orderIndex }))
    }

    override suspend fun getAllSessionsDirect(): List<Session> {
        return sessions.sortedBy { it.startedAt }
    }

    override suspend fun getAllSetLogsDirect(): List<SetLog> {
        return setLogs.sortedWith(compareBy({ it.sessionId }, { it.exerciseId }, { it.setNumber }))
    }

    override suspend fun getAllExerciseCommentsDirect(): List<ExerciseComment> {
        return exerciseComments.sortedWith(compareBy({ it.sessionId }, { it.exerciseId }))
    }

    override suspend fun insertDaysIgnore(days: List<WorkoutDay>): List<Long> {
        return days.map { d ->
            if (this.days.any { it.id == d.id }) -1L else insertDay(d)
        }
    }

    override suspend fun insertExercisesIgnore(exercises: List<Exercise>): List<Long> {
        val result = mutableListOf<Long>()
        exercises.forEach { ex ->
            if (this.exercises.any { it.id == ex.id }) {
                result.add(-1L)
            } else {
                val id = if (ex.id == 0L) nextExerciseId++ else ex.id
                this.exercises.add(ex.copy(id = id))
                result.add(id)
            }
        }
        notifyChanges()
        return result
    }

    override suspend fun insertSessionsIgnore(sessions: List<Session>): List<Long> {
        return sessions.map { s ->
            if (this.sessions.any { it.id == s.id }) -1L else insertSession(s)
        }
    }

    override suspend fun insertSetLogsIgnore(setLogs: List<SetLog>): List<Long> {
        val result = mutableListOf<Long>()
        setLogs.forEach { sl ->
            val duplicate = this.setLogs.any {
                it.id == sl.id || (it.sessionId == sl.sessionId && it.exerciseId == sl.exerciseId && it.setNumber == sl.setNumber)
            }
            if (duplicate) {
                result.add(-1L)
            } else {
                val id = if (sl.id == 0L) nextSetLogId++ else sl.id
                this.setLogs.add(sl.copy(id = id))
                result.add(id)
            }
        }
        notifyChanges()
        return result
    }

    override suspend fun insertExerciseCommentsIgnore(comments: List<ExerciseComment>): List<Long> {
        val result = mutableListOf<Long>()
        comments.forEach { c ->
            val duplicate = this.exerciseComments.any {
                it.id == c.id || (it.sessionId == c.sessionId && it.exerciseId == c.exerciseId)
            }
            if (duplicate) {
                result.add(-1L)
            } else {
                val id = if (c.id == 0L) nextCommentId++ else c.id
                this.exerciseComments.add(c.copy(id = id))
                result.add(id)
            }
        }
        return result
    }

    override fun getAllLoggedSetsWithSession(): Flow<List<LoggedSetWithSession>> {
        return setLogsFlow.map { list ->
            list.mapNotNull { sl ->
                val ex = exercises.find { it.id == sl.exerciseId } ?: return@mapNotNull null
                val s = sessions.find { it.id == sl.sessionId } ?: return@mapNotNull null
                LoggedSetWithSession(
                    setLogId = sl.id,
                    sessionId = sl.sessionId,
                    exerciseId = sl.exerciseId,
                    exerciseName = ex.name,
                    setNumber = sl.setNumber,
                    weight = sl.weight,
                    reps = sl.reps,
                    completed = sl.completed,
                    sessionStartedAt = s.startedAt,
                    pressureRating = s.pressureRating,
                    difficultyRating = s.difficultyRating,
                    note = s.note
                )
            }.sortedWith(compareByDescending<LoggedSetWithSession> { it.sessionStartedAt }.thenBy { it.setNumber })
        }
    }

    override fun getLoggedSetsForExerciseName(exerciseName: String): Flow<List<LoggedSetWithSession>> {
        return getAllLoggedSetsWithSession().map { list ->
            list.filter { it.exerciseName == exerciseName }
        }
    }
}
