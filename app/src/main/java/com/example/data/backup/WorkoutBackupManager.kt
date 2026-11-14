package com.example.data.backup

import com.example.data.model.Exercise
import com.example.data.model.ExerciseComment
import com.example.data.model.Session
import com.example.data.model.SetLog
import com.example.data.model.WorkoutDay
import com.example.util.JalaliDateUtil
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.Locale

data class WorkoutBackupDto(
    val schemaVersion: Int? = null,
    val exportedAt: Long? = null,
    val workoutDays: List<WorkoutDayDto>? = null,
    val exercises: List<ExerciseDto>? = null,
    val sessions: List<SessionDto>? = null,
    val setLogs: List<SetLogDto>? = null,
    val exerciseComments: List<ExerciseCommentDto>? = null
)

data class WorkoutDayDto(
    val id: Long? = null,
    val name: String? = null,
    val orderIndex: Int? = null
)

data class ExerciseDto(
    val id: Long? = null,
    val dayId: Long? = null,
    val name: String? = null,
    val orderIndex: Int? = null,
    val plannedSets: Int? = null,
    val plannedRepsPerSet: Int? = null
)

data class SessionDto(
    val id: Long? = null,
    val dayId: Long? = null,
    val dayName: String? = null,
    val startedAt: Long? = null,
    val pressureRating: Int? = null,
    val difficultyRating: Int? = null,
    val note: String? = null,
    val isCompleted: Boolean? = null,
    val currentExerciseIndex: Int? = null,
    val isResumeDismissed: Boolean? = null
)

data class SetLogDto(
    val id: Long? = null,
    val sessionId: Long? = null,
    val exerciseId: Long? = null,
    val exerciseName: String? = null,
    val setNumber: Int? = null,
    val weight: Double? = null,
    val reps: Int? = null,
    val completed: Boolean? = null
)

data class ExerciseCommentDto(
    val id: Long? = null,
    val sessionId: Long? = null,
    val exerciseId: Long? = null,
    val exerciseName: String? = null,
    val comment: String? = null
)

data class ValidatedBackupData(
    val schemaVersion: Int,
    val workoutDays: List<WorkoutDay>,
    val exercises: List<Exercise>,
    val sessions: List<Session>,
    val setLogs: List<SetLog>,
    val exerciseComments: List<ExerciseComment>
)

object WorkoutBackupManager {

    const val CURRENT_SCHEMA_VERSION = 1

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(WorkoutBackupDto::class.java).indent("  ")

    fun generateBackupFileName(epochMillis: Long = System.currentTimeMillis()): String {
        val jalali = JalaliDateUtil.fromEpochMillis(epochMillis)
        return String.format(
            Locale.US,
            "workout-backup-%04d-%02d-%02d.json",
            jalali.year,
            jalali.month,
            jalali.day
        )
    }

    fun serializeBackup(
        workoutDays: List<WorkoutDay>,
        exercises: List<Exercise>,
        sessions: List<Session>,
        setLogs: List<SetLog>,
        exerciseComments: List<ExerciseComment>,
        exportedAt: Long = System.currentTimeMillis()
    ): String {
        val dto = WorkoutBackupDto(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            exportedAt = exportedAt,
            workoutDays = workoutDays.map {
                WorkoutDayDto(id = it.id, name = it.name, orderIndex = it.orderIndex)
            },
            exercises = exercises.map {
                ExerciseDto(
                    id = it.id,
                    dayId = it.dayId,
                    name = it.name,
                    orderIndex = it.orderIndex,
                    plannedSets = it.plannedSets,
                    plannedRepsPerSet = it.plannedRepsPerSet
                )
            },
            sessions = sessions.map {
                SessionDto(
                    id = it.id,
                    dayId = it.dayId,
                    dayName = it.dayName,
                    startedAt = it.startedAt,
                    pressureRating = it.pressureRating,
                    difficultyRating = it.difficultyRating,
                    note = it.note,
                    isCompleted = it.isCompleted,
                    currentExerciseIndex = it.currentExerciseIndex,
                    isResumeDismissed = it.isResumeDismissed
                )
            },
            setLogs = setLogs.map {
                SetLogDto(
                    id = it.id,
                    sessionId = it.sessionId,
                    exerciseId = it.exerciseId,
                    exerciseName = it.exerciseName,
                    setNumber = it.setNumber,
                    weight = it.weight,
                    reps = it.reps,
                    completed = it.completed
                )
            },
            exerciseComments = exerciseComments.map {
                ExerciseCommentDto(
                    id = it.id,
                    sessionId = it.sessionId,
                    exerciseId = it.exerciseId,
                    exerciseName = it.exerciseName,
                    comment = it.comment
                )
            }
        )
        return adapter.toJson(dto)
    }

