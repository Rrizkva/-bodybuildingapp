package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.WorkoutDao
import com.example.data.model.Exercise
import com.example.data.model.ExerciseComment
import com.example.data.model.Session
import com.example.data.model.SetLog
import com.example.data.model.WorkoutDay

/**
 * Local Room database for the workout tracker.
 *
 * IMPORTANT FOR DATA PRESERVATION ACROSS UPDATES:
 * - Users should NEVER uninstall the app before installing an update; uninstalling deletes all
 *   local SQLite database files on Android, whereas installing a newer APK over the existing app
 *   preserves all user data and runs the explicit Room migrations below.
 * - NEVER use `fallbackToDestructiveMigration()`, as it wipes all user data on schema mismatch.
 *
 * HOW TO ADD FUTURE MIGRATIONS:
 * 1. Update your Entity classes and increment `version` in `@Database(...)` (e.g., 4 -> 5).
 * 2. Keep `exportSchema = true` so Room writes the schema JSON to `app/schemas/`.
 * 3. Define a `val MIGRATION_4_5 = object : Migration(4, 5) { override fun migrate(db: SupportSQLiteDatabase) { ... } }`
 *    containing the exact `ALTER TABLE` / `CREATE TABLE` / `CREATE INDEX` SQL statements.
 * 4. Register the new migration in `.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)`.
 */
@Database(
    entities = [
        WorkoutDay::class,
        Exercise::class,
        Session::class,
        SetLog::class,
        ExerciseComment::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rebuild sessions table without the restrictive WorkoutDay foreign key and add dayName
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sessions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dayId` INTEGER NOT NULL,
                        `dayName` TEXT NOT NULL DEFAULT '',
                        `startedAt` INTEGER NOT NULL,
                        `pressureRating` INTEGER NOT NULL,
                        `difficultyRating` INTEGER NOT NULL,
                        `note` TEXT,
                        `isCompleted` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `sessions_new` (`id`, `dayId`, `dayName`, `startedAt`, `pressureRating`, `difficultyRating`, `note`, `isCompleted`)
                    SELECT `id`, `dayId`, '', `startedAt`, `pressureRating`, `difficultyRating`, `note`, `isCompleted` FROM `sessions`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `sessions`")
                db.execSQL("ALTER TABLE `sessions_new` RENAME TO `sessions`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_dayId` ON `sessions` (`dayId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_startedAt` ON `sessions` (`startedAt`)")

                // Add exerciseName snapshot column to set_logs
                db.execSQL("ALTER TABLE `set_logs` ADD COLUMN `exerciseName` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exercise_comments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` INTEGER NOT NULL,
                        `exerciseId` INTEGER NOT NULL,
                        `exerciseName` TEXT NOT NULL,
                        `comment` TEXT NOT NULL,
                        FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_exercise_comments_sessionId_exerciseId` ON `exercise_comments` (`sessionId`, `exerciseId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exercise_comments_sessionId` ON `exercise_comments` (`sessionId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exercise_comments_exerciseId` ON `exercise_comments` (`exerciseId`)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `currentExerciseIndex` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `isResumeDismissed` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_database.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
