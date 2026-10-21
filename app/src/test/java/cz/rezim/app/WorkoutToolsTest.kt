package cz.rezim.app

import org.junit.Assert.*
import org.junit.Test

class WorkoutToolsTest {
    private fun plan()=listOf(
        Exercise("curl","Curl",4,UnitType.KG,supersetId="legs"),
        Exercise("extension","Extension",4,UnitType.KG,supersetId="legs"),
        Exercise("abduction","Abduction",4,UnitType.REPS,supersetId="legs"),
        Exercise("adduction","Adduction",1,UnitType.REPS,supersetId="legs"),
        Exercise("next","Next",1,UnitType.KG))
    private fun entries(plan:List<Exercise>)=plan.flatMapIndexed {i,e ->
        (1..e.sets).map {n -> SetEntry((i*100+n).toLong(),1,e.key,n,"20","12",false)}
    }
    @Test fun fourMemberSupersetWithSingleAdductionCompletesItInFirstRound() {
        val plan=plan();var entries=entries(plan)
        val sequence=listOf(1L,101L,201L,301L,2L,102L,202L,3L,103L,203L,4L,104L,204L,401L)
        sequence.forEachIndexed {i,id ->
            entries=entries.map {if(it.id==id) it.copy(done=true) else it}
            val next=sequence.getOrNull(i+1)?.let {key -> WorkoutScrollTarget.NextSet(entries.first {it.id==key}.exercise,key)}
                ?: WorkoutScrollTarget.SaveWorkout
            assertEquals("After $id",next,nextWorkoutTarget(plan,entries,id))
        }
    }
    @Test fun restAfterRoundSkipsTransitionsWithinGroupAndLastWorkoutSet() {
        val plan=plan();var entries=entries(plan)
        listOf(1L,101L,201L,301L,2L,102L,202L,3L,103L,203L,4L,104L,204L,401L).forEach {id ->
            entries=entries.map {if(it.id==id) it.copy(done=true) else it}
            assertEquals("After $id",id in setOf(301L,202L,203L,204L),
                shouldStartRest(RestStartMode.AFTER_ROUND,plan,entries,id))
            assertEquals(id!=401L,shouldStartRest(RestStartMode.AFTER_SET,plan,entries,id))
            assertFalse(shouldStartRest(RestStartMode.MANUAL,plan,entries,id))
        }
    }
    @Test fun undoAndValidationFailureDoNotStartRest() {
        val plan=plan();val entries=entries(plan)
        assertFalse(shouldStartRest(RestStartMode.AFTER_SET,plan,entries,1))
        assertFalse(shouldStartRest(RestStartMode.AFTER_SET,plan,entries,999))
    }
    @Test fun groupChangeMidWorkoutPreservesCompletedSetsAndUsesNewRoundOrder() {
        val old=plan();val entries=entries(old).map {if(it.id in setOf(1L,101L)) it.copy(done=true) else it}
        val changed=old.map {if(it.key=="adduction") it.copy(supersetId=null) else it}
        assertEquals(WorkoutScrollTarget.NextSet("abduction",201),nextWorkoutTarget(changed,entries,101))
        assertEquals(2,entries.count {it.done})
    }
    @Test fun nonAdjacentGroupsDoNotSkipInterveningExercise() {
        val plan=listOf(Exercise("a","A",1,UnitType.KG,supersetId="g"),Exercise("b","B",1,UnitType.KG),
            Exercise("c","C",1,UnitType.KG,supersetId="g"),Exercise("d","D",1,UnitType.KG,supersetId="g"))
        val entries=entries(plan).map {if(it.exercise!="b") it.copy(done=true) else it}
        assertEquals(WorkoutScrollTarget.NextSet("b",101),nextWorkoutTarget(plan,entries,301))
    }
    @Test fun countdownPausesResumesAndChangesLengthWithoutWallClock() {
        val timer=RestCountdown.start(1,90,1000,42)
        assertEquals(70000L,timer.remainingMillis(21000))
        val paused=timer.pause(21000)
        assertEquals(70000L,paused.remainingMillis(1000000))
        val longer=paused.adjust(30,1000000).resume(1000000)
        assertEquals(100000L,longer.remainingMillis(1000000))
        assertEquals(70000L,longer.remainingMillis(1030000))
        assertEquals(42L,longer.sourceSetId)
    }
    @Test fun countdownBoundsAndZeroHandling() {
        assertThrows(IllegalArgumentException::class.java) {RestCountdown.start(0,90,1000)}
        assertThrows(IllegalArgumentException::class.java) {RestCountdown.start(1,0,1000)}
        assertThrows(IllegalArgumentException::class.java) {RestCountdown.start(1,3601,1000)}
        val timer=RestCountdown.start(1,10,1000)
        assertEquals(3600000L,timer.adjust(Int.MAX_VALUE,1000).remainingMillis(1000))
        assertEquals(0L,timer.adjust(Int.MIN_VALUE,1000).remainingMillis(1000))
        assertEquals(RestPhase.IDLE,timer.pause(1000).adjust(-30,1000).phase)
        assertEquals(RestPhase.RUNNING,timer.pause(11000).phase)
        assertEquals(RestPhase.FINISHED,timer.finished().phase)
        assertEquals(0L,timer.finished().remainingMillis(0))
    }
    @Test fun restDurationInputAcceptsSecondsOrMinutesAndRejectsOverflow() {
        for(raw in listOf("90","1:30"," 1:30 ")) assertEquals(90,parseRestSeconds(raw))
        assertEquals(3600,parseRestSeconds("60:00"))
        for(raw in listOf("","0","-1","1:60","1:2:3","3601","9999999999999999999","NaN")) assertNull(raw,parseRestSeconds(raw))
    }
    @Test fun liveDurationAdjustmentKeepsRunningButFinishedOverrideIsFixed() {
        val live=Session(1,"mon",1000,null,elapsedOffsetSeconds=300)
        assertEquals(360,live.elapsedSeconds(61000))
        assertEquals(370,live.elapsedSeconds(71000))
        val finished=live.copy(finished=61000)
        assertEquals(360,finished.elapsedSeconds(999999))
        assertEquals(180,finished.copy(durationSeconds=180).elapsedSeconds())
        assertEquals(0,live.copy(elapsedOffsetSeconds=-1000).elapsedSeconds(0))
        assertEquals("1:02:03",workoutClock(3723))
    }
}
