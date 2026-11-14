package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "workout_days",
    indices = [Index(value = ["orderIndex"])]
)
data class WorkoutDay(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val orderIndex: Int
)

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDay::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["dayId"]), Index(value = ["name"])]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayId: Long,
    val name: String,
    val orderIndex: Int,
    val plannedSets: Int,
    val plannedRepsPerSet: Int
)

@Entity(
    tableName = "sessions",
    indices = [Index(value = ["dayId"]), Index(value = ["startedAt"])]
)
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayId: Long = 0,
    val dayName: String = "",
    val startedAt: Long,
    val pressureRating: Int = 5,
    val difficultyRating: Int = 5,
    val note: String? = null,
    val isCompleted: Boolean = false,
    val currentExerciseIndex: Int = 0,
    val isResumeDismissed: Boolean = false
)

@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(
            entity = Session::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId", "exerciseId", "setNumber"], unique = true),
        Index(value = ["exerciseId"]),
        Index(value = ["sessionId"])
    ]
)
data class SetLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String = "",
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val completed: Boolean
)

@Entity(
    tableName = "exercise_comments",
    foreignKeys = [
        ForeignKey(
            entity = Session::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId", "exerciseId"], unique = true),
        Index(value = ["sessionId"]),
        Index(value = ["exerciseId"])
    ]
)
data class ExerciseComment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String = "",
    val comment: String = ""
)

data class DayWithExercises(
    @Embedded val day: WorkoutDay,
    @Relation(
        parentColumn = "id",
        entityColumn = "dayId"
    )
    val exercises: List<Exercise>
)

data class SessionWithDay(
    @Embedded val session: Session,
    @Relation(
        parentColumn = "dayId",
        entityColumn = "id"
    )
    val day: WorkoutDay?
)
