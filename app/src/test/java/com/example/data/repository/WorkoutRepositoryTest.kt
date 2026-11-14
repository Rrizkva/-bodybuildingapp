package com.example.data.repository

import com.example.data.model.SetLog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorkoutRepositoryTest {

    private lateinit var fakeDao: FakeWorkoutDao
    private lateinit var repository: WorkoutRepository

    @Before
    fun setup() {
        fakeDao = FakeWorkoutDao()
        repository = WorkoutRepository(fakeDao)
    }

    @Test
    fun saveFullPlan_persistsDaysAndExercisesCorrectly() = runTest {
        val plan = listOf(
            DayWithExercisesData(
                name = "روز اول",
                exercises = listOf(
                    ExerciseData(name = "پرس سینه هالتر", plannedSets = 4, plannedRepsPerSet = 10),
                    ExerciseData(name = "بالا سینه دمبل", plannedSets = 3, plannedRepsPerSet = 12)
                )
            ),
            DayWithExercisesData(
                name = "روز دوم",
                exercises = listOf(
                    ExerciseData(name = "اسکات پا", plannedSets = 4, plannedRepsPerSet = 8)
                )
            )
        )

        repository.saveFullPlan(plan)

        val daysWithExercises = repository.allDaysWithExercises.first()
        assertEquals(2, daysWithExercises.size)
        assertEquals("روز اول", daysWithExercises[0].day.name)
        assertEquals(2, daysWithExercises[0].exercises.size)
        assertEquals("پرس سینه هالتر", daysWithExercises[0].exercises[0].name)
        assertEquals(4, daysWithExercises[0].exercises[0].plannedSets)

        assertEquals("روز دوم", daysWithExercises[1].day.name)
        assertEquals(1, daysWithExercises[1].exercises.size)
    }

    @Test
    fun startNewSession_createsSessionWithValidId() = runTest {
        val plan = listOf(
            DayWithExercisesData(
                name = "روز اول",
                exercises = listOf(ExerciseData(name = "پرس سینه", plannedSets = 3, plannedRepsPerSet = 10))
            )
        )
        repository.saveFullPlan(plan)
        val dayId = repository.allDaysWithExercises.first().first().day.id

        val sessionId = repository.startNewSession(dayId)
        assertTrue(sessionId > 0)

        val session = repository.getSessionByIdDirect(sessionId)
        assertNotNull(session)
        assertEquals(dayId, session?.dayId)
        assertTrue(session?.startedAt ?: 0L > 0L)
    }

    @Test
    fun setCompletionLogic_computesCompletedSetsMaxWeightAndVolumeAccurately() = runTest {
        // Setup day and exercise
        val plan = listOf(
            DayWithExercisesData(
                name = "روز اول",
                exercises = listOf(ExerciseData(name = "ددلیفت", plannedSets = 3, plannedRepsPerSet = 10))
            )
        )
        repository.saveFullPlan(plan)
        val dayWithEx = repository.allDaysWithExercises.first().first()
        val exerciseId = dayWithEx.exercises.first().id
        val sessionId = repository.startNewSession(dayWithEx.day.id)

        // Set 1: 60kg x 10 reps -> completed = true
        repository.saveSetLog(
            SetLog(
                sessionId = sessionId,
                exerciseId = exerciseId,
                setNumber = 1,
                weight = 60.0,
                reps = 10,
                completed = true
            )
        )

        // Set 2: 70kg x 8 reps -> completed = true
        repository.saveSetLog(
            SetLog(
                sessionId = sessionId,
                exerciseId = exerciseId,
                setNumber = 2,
                weight = 70.0,
                reps = 8,
                completed = true
            )
        )

        // Set 3: 75kg x 6 reps -> completed = false (unticked / unfinished)
        repository.saveSetLog(
            SetLog(
                sessionId = sessionId,
                exerciseId = exerciseId,
                setNumber = 3,
                weight = 75.0,
                reps = 6,
                completed = false
            )
        )

        val logs = repository.getSetLogsForSessionAndExerciseDirect(sessionId, exerciseId)
        assertEquals(3, logs.size)

        // Verify set completion filter logic: only completed sets are counted
        val completedSets = logs.filter { it.completed }
        assertEquals(2, completedSets.size)

        // Max weight must only consider completed sets: 70.0 (not 75.0)
        val maxWeight = completedSets.maxOfOrNull { it.weight } ?: 0.0
        assertEquals(70.0, maxWeight, 0.001)

        // Total volume = sum of (weight * reps) for completed sets
        // (60.0 * 10) + (70.0 * 8) = 600.0 + 560.0 = 1160.0
        val totalVolume = completedSets.sumOf { it.weight * it.reps }
        assertEquals(1160.0, totalVolume, 0.001)
    }

    @Test
    fun finishSession_updatesRatingsAndNotes() = runTest {
        val plan = listOf(
            DayWithExercisesData(
                name = "روز اول",
                exercises = listOf(ExerciseData(name = "اسکات", plannedSets = 3, plannedRepsPerSet = 10))
            )
        )
        repository.saveFullPlan(plan)
        val dayId = repository.allDaysWithExercises.first().first().day.id
        val sessionId = repository.startNewSession(dayId)

        repository.finishSession(
            sessionId = sessionId,
            pressureRating = 8,
            difficultyRating = 7,
            note = "تمرین سنگین و عالی بود"
        )

        val finishedSession = repository.getSessionByIdDirect(sessionId)
        assertNotNull(finishedSession)
        assertEquals(8, finishedSession?.pressureRating)
        assertEquals(7, finishedSession?.difficultyRating)
        assertEquals("تمرین سنگین و عالی بود", finishedSession?.note)
        assertTrue(finishedSession?.isCompleted == true)
    }

    @Test
    fun sessionAnalysisQueries_returnSessionsWithDayAndSetsCorrectly() = runTest {
        val plan = listOf(
            DayWithExercisesData(
                name = "روز دوم",
                exercises = listOf(
                    ExerciseData(name = "اسکات پا", plannedSets = 4, plannedRepsPerSet = 10)
                )
            )
        )
        repository.saveFullPlan(plan)
        val day = repository.allDaysWithExercises.first().first()
        val sessionId = repository.startNewSession(day.day.id)

        repository.saveSetLog(
            SetLog(
                sessionId = sessionId,
                exerciseId = day.exercises.first().id,
                setNumber = 1,
                weight = 80.0,
                reps = 10,
                completed = true
            )
        )

        val sessionsWithDay = repository.allSessionsWithDay.first()
        assertEquals(1, sessionsWithDay.size)
        assertEquals("روز دوم", sessionsWithDay.first().day?.name)

        val sessionWithDayDirect = repository.getSessionWithDayDirect(sessionId)
        assertNotNull(sessionWithDayDirect)
        assertEquals("روز دوم", sessionWithDayDirect?.day?.name)

        val setLogs = repository.getSetLogsForSessionDirect(sessionId)
        assertEquals(1, setLogs.size)
        assertEquals(80.0, setLogs.first().weight, 0.001)
        assertTrue(setLogs.first().completed)
    }

    @Test
    fun deletePlan_removesDaysAndExercisesPreservingSessions() = runTest {
        val plan = listOf(
            DayWithExercisesData(
                name = "روز اول",
                exercises = listOf(ExerciseData(name = "پرس سینه", plannedSets = 3, plannedRepsPerSet = 10))
            )
        )
        repository.saveFullPlan(plan)
        val day = repository.allDaysWithExercises.first().first()
        val sessionId = repository.startNewSession(day.day.id)

        repository.deletePlan()

        val remainingDays = repository.allDaysWithExercises.first()
        assertTrue(remainingDays.isEmpty())

        // Sessions still exist!
        val session = repository.getSessionByIdDirect(sessionId)
        assertNotNull(session)
        assertEquals("روز اول", session?.dayName)
    }

    @Test
    fun deleteSession_removesSessionAndItsLogs() = runTest {
        val plan = listOf(
            DayWithExercisesData(
                name = "روز اول",
                exercises = listOf(ExerciseData(name = "پرس سینه", plannedSets = 3, plannedRepsPerSet = 10))
            )
        )
        repository.saveFullPlan(plan)
        val day = repository.allDaysWithExercises.first().first()
        val sessionId = repository.startNewSession(day.day.id)
        repository.saveSetLog(
            SetLog(sessionId = sessionId, exerciseId = day.exercises.first().id, setNumber = 1, weight = 50.0, reps = 10, completed = true)
        )

        assertEquals(1, repository.allSessionsWithDay.first().size)
        assertEquals(1, repository.getSetLogsForSessionDirect(sessionId).size)

        repository.deleteSession(sessionId)

        assertEquals(0, repository.allSessionsWithDay.first().size)
        assertEquals(0, repository.getSetLogsForSessionDirect(sessionId).size)
    }

    @Test
    fun exerciseComment_isSavedPerSessionAndIsolatedAcrossSessions() = runTest {
        val plan = listOf(
            DayWithExercisesData(
                name = "روز اول",
                exercises = listOf(ExerciseData(name = "پرس سینه", plannedSets = 3, plannedRepsPerSet = 10))
            )
        )
        repository.saveFullPlan(plan)
        val day = repository.allDaysWithExercises.first().first()
        val exerciseId = day.exercises.first().id

        val session1 = repository.startNewSession(day.day.id)
        val session2 = repository.startNewSession(day.day.id)

        repository.saveExerciseComment(session1, exerciseId, "پرس سینه", "خط اول\nخط دوم عالی بود")
        repository.saveExerciseComment(session2, exerciseId, "پرس سینه", "جلسه دوم سنگین‌تر بود")

        val comment1 = repository.getExerciseCommentDirect(session1, exerciseId)
        val comment2 = repository.getExerciseCommentDirect(session2, exerciseId)

        assertEquals("خط اول\nخط دوم عالی بود", comment1?.comment)
        assertEquals("جلسه دوم سنگین‌تر بود", comment2?.comment)

        // Blank comment clears the saved comment
        repository.saveExerciseComment(session1, exerciseId, "پرس سینه", "   ")
        assertEquals(null, repository.getExerciseCommentDirect(session1, exerciseId))
        assertEquals("جلسه دوم سنگین‌تر بود", repository.getExerciseCommentDirect(session2, exerciseId)?.comment)
    }

    @Test
    fun backupExportAndMergeImport_skipsExistingAndInsertsNewWithoutWiping() = runTest {
        // Source database exports 2 sessions with setLogs and exercise comments
        val plan = listOf(
            DayWithExercisesData(
                name = "روز اول",
                exercises = listOf(ExerciseData(name = "اسکات", plannedSets = 3, plannedRepsPerSet = 10))
            )
        )
        repository.saveFullPlan(plan)
        val day = repository.allDaysWithExercises.first().first()
        val ex = day.exercises.first()

        val s1 = repository.startNewSession(day.day.id)
        repository.saveSetLog(SetLog(sessionId = s1, exerciseId = ex.id, exerciseName = ex.name, setNumber = 1, weight = 100.0, reps = 10, completed = true))
        repository.saveExerciseComment(s1, ex.id, ex.name, "خیلی خوب")
        repository.finishSession(s1, pressureRating = 8, difficultyRating = 7, note = "یادداشت ۱")

        val s2 = repository.startNewSession(day.day.id)
        repository.saveSetLog(SetLog(sessionId = s2, exerciseId = ex.id, exerciseName = ex.name, setNumber = 1, weight = 110.0, reps = 8, completed = true))
        repository.finishSession(s2, pressureRating = 9, difficultyRating = 9, note = "یادداشت ۲")

        val exportedJson = repository.exportAllDataToJson()
        assertTrue(exportedJson.contains("\"schemaVersion\": 1"))

        // Target database already has session s1 (id=1) with original note; session s2 (id=2) is missing
        val targetDao = FakeWorkoutDao()
        val targetRepo = WorkoutRepository(targetDao)
        targetRepo.saveFullPlan(plan)
        val targetDay = targetRepo.allDaysWithExercises.first().first()
        val targetEx = targetDay.exercises.first()
        val existingS1 = targetRepo.startNewSession(targetDay.day.id)
        targetRepo.finishSession(existingS1, pressureRating = 4, difficultyRating = 4, note = "نسخه فعلی")

        val validated = targetRepo.validateBackupJson(exportedJson).getOrThrow()
        val importedCount = targetRepo.mergeBackupData(validated)

        // Only session s2 (id=2) should be imported; s1 (id=1) was skipped and preserved!
        assertEquals(1, importedCount)
        assertEquals(2, targetDao.sessions.size)
        assertEquals("نسخه فعلی", targetRepo.getSessionByIdDirect(existingS1)?.note)
        assertEquals("یادداشت ۲", targetRepo.getSessionByIdDirect(s2)?.note)

        // Importing the exact same backup a second time imports 0 new sessions
        val secondImportedCount = targetRepo.mergeBackupData(validated)
        assertEquals(0, secondImportedCount)
        assertEquals(2, targetDao.sessions.size)
    }

    @Test
    fun backupValidation_rejectsMalformedOrUnsupportedJson() = runTest {
        assertTrue(repository.validateBackupJson("").isFailure)
        assertTrue(repository.validateBackupJson("{not-json}").isFailure)
        assertTrue(repository.validateBackupJson("""{"schemaVersion": 99}""").isFailure)
        assertTrue(repository.validateBackupJson("""{"schemaVersion": 1, "sessions": []}""").isFailure)
    }

    @Test
    fun unfinishedSession_tracksExerciseIndexAndDismissalPreservesIncompleteSession() = runTest {
        val plan = listOf(
            DayWithExercisesData(
                name = "روز اول",
                exercises = listOf(
                    ExerciseData(name = "پرس سینه", plannedSets = 3, plannedRepsPerSet = 10),
                    ExerciseData(name = "قفسه سینه", plannedSets = 3, plannedRepsPerSet = 12),
                    ExerciseData(name = "جلو بازو", plannedSets = 3, plannedRepsPerSet = 12)
                )
            )
        )
        repository.saveFullPlan(plan)
        val day = repository.allDaysWithExercises.first().first()
        val sessionId = repository.startNewSession(day.day.id)

        // Newly started session is resumable at exercise index 0
        val resumable1 = repository.getLatestResumableSessionDirect()
        assertNotNull(resumable1)
        assertEquals(sessionId, resumable1?.id)
        assertEquals(0, resumable1?.currentExerciseIndex)

        // Advance progress to exercise index 2 and save sets & comments
        repository.updateSessionExerciseIndex(sessionId, 2)
        repository.saveSetLog(
            SetLog(
                sessionId = sessionId,
                exerciseId = day.exercises[1].id,
                exerciseName = day.exercises[1].name,
                setNumber = 1,
                weight = 45.0,
                reps = 12,
                completed = true
            )
        )
        repository.saveExerciseComment(sessionId, day.exercises[1].id, day.exercises[1].name, "پمپ عالی")

        val resumable2 = repository.getLatestResumableSessionDirect()
        assertEquals(2, resumable2?.currentExerciseIndex)
        assertEquals(45.0, repository.getSetLogsForSessionAndExerciseDirect(sessionId, day.exercises[1].id).first().weight, 0.001)
        assertEquals("پمپ عالی", repository.getExerciseCommentDirect(sessionId, day.exercises[1].id)?.comment)

        // Choosing "شروع تازه" dismisses resume prompt while keeping incomplete session in Analysis
        repository.dismissSessionResume(sessionId)
        assertEquals(null, repository.getLatestResumableSessionDirect())
        val archivedSession = repository.getSessionByIdDirect(sessionId)
        assertNotNull(archivedSession)
        assertEquals(false, archivedSession?.isCompleted)
        assertTrue(repository.allSessionsWithDay.first().any { it.session.id == sessionId })
    }

    @Test
    fun finishSession_removesSessionFromResumableCheck() = runTest {
        val plan = listOf(
            DayWithExercisesData(
                name = "روز اول",
                exercises = listOf(ExerciseData(name = "اسکات", plannedSets = 3, plannedRepsPerSet = 10))
            )
        )
        repository.saveFullPlan(plan)
        val dayId = repository.allDaysWithExercises.first().first().day.id
        val sessionId = repository.startNewSession(dayId)

        assertNotNull(repository.getLatestResumableSessionDirect())

        repository.finishSession(
            sessionId = sessionId,
            pressureRating = 7,
            difficultyRating = 6,
            note = "تکمیل شد"
        )

        assertEquals(null, repository.getLatestResumableSessionDirect())
    }
}
