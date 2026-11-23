package com.example.data.repository

import com.example.data.model.SetLog
import com.example.data.preferences.VersionComparator
import com.example.data.preferences.VersionPreferences
import com.example.ui.viewmodel.AnalysisViewModel
import com.example.ui.viewmodel.WorkoutViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun previousExerciseReference_matchesByNameSelectsMostRecentBeforeCurrentAndHandlesMissingSets() = runTest {
        val plan = listOf(
            DayWithExercisesData(
                name = "روز اول",
                exercises = listOf(
                    ExerciseData(name = "پرس سینه هالتر", plannedSets = 3, plannedRepsPerSet = 10)
                )
            )
        )
        repository.saveFullPlan(plan)
        val day = repository.allDaysWithExercises.first().first()
        val exId = day.exercises.first().id

        // Older session (s1): set 1 completed 60kg x 10, set 2 unticked, set 3 not logged
        val s1 = repository.startNewSession(day.day.id)
        val s1Entity = repository.getSessionByIdDirect(s1)!!.copy(startedAt = 1_700_000_000_000L)
        fakeDao.updateSession(s1Entity)
        repository.saveSetLog(
            SetLog(sessionId = s1, exerciseId = exId, exerciseName = "پرس سینه هالتر", setNumber = 1, weight = 60.0, reps = 10, completed = true)
        )
        repository.saveSetLog(
            SetLog(sessionId = s1, exerciseId = exId, exerciseName = "پرس سینه هالتر", setNumber = 2, weight = 65.0, reps = 8, completed = false)
        )

        // Even after re-saving the plan (new exerciseId!), name-based lookup still finds s1 for s2!
        repository.saveFullPlan(plan)
        val newDay = repository.allDaysWithExercises.first().first()
        val s2 = repository.startNewSession(newDay.day.id)
        val s2Entity = repository.getSessionByIdDirect(s2)!!.copy(startedAt = 1_700_600_000_000L)
        fakeDao.updateSession(s2Entity)

        val refForS2 = repository.getPreviousExerciseReference(
            currentSessionId = s2,
            currentStartedAt = s2Entity.startedAt,
            exerciseName = "پرس سینه هالتر",
            plannedSets = 3
        )
        assertNotNull(refForS2)
        assertEquals(s1, refForS2?.sessionId)
        assertEquals(3, refForS2?.sets?.size)
        assertTrue(refForS2!!.sets[0].isCompleted)
        assertEquals(60.0, refForS2.sets[0].weight ?: 0.0, 0.001)
        assertEquals(10, refForS2.sets[0].reps)
        // Set 2 was unticked -> not completed
        assertFalse(refForS2.sets[1].isCompleted)
        // Set 3 was missing -> not completed
        assertFalse(refForS2.sets[2].isCompleted)

        // When no previous record exists (e.g., for s1 itself or another exercise), returns null
        assertNull(
            repository.getPreviousExerciseReference(
                currentSessionId = s1,
                currentStartedAt = s1Entity.startedAt,
                exerciseName = "پرس سینه هالتر",
                plannedSets = 3
            )
        )
        assertNull(
            repository.getPreviousExerciseReference(
                currentSessionId = s2,
                currentStartedAt = s2Entity.startedAt,
                exerciseName = "اسکات پا",
                plannedSets = 3
            )
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun analysisViewModel_allowsOnlyOneExpandedExerciseAtATime() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val plan = listOf(
                DayWithExercisesData(
                    name = "روز اول",
                    exercises = listOf(
                        ExerciseData(name = "پرس سینه", plannedSets = 3, plannedRepsPerSet = 10),
                        ExerciseData(name = "قفسه سینه", plannedSets = 3, plannedRepsPerSet = 12),
                        ExerciseData(name = "پشت بازو", plannedSets = 3, plannedRepsPerSet = 12)
                    )
                )
            )
            repository.saveFullPlan(plan)
            val day = repository.allDaysWithExercises.first().first()
            val sessionId = repository.startNewSession(day.day.id)

            val analysisVm = AnalysisViewModel(repository)
            analysisVm.loadSessionExercises(sessionId)
            advanceUntilIdle()

            val ex1 = day.exercises[0].id
            val ex2 = day.exercises[1].id

            // Initially none expanded
            assertTrue(analysisVm.sessionExercisesState.value.exercises.none { it.isExpanded })

            // Tap exercise 1 -> only exercise 1 expanded
            analysisVm.toggleExerciseExpanded(ex1)
            val afterEx1 = analysisVm.sessionExercisesState.value
            assertEquals(ex1, afterEx1.expandedExerciseId)
            assertEquals(listOf(true, false, false), afterEx1.exercises.map { it.isExpanded })

            // Tap exercise 2 -> exercise 1 closes and only exercise 2 expands
            analysisVm.toggleExerciseExpanded(ex2)
            val afterEx2 = analysisVm.sessionExercisesState.value
            assertEquals(ex2, afterEx2.expandedExerciseId)
            assertEquals(listOf(false, true, false), afterEx2.exercises.map { it.isExpanded })

            // Tap exercise 2 again -> all closed
            analysisVm.toggleExerciseExpanded(ex2)
            val afterCollapse = analysisVm.sessionExercisesState.value
            assertNull(afterCollapse.expandedExerciseId)
            assertTrue(afterCollapse.exercises.none { it.isExpanded })
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeVersionPreferences(initialVersion: String? = null) : VersionPreferences {
        var storedVersion: String? = initialVersion
        override suspend fun getLastSeenVersion(): String? = storedVersion
        override suspend fun setLastSeenVersion(versionName: String) {
            storedVersion = versionName
        }
    }

    @Test
    fun versionComparator_handlesSemanticAndNumericVersions() {
        assertTrue(VersionComparator.isVersionNewer("2.5", "2.1"))
        assertTrue(VersionComparator.isVersionNewer("2.5", "2.0"))
        assertTrue(VersionComparator.isVersionNewer("2.10", "2.5"))
        assertTrue(VersionComparator.isVersionNewer("3.0", "2.5"))
        assertFalse(VersionComparator.isVersionNewer("2.5", "2.5"))
        assertFalse(VersionComparator.isVersionNewer("2.1", "2.5"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun whatsNewDialog_freshInstallDoesNotShowAndStoresCurrentVersion() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val prefs = FakeVersionPreferences(initialVersion = null)
            val vm = WorkoutViewModel(repository, prefs)

            vm.checkLaunchStatusIfNeeded("2.5")
            advanceUntilIdle()

            assertFalse(vm.showWhatsNewDialog.value)
            assertEquals("2.5", prefs.storedVersion)

            // Even after user creates a plan later and restarts app, still won't show for 2.5
            repository.saveFullPlan(
                listOf(
                    DayWithExercisesData(
                        name = "روز اول",
                        exercises = listOf(ExerciseData(name = "اسکات", plannedSets = 3, plannedRepsPerSet = 10))
                    )
                )
            )
            val vm2 = WorkoutViewModel(repository, prefs)
            vm2.checkLaunchStatusIfNeeded("2.5")
            advanceUntilIdle()
            assertFalse(vm2.showWhatsNewDialog.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun whatsNewDialog_showsOnceOnUpdateFromPrePreferencesOrOlderVersion() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            // Existing data from an older install that had no stored version yet
            repository.saveFullPlan(
                listOf(
                    DayWithExercisesData(
                        name = "روز اول",
                        exercises = listOf(ExerciseData(name = "پرس سینه", plannedSets = 3, plannedRepsPerSet = 10))
                    )
                )
            )
            val prefs = FakeVersionPreferences(initialVersion = null)
            val vm = WorkoutViewModel(repository, prefs)

            vm.checkLaunchStatusIfNeeded("2.5")
            advanceUntilIdle()
            assertTrue(vm.showWhatsNewDialog.value)
            assertNull(prefs.storedVersion)

            // User taps "متوجه شدم" -> closes dialog and stores "2.5"
            vm.dismissWhatsNewDialog("2.5")
            advanceUntilIdle()
            assertFalse(vm.showWhatsNewDialog.value)
            assertEquals("2.5", prefs.storedVersion)

            // Restarting app on 2.5 does NOT show dialog again
            val vmRestart = WorkoutViewModel(repository, prefs)
            vmRestart.checkLaunchStatusIfNeeded("2.5")
            advanceUntilIdle()
            assertFalse(vmRestart.showWhatsNewDialog.value)

            // Updating later from 2.5 to 2.6 shows dialog again
            val vmFutureUpdate = WorkoutViewModel(repository, prefs)
            vmFutureUpdate.checkLaunchStatusIfNeeded("2.6")
            advanceUntilIdle()
            assertTrue(vmFutureUpdate.showWhatsNewDialog.value)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
