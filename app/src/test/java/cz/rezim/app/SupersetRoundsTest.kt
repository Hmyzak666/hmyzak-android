package cz.rezim.app

import org.junit.Assert.*
import org.junit.Test

class SupersetRoundsTest {
    private fun plan(start:Int=0,shortSets:Int=1)=listOf(
        Exercise("bench","Bench",4,UnitType.KG,supersetId="g"),
        Exercise("pushups","Push-ups",shortSets,UnitType.REPS,supersetId="g",supersetStartRound=start),
        Exercise("next","Next",1,UnitType.KG))
    private fun sets(plan:List<Exercise>)=plan.flatMapIndexed {i,e ->
        (1..e.sets).map {n ->SetEntry((i*100+n).toLong(),1,e.key,n,"20","10",false)}
    }
    private fun sequence(plan:List<Exercise>,ids:List<Long>,roundEnds:Set<Long>,initial:List<SetEntry> = sets(plan)) {
        var entries=initial
        ids.forEachIndexed {i,id ->
            entries=entries.map {if(it.id==id) it.copy(done=true) else it}
            val next=ids.getOrNull(i+1)?.let {key ->WorkoutScrollTarget.NextSet(entries.first {it.id==key}.exercise,key)}
                ?: WorkoutScrollTarget.SaveWorkout
            assertEquals("Next after $id",next,nextWorkoutTarget(plan,entries,id))
            assertEquals("Rest after $id",id in roundEnds,shouldStartRest(RestStartMode.AFTER_ROUND,plan,entries,id))
        }
    }
    @Test fun onePushupSetFollowsTheFinalBenchSet() {
        sequence(plan(),listOf(1L,2L,3L,4L,101L,201L),setOf(1L,2L,3L,101L))
    }
    @Test fun oneSetCanJoinAnySelectedRound() {
        for(round in 1..4) {
            val ids=(1L..4L).flatMap {if(it.toInt()==round) listOf(it,101L) else listOf(it)}+201L
            sequence(plan(round),ids,(1L..4L).filter {it.toInt()!=round}.toSet()+101L)
        }
    }
    @Test fun twoShortSetsFillTheFinalTwoRounds() {
        sequence(plan(shortSets=2),listOf(1L,2L,3L,101L,4L,102L,201L),setOf(1L,2L,101L,102L))
    }
    @Test fun twoShortSetsCanStartInTheSecondRound() {
        sequence(plan(2,2),listOf(1L,2L,101L,3L,102L,4L,201L),setOf(1L,101L,102L,4L))
    }
    @Test fun adductionCanFinishAFourExerciseSuperset() {
        val p=listOf(
            Exercise("curl","Curl",4,UnitType.KG,supersetId="legs"),
            Exercise("extension","Extension",4,UnitType.KG,supersetId="legs"),
            Exercise("abduction","Abduction",4,UnitType.REPS,supersetId="legs"),
            Exercise("adduction","Adduction",1,UnitType.REPS,supersetId="legs",supersetStartRound=0),
            Exercise("next","Next",1,UnitType.KG))
        sequence(p,listOf(1L,101L,201L,2L,102L,202L,3L,103L,203L,4L,104L,204L,301L,401L),setOf(201L,202L,203L,301L))
    }
    @Test fun lastRoundFollowsActualAddedWorkoutSets() {
        val p=plan();val entries=sets(p)+SetEntry(50,1,"bench",5,"20","10",false)
        sequence(p,listOf(1L,2L,3L,4L,50L,101L,201L),setOf(1L,2L,3L,4L,101L),entries)
    }
    @Test fun reducingRoundsKeepsEveryShortSetWithinTheGroup() {
        val p=plan(4,2)
        assertEquals(3,supersetRoundStarts(p.take(2))["pushups"])
        assertEquals("Kola 3–4",supersetRoundLabel(p[1],p.take(2)))
        val reduced=sets(p).filterNot {it.exercise=="bench" && it.number>2}
        assertEquals(1,supersetRoundStarts(p.take(2),reduced)["pushups"])
        assertEquals("Kola 1–2",supersetRoundLabel(p[1],p.take(2),reduced))
    }
    @Test fun changingRoundDuringWorkoutKeepsCompletedSetsAndUndoDoesNotTriggerRest() {
        val p=plan(3);val entries=sets(p).map {if(it.id==1L) it.copy(done=true) else it}
        val before=entries.toList()
        assertEquals(WorkoutScrollTarget.NextSet("bench",2),nextWorkoutTarget(p,entries,1))
        assertEquals(before,entries)
        assertFalse(shouldStartRest(RestStartMode.AFTER_ROUND,p,entries,101))
    }
    @Test fun independentExercisesIgnoreStoredRoundPreference() {
        val p=plan().map {it.copy(supersetId=null)}
        sequence(p,listOf(1L,2L,3L,4L,101L,201L),setOf(1L,2L,3L,4L,101L))
    }
    @Test fun finalShiftedSetOffersSaveWithoutAnotherRest() {
        val p=plan().take(2)
        sequence(p,listOf(1L,2L,3L,4L,101L),setOf(1L,2L,3L))
    }
}
