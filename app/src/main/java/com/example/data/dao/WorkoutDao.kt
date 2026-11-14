package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.DayWithExercises
import com.example.data.model.Exercise
import com.example.data.model.ExerciseComment
import com.example.data.model.Session
import com.example.data.model.SessionWithDay
import com.example.data.model.SetLog
import com.example.data.model.WorkoutDay
import kotlinx.coroutines.flow.Flow

data class LoggedSetWithSession(
    val setLogId: Long,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val completed: Boolean,
    val sessionStartedAt: Long,
    val pressureRating: Int,
    val difficultyRating: Int,
    val note: String?
)

@Dao
interface WorkoutDao {

    // === Workout Day & Exercises ===
    @Transaction
    @Query("SELECT * FROM workout_days ORDER BY orderIndex ASC")
    fun getAllDaysWithExercises(): Flow<List<DayWithExercises>>

    @Transaction
    @Query("SELECT * FROM workout_days WHERE id = :dayId LIMIT 1")
    fun getDayWithExercises(dayId: Long): Flow<DayWithExercises?>

    @Query("SELECT * FROM workout_days ORDER BY orderIndex ASC")
    suspend fun getDaysDirect(): List<WorkoutDay>

    @Query("SELECT * FROM exercises WHERE dayId = :dayId ORDER BY orderIndex ASC")
    suspend fun getExercisesForDayDirect(dayId: Long): List<Exercise>

