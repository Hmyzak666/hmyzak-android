package cz.rezim.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutNavigationTest {
    private fun exercise(key: String, count: Int = 2, partner: String? = null) =
        Exercise(key, key, count, UnitType.KG, partner)

    private fun sets(plan: List<Exercise>, done: Set<Long> = emptySet()): List<SetEntry> =
        plan.flatMapIndexed { index, ex ->
            (1..ex.sets).map { number ->
                val id = (index * 100 + number).toLong()
                SetEntry(id, 1, ex.key, number, "20", "10", id in done)
            }
        }

    @Test fun ordinaryExerciseContinuesWithNextPendingSet() {
        val plan = listOf(exercise("a"), exercise("b"))
        assertEquals(WorkoutScrollTarget.NextSet("a", 2), nextWorkoutTarget(plan, sets(plan, setOf(1)), 1))
    }

    @Test fun finishedExerciseMovesToNextExercise() {
        val plan = listOf(exercise("a"), exercise("b"))
        assertEquals(WorkoutScrollTarget.NextSet("b", 101), nextWorkoutTarget(plan, sets(plan, setOf(1, 2)), 2))
    }

    @Test fun pairsAlternateAndThenContinueThroughTheNextPair() {
        val plan = listOf(exercise("a", partner="b"), exercise("b", partner="a"),
            exercise("c", partner="d"), exercise("d", partner="c"), exercise("e", 1))
        var entries = sets(plan)
        val sequence = listOf(1L, 101L, 2L, 102L, 201L, 301L, 202L, 302L, 401L)
        sequence.forEachIndexed { index, id ->
            entries = entries.map { if (it.id == id) it.copy(done=true) else it }
            val expected = sequence.getOrNull(index + 1)?.let { next ->
                WorkoutScrollTarget.NextSet(entries.first { it.id == next }.exercise, next)
            } ?: WorkoutScrollTarget.SaveWorkout
            assertEquals("After completing $id", expected, nextWorkoutTarget(plan, entries, id))
        }
    }

    @Test fun aShorterPartnerDoesNotLoseRemainingSets() {
        val plan = listOf(exercise("a", 4, "b"), exercise("b", 1, "a"), exercise("c", 1))
        assertEquals(WorkoutScrollTarget.NextSet("a", 2),
            nextWorkoutTarget(plan, sets(plan, setOf(1, 101)), 101))
        assertEquals(WorkoutScrollTarget.NextSet("a", 4),
            nextWorkoutTarget(plan, sets(plan, setOf(1, 2, 3, 101)), 3))
        assertEquals(WorkoutScrollTarget.NextSet("c", 201),
            nextWorkoutTarget(plan, sets(plan, setOf(1, 2, 3, 4, 101)), 4))
    }

    @Test fun aLongerPartnerContinuesAfterTheFirstExerciseIsDone() {
        val plan = listOf(exercise("a", 1, "b"), exercise("b", 3, "a"))
        assertEquals(WorkoutScrollTarget.NextSet("b", 102),
            nextWorkoutTarget(plan, sets(plan, setOf(1, 101)), 101))
    }

    @Test fun completedFollowingExercisesAreSkipped() {
        val plan = listOf(exercise("a", 1), exercise("b", 1), exercise("c", 1))
        assertEquals(WorkoutScrollTarget.NextSet("c", 201),
            nextWorkoutTarget(plan, sets(plan, setOf(1, 101)), 1))
    }

    @Test fun skippedEarlierWorkIsRevisitedBeforeSaving() {
        val plan = listOf(exercise("a"), exercise("b", 1))
        assertEquals(WorkoutScrollTarget.NextSet("a", 2),
            nextWorkoutTarget(plan, sets(plan, setOf(1, 101)), 101))
    }

    @Test fun alreadyCompletedPartnerSetsAreSkipped() {
        val plan = listOf(exercise("a", partner="b"), exercise("b", partner="a"))
        assertEquals(WorkoutScrollTarget.NextSet("b", 102),
            nextWorkoutTarget(plan, sets(plan, setOf(1, 101)), 1))
    }

    @Test fun aPairWorksWhenStartedFromTheSecondMember() {
        val plan = listOf(exercise("a", partner="b"), exercise("b", partner="a"))
        assertEquals(WorkoutScrollTarget.NextSet("a", 1),
            nextWorkoutTarget(plan, sets(plan, setOf(101)), 101))
    }

    @Test fun aNonAdjacentPairKeepsTheInterveningExercise() {
        val plan = listOf(exercise("a", 1, "c"), exercise("b", 1), exercise("c", 1, "a"), exercise("d", 1))
        assertEquals(WorkoutScrollTarget.NextSet("b", 101),
            nextWorkoutTarget(plan, sets(plan, setOf(1, 201)), 201))
    }

    @Test fun actualAddedWorkoutSetsAreIncluded() {
        val plan = listOf(exercise("a", 1), exercise("b", 1))
        val entries = sets(plan, setOf(1)) + SetEntry(3, 1, "a", 2, "20", "10", false)
        assertEquals(WorkoutScrollTarget.NextSet("a", 3), nextWorkoutTarget(plan, entries, 1))
    }

    @Test fun missingPartnerFallsBackToOrdinaryOrder() {
        val plan = listOf(exercise("a", 1, "removed"), exercise("b", 1))
        assertEquals(WorkoutScrollTarget.NextSet("b", 101),
            nextWorkoutTarget(plan, sets(plan, setOf(1)), 1))
    }

    @Test fun unfinishedOrUndoneSetsDoNotTriggerNavigation() {
        val plan = listOf(exercise("a"))
        assertNull(nextWorkoutTarget(plan, sets(plan), 1))
    }

    @Test fun missingSetsDoNotTriggerNavigation() {
        val plan = listOf(exercise("a"))
        assertNull(nextWorkoutTarget(plan, sets(plan), 999))
    }

    @Test fun allCompletedWorkLeadsToSaveWithoutSavingAnything() {
        val plan = listOf(exercise("a", 1))
        val entries = sets(plan, setOf(1))
        assertEquals(WorkoutScrollTarget.SaveWorkout, nextWorkoutTarget(plan, entries, 1))
        assertEquals(entries, sets(plan, setOf(1)))
    }
}