    fun parseAndValidateBackup(jsonString: String): Result<ValidatedBackupData> {
        return try {
            if (jsonString.isBlank()) {
                return Result.failure(IllegalArgumentException("فایل پشتیبان خالی است"))
            }
            val dto = moshi.adapter(WorkoutBackupDto::class.java).fromJson(jsonString)
                ?: return Result.failure(IllegalArgumentException("ساختار فایل JSON معتبر نیست"))

            val version = dto.schemaVersion
            if (version == null || version < 1 || version > CURRENT_SCHEMA_VERSION) {
                return Result.failure(IllegalArgumentException("نسخه فایل پشتیبان پشتیبانی نمی‌شود"))
            }

            val rawDays = dto.workoutDays
                ?: return Result.failure(IllegalArgumentException("فیلد روزهای تمرینی در فایل وجود ندارد"))
            val rawExercises = dto.exercises
                ?: return Result.failure(IllegalArgumentException("فیلد حرکات تمرینی در فایل وجود ندارد"))
            val rawSessions = dto.sessions
                ?: return Result.failure(IllegalArgumentException("فیلد جلسات در فایل وجود ندارد"))
            val rawSetLogs = dto.setLogs
                ?: return Result.failure(IllegalArgumentException("فیلد ست‌های ثبت‌شده در فایل وجود ندارد"))
            val rawComments = dto.exerciseComments ?: emptyList()

            val days = rawDays.map { d ->
                val id = d.id ?: return Result.failure(IllegalArgumentException("شناسه روز نامعتبر است"))
                val name = d.name ?: return Result.failure(IllegalArgumentException("نام روز نامعتبر است"))
                val orderIndex = d.orderIndex ?: return Result.failure(IllegalArgumentException("ترتیب روز نامعتبر است"))
                if (id <= 0L) return Result.failure(IllegalArgumentException("شناسه روز باید مثبت باشد"))
                WorkoutDay(id = id, name = name, orderIndex = orderIndex)
            }

            val exercises = rawExercises.map { e ->
                val id = e.id ?: return Result.failure(IllegalArgumentException("شناسه حرکت نامعتبر است"))
                val dayId = e.dayId ?: return Result.failure(IllegalArgumentException("شناسه روزِ حرکت نامعتبر است"))
                val name = e.name ?: return Result.failure(IllegalArgumentException("نام حرکت نامعتبر است"))
                val orderIndex = e.orderIndex ?: return Result.failure(IllegalArgumentException("ترتیب حرکت نامعتبر است"))
                val sets = e.plannedSets ?: return Result.failure(IllegalArgumentException("تعداد ست نامعتبر است"))
                val reps = e.plannedRepsPerSet ?: return Result.failure(IllegalArgumentException("تعداد تکرار نامعتبر است"))
                if (id <= 0L || dayId <= 0L || sets <= 0 || reps <= 0) {
                    return Result.failure(IllegalArgumentException("مقادیر عددی حرکت نامعتبر است"))
                }
                Exercise(
                    id = id,
                    dayId = dayId,
                    name = name,
                    orderIndex = orderIndex,
                    plannedSets = sets,
                    plannedRepsPerSet = reps
                )
            }

            val sessions = rawSessions.map { s ->
                val id = s.id ?: return Result.failure(IllegalArgumentException("شناسه جلسه نامعتبر است"))
                val startedAt = s.startedAt ?: return Result.failure(IllegalArgumentException("زمان شروع جلسه نامعتبر است"))
                if (id <= 0L || startedAt <= 0L) {
                    return Result.failure(IllegalArgumentException("اطلاعات جلسه نامعتبر است"))
                }
                Session(
                    id = id,
                    dayId = s.dayId ?: 0L,
                    dayName = s.dayName ?: "",
                    startedAt = startedAt,
                    pressureRating = (s.pressureRating ?: 5).coerceIn(1, 10),
                    difficultyRating = (s.difficultyRating ?: 5).coerceIn(1, 10),
                    note = s.note,
                    isCompleted = s.isCompleted ?: true,
                    currentExerciseIndex = (s.currentExerciseIndex ?: 0).coerceAtLeast(0),
                    isResumeDismissed = s.isResumeDismissed ?: false
                )
            }

            val setLogs = rawSetLogs.map { l ->
                val id = l.id ?: return Result.failure(IllegalArgumentException("شناسه ست نامعتبر است"))
                val sessionId = l.sessionId ?: return Result.failure(IllegalArgumentException("شناسه جلسه ست نامعتبر است"))
                val exerciseId = l.exerciseId ?: return Result.failure(IllegalArgumentException("شناسه حرکت ست نامعتبر است"))
                val setNumber = l.setNumber ?: return Result.failure(IllegalArgumentException("شماره ست نامعتبر است"))
                val weight = l.weight ?: return Result.failure(IllegalArgumentException("وزنه ست نامعتبر است"))
                val reps = l.reps ?: return Result.failure(IllegalArgumentException("تعداد تکرار ست نامعتبر است"))
                val completed = l.completed ?: return Result.failure(IllegalArgumentException("وضعیت انجام ست نامعتبر است"))
                if (id <= 0L || sessionId <= 0L || exerciseId <= 0L || setNumber <= 0 || weight < 0.0 || reps < 0) {
                    return Result.failure(IllegalArgumentException("مقادیر ست نامعتبر است"))
                }
                SetLog(
                    id = id,
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    exerciseName = l.exerciseName ?: "",
                    setNumber = setNumber,
                    weight = weight,
                    reps = reps,
                    completed = completed
                )
            }

            val comments = rawComments.map { c ->
                val id = c.id ?: return Result.failure(IllegalArgumentException("شناسه کامنت نامعتبر است"))
                val sessionId = c.sessionId ?: return Result.failure(IllegalArgumentException("شناسه جلسه کامنت نامعتبر است"))
                val exerciseId = c.exerciseId ?: return Result.failure(IllegalArgumentException("شناسه حرکت کامنت نامعتبر است"))
                val comment = c.comment ?: return Result.failure(IllegalArgumentException("متن کامنت نامعتبر است"))
                if (id <= 0L || sessionId <= 0L || exerciseId <= 0L) {
                    return Result.failure(IllegalArgumentException("مقادیر کامنت نامعتبر است"))
                }
                ExerciseComment(
                    id = id,
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    exerciseName = c.exerciseName ?: "",
                    comment = comment
                )
            }

            Result.success(
                ValidatedBackupData(
                    schemaVersion = version,
                    workoutDays = days,
                    exercises = exercises,
                    sessions = sessions,
                    setLogs = setLogs,
                    exerciseComments = comments
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