    @Query("SELECT * FROM exercises WHERE dayId = :dayId ORDER BY orderIndex ASC")
    fun getExercisesForDay(dayId: Long): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: WorkoutDay): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDays(days: List<WorkoutDay>): List<Long>

    @Query("DELETE FROM workout_days WHERE id = :dayId")
    suspend fun deleteDayById(dayId: Long)

    @Query("DELETE FROM exercises WHERE id = :exerciseId")
    suspend fun deleteExerciseById(exerciseId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<Exercise>): List<Long>

    @Query("DELETE FROM workout_days")
    suspend fun clearDays()

    @Query("DELETE FROM exercises")
    suspend fun clearExercises()

    @Transaction
    suspend fun clearPlanTransaction() {
        clearExercises()
        clearDays()
    }

    @Transaction
    suspend fun replacePlanTransaction(daysData: List<Pair<WorkoutDay, List<Exercise>>>) {
        clearExercises()
        clearDays()
        daysData.forEachIndexed { dayIdx, (day, exercises) ->
            val dayId = insertDay(day.copy(orderIndex = dayIdx + 1))
            val mappedExercises = exercises.mapIndexed { exIdx, ex ->
                ex.copy(dayId = dayId, orderIndex = exIdx + 1)
            }
            if (mappedExercises.isNotEmpty()) {
                insertExercises(mappedExercises)
            }
        }
    }

    // === Sessions ===
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session): Long

    @Update
    suspend fun updateSession(session: Session)

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionById(sessionId: Long): Flow<Session?>

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionByIdDirect(sessionId: Long): Session?

    @Query("SELECT * FROM sessions WHERE isCompleted = 0 AND isResumeDismissed = 0 ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLatestUnfinishedSessionDirect(): Session?

    @Query("UPDATE sessions SET currentExerciseIndex = :exerciseIndex WHERE id = :sessionId")
    suspend fun updateSessionExerciseIndex(sessionId: Long, exerciseIndex: Int)

    @Query("UPDATE sessions SET isResumeDismissed = 1 WHERE id = :sessionId")
    suspend fun dismissSessionResume(sessionId: Long)

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Transaction
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun getAllSessionsWithDay(): Flow<List<SessionWithDay>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionWithDay(sessionId: Long): Flow<SessionWithDay?>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionWithDayDirect(sessionId: Long): SessionWithDay?

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("DELETE FROM set_logs WHERE sessionId = :sessionId")
    suspend fun deleteSetLogsForSession(sessionId: Long)

    @Transaction
    suspend fun deleteSessionTransaction(sessionId: Long) {
        deleteSetLogsForSession(sessionId)
        deleteSessionById(sessionId)
    }

    // === Set Logs ===
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSetLog(setLog: SetLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetLogs(setLogs: List<SetLog>)

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId AND exerciseId = :exerciseId ORDER BY setNumber ASC")
    fun getSetLogsForSessionAndExercise(sessionId: Long, exerciseId: Long): Flow<List<SetLog>>

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId AND exerciseId = :exerciseId ORDER BY setNumber ASC")
    suspend fun getSetLogsForSessionAndExerciseDirect(sessionId: Long, exerciseId: Long): List<SetLog>

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY setNumber ASC")
    fun getSetLogsForSession(sessionId: Long): Flow<List<SetLog>>

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY setNumber ASC")
    suspend fun getSetLogsForSessionDirect(sessionId: Long): List<SetLog>

    // === Exercise Comments ===
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateExerciseComment(comment: ExerciseComment): Long

    @Query("SELECT * FROM exercise_comments WHERE sessionId = :sessionId AND exerciseId = :exerciseId LIMIT 1")
    suspend fun getExerciseCommentDirect(sessionId: Long, exerciseId: Long): ExerciseComment?

    @Query("SELECT * FROM exercise_comments WHERE sessionId = :sessionId")
    suspend fun getExerciseCommentsForSessionDirect(sessionId: Long): List<ExerciseComment>

    @Query("DELETE FROM exercise_comments WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun deleteExerciseComment(sessionId: Long, exerciseId: Long)

    // === Export & Import Helpers ===
    @Query("SELECT * FROM exercises ORDER BY dayId ASC, orderIndex ASC")
    suspend fun getAllExercisesDirect(): List<Exercise>

    @Query("SELECT * FROM sessions ORDER BY startedAt ASC")
    suspend fun getAllSessionsDirect(): List<Session>

    @Query("SELECT * FROM set_logs ORDER BY sessionId ASC, exerciseId ASC, setNumber ASC")
    suspend fun getAllSetLogsDirect(): List<SetLog>

    @Query("SELECT * FROM exercise_comments ORDER BY sessionId ASC, exerciseId ASC")
    suspend fun getAllExerciseCommentsDirect(): List<ExerciseComment>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDaysIgnore(days: List<WorkoutDay>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercisesIgnore(exercises: List<Exercise>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSessionsIgnore(sessions: List<Session>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSetLogsIgnore(setLogs: List<SetLog>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExerciseCommentsIgnore(comments: List<ExerciseComment>): List<Long>

    @Transaction
    suspend fun mergeImportTransaction(
        days: List<WorkoutDay>,
        exercises: List<Exercise>,
        sessions: List<Session>,
        setLogs: List<SetLog>,
        exerciseComments: List<ExerciseComment>
    ): Int {
        val existingDayIds = getDaysDirect().map { it.id }.toMutableSet()
        val daysToInsert = days.filter { it.id > 0L && it.id !in existingDayIds }
        if (daysToInsert.isNotEmpty()) {
            val inserted = insertDaysIgnore(daysToInsert)
            inserted.forEach { if (it != -1L) existingDayIds.add(it) }
        }

        val existingExerciseIds = getAllExercisesDirect().map { it.id }.toSet()
        val exercisesToInsert = exercises.filter {
            it.id > 0L && it.id !in existingExerciseIds && it.dayId in existingDayIds
        }
        if (exercisesToInsert.isNotEmpty()) {
            insertExercisesIgnore(exercisesToInsert)
        }

        val existingSessionIds = getAllSessionsDirect().map { it.id }.toMutableSet()
        val sessionsToInsert = sessions.filter { it.id > 0L && it.id !in existingSessionIds }
        var importedSessionCount = 0
        if (sessionsToInsert.isNotEmpty()) {
            val insertedIds = insertSessionsIgnore(sessionsToInsert)
            insertedIds.forEach { insertedId ->
                if (insertedId != -1L) {
                    existingSessionIds.add(insertedId)
                    importedSessionCount++
                }
            }
        }

        val existingLogs = getAllSetLogsDirect()
        val existingSetLogIds = existingLogs.map { it.id }.toSet()
        val existingSetLogKeys = existingLogs.map { Triple(it.sessionId, it.exerciseId, it.setNumber) }.toSet()
        val setLogsToInsert = setLogs.filter { log ->
            log.id > 0L &&
                log.id !in existingSetLogIds &&
                Triple(log.sessionId, log.exerciseId, log.setNumber) !in existingSetLogKeys &&
                log.sessionId in existingSessionIds
        }
        if (setLogsToInsert.isNotEmpty()) {
            insertSetLogsIgnore(setLogsToInsert)
        }

        val existingComments = getAllExerciseCommentsDirect()
        val existingCommentIds = existingComments.map { it.id }.toSet()
        val existingCommentKeys = existingComments.map { Pair(it.sessionId, it.exerciseId) }.toSet()
        val commentsToInsert = exerciseComments.filter { c ->
            c.id > 0L &&
                c.id !in existingCommentIds &&
                Pair(c.sessionId, c.exerciseId) !in existingCommentKeys &&
                c.sessionId in existingSessionIds
        }
        if (commentsToInsert.isNotEmpty()) {
            insertExerciseCommentsIgnore(commentsToInsert)
        }

        return importedSessionCount
    }

    // === Analysis Queries ===
    @Query("""
        SELECT 
            sl.id AS setLogId,
            sl.sessionId AS sessionId,
            sl.exerciseId AS exerciseId,
            ex.name AS exerciseName,
            sl.setNumber AS setNumber,
            sl.weight AS weight,
            sl.reps AS reps,
            sl.completed AS completed,
            s.startedAt AS sessionStartedAt,
            s.pressureRating AS pressureRating,
            s.difficultyRating AS difficultyRating,
            s.note AS note
        FROM set_logs sl
        INNER JOIN exercises ex ON sl.exerciseId = ex.id
        INNER JOIN sessions s ON sl.sessionId = s.id
        ORDER BY s.startedAt DESC, sl.setNumber ASC
    """)
    fun getAllLoggedSetsWithSession(): Flow<List<LoggedSetWithSession>>

    @Query("""
        SELECT 
            sl.id AS setLogId,
            sl.sessionId AS sessionId,
            sl.exerciseId AS exerciseId,
            ex.name AS exerciseName,
            sl.setNumber AS setNumber,
            sl.weight AS weight,
            sl.reps AS reps,
            sl.completed AS completed,
            s.startedAt AS sessionStartedAt,
            s.pressureRating AS pressureRating,
            s.difficultyRating AS difficultyRating,
            s.note AS note
        FROM set_logs sl
        INNER JOIN exercises ex ON sl.exerciseId = ex.id
        INNER JOIN sessions s ON sl.sessionId = s.id
        WHERE ex.name = :exerciseName
        ORDER BY s.startedAt DESC, sl.setNumber ASC
    """)
    fun getLoggedSetsForExerciseName(exerciseName: String): Flow<List<LoggedSetWithSession>>
}
