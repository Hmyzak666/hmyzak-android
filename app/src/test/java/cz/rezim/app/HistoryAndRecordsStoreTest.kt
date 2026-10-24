package cz.rezim.app

import android.app.Application
import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="en-rUS")
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class HistoryAndRecordsStoreTest {
    private val app:Application get()=RuntimeEnvironment.getApplication()
    @Before fun freshDatabase() {
        app.deleteDatabase("rezim_local_v1.db")
        app.getSharedPreferences("hmyzak_prefs",Context.MODE_PRIVATE).edit().clear().commit()
    }
    private fun plan(store:LocalStore,type:UnitType=UnitType.KG):String {
        store.completeSetup();val day=store.addDay("Own strength day")
        store.addExercise(day,"Own press",2,type);return day
    }
    private fun finish(store:LocalStore,day:String,kg:String,reps:String,finished:Long=System.currentTimeMillis()):Long {
        val session=store.beginWorkout(day)
        store.updateSet(store.sets(session.id).first().copy(weight=kg,reps=reps,done=true))
        store.finishWorkout(session.id)
        store.writableDatabase.execSQL("UPDATE sessions SET created=?,finished=?,duration_seconds=125 WHERE id=?",arrayOf(finished-125000,finished,session.id))
        return session.id
    }
    @Test fun recentThreeAndUnboundedHistoryUseCompletionOrderAndIgnoreOpenWorkout() {
        LocalStore(app).use {store ->
            val day=plan(store);val ids=(1..20).map {finish(store,day,"20","8",it*1000000L)}
            // Completion time, not creation/id, determines position; ties use id.
            store.writableDatabase.execSQL("UPDATE sessions SET finished=25000000 WHERE id=?",arrayOf(ids.first()))
            store.writableDatabase.execSQL("UPDATE sessions SET finished=25000000 WHERE id=?",arrayOf(ids[1]))
            store.beginWorkout(day)
            val before=store.exportJson();val vm=RezimState(app);vm.setProForTest(false)
            assertEquals(listOf(ids[1],ids[0],ids.last()),vm.recentWorkouts.map{it.id})
            assertEquals(20,vm.completedWorkouts.size);assertEquals(20,vm.totalWorkouts)
            assertTrue(vm.completedWorkouts.all{it.finished!=null})
            assertEquals(before,store.exportJson())
        }
    }
    @Test fun viewingOldWorkoutAndReturningToFullHistoryNeverWritesData() {
        LocalStore(app).use {store ->
            val day=plan(store);val ids=(1..20).map {finish(store,day,"12,5","9",it*1000000L)}
            val before=store.exportJson();val vm=RezimState(app);vm.setProForTest(false)
            vm.openWorkoutHistory();vm.openWorkout(ids.first())
            assertEquals(Page.WORKOUT_DETAIL,vm.page);assertNull(vm.editingSavedWorkout)
            assertEquals(store.sets(ids.first()),vm.savedWorkoutSets(ids.first()))
            assertEquals(125,vm.completedWorkouts.last().elapsedSeconds())
            vm.leaveWorkoutHistory();assertEquals(Page.HISTORY,vm.page)
            vm.leaveWorkoutHistory();assertEquals(Page.HOME,vm.page)
            vm.openWorkout(Long.MAX_VALUE);assertEquals(Page.HOME,vm.page)
            assertEquals(before,store.exportJson())
        }
    }
    @Test fun deletedPlanDoesNotHideFinishedHistoryAndCancelledEditsRemainInMemoryOnly() {
        LocalStore(app).use {store ->
            val day=plan(store);val id=finish(store,day,"20","8")
            store.removeDay(day)
            val before=store.exportJson();val vm=RezimState(app);vm.setProForTest(false)
            assertTrue(vm.days.isEmpty());vm.openWorkout(id)
            vm.beginEditSavedWorkout(vm.completedWorkouts.single())
            vm.setSavedEntry(vm.editingSavedSets.first(),weight="99",reps="11")
            assertEquals(before,store.exportJson())
            vm.cancelSavedEdit();assertEquals(Page.WORKOUT_DETAIL,vm.page)
            assertEquals(before,store.exportJson());assertEquals(2,vm.savedWorkoutSets(id).size)
        }
    }
    @Test fun invalidSetOrDurationRollsBackAllCorrectionsAndValidEditCommitsTogether() {
        LocalStore(app).use {store ->
            val day=plan(store);val id=finish(store,day,"20","8");val before=store.exportJson()
            val rows=store.sets(id)
            assertThrows(IllegalArgumentException::class.java) {store.saveFinishedWorkout(id,rows.map{it.copy(reps="0",done=true)},600)}
            assertEquals(before,store.exportJson())
            assertThrows(IllegalArgumentException::class.java) {store.saveFinishedWorkout(id,rows.map{it.copy(weight="30")},0)}
            assertEquals(before,store.exportJson())
            assertThrows(IllegalArgumentException::class.java) {store.saveFinishedWorkout(id,listOf(rows.first(),rows.first()),600)}
            assertEquals(before,store.exportJson())
            val vm=RezimState(app);vm.setProForTest(false);vm.openWorkout(id);vm.beginEditSavedWorkout(vm.completedWorkouts.single())
            vm.setSavedEntry(vm.editingSavedSets.first(),weight="25",reps="10");vm.commitSavedEdit(12)
            assertNull(vm.editingSavedWorkout);assertEquals(Page.WORKOUT_DETAIL,vm.page)
            assertEquals(720,vm.completedWorkouts.single().elapsedSeconds())
            assertEquals("25",store.sets(id).first().weight);assertEquals(rows.map{it.id},store.sets(id).map{it.id})
        }
    }
    @Test fun actualWeightAndSameLoadRepetitionRecordsAreFreeAndEstimatesAreSeparate() {
        LocalStore(app).use {store ->
            val day=plan(store);val key=store.plans().single().exercises.first().key
            val first=finish(store,day,"20","8",1000000)
            val heavier=finish(store,day,"25","5",2000000)
            val moreReps=finish(store,day,"25","6",3000000)
            val lighter=finish(store,day,"24","30",4000000)
            val vm=RezimState(app);vm.setProForTest(false)
            assertFalse(vm.isRecordInSession(key,first));assertTrue(vm.isRecordInSession(key,heavier))
            assertTrue(vm.isRecordInSession(key,moreReps));assertFalse(vm.isRecordInSession(key,lighter))
            assertFalse(vm.exerciseRecordText(key).contains("1RM",ignoreCase=true))
            assertTrue(vm.exerciseRecordText(key,includeEstimate=true).contains("1RM",ignoreCase=true))
            assertTrue(vm.exerciseRecordText(key).contains("25"));assertEquals(4,vm.exerciseHistory(key).size)
            assertEquals(listOf("Own press"),vm.workoutRecords(moreReps))
        }
    }
    @Test fun bodyweightRecordAndCompletionAlertDoNotRequirePro() {
        LocalStore(app).use {store ->
            val day=plan(store,UnitType.REPS);val key=store.plans().single().exercises.first().key
            finish(store,day,"","10",System.currentTimeMillis()-86400000)
            val vm=RezimState(app);vm.setProForTest(false);vm.selectDay(day);vm.start()
            vm.changeSet(vm.workoutSets.first(),weight="0",reps="11")
            assertEquals(true,vm.toggle(vm.workoutSets.first(),UnitType.REPS));vm.finish()
            assertNull(vm.session);assertTrue(vm.isLatestRecord(key))
            assertTrue(vm.message!!.contains("New personal record"));assertTrue(vm.message!!.contains("Own press"))
        }
    }
    @Test fun openingHistoryWhileWorkoutIsRunningCannotOverwriteOrEditIt() {
        LocalStore(app).use {store ->
            val day=plan(store);val old=finish(store,day,"20","8")
            val vm=RezimState(app);vm.selectDay(day);vm.start()
            val open=vm.session;val sets=vm.workoutSets.toList();val before=store.exportJson()
            vm.page=Page.HOME;vm.openWorkout(old);vm.beginEditSavedWorkout(vm.completedWorkouts.single())
            assertNull(vm.editingSavedWorkout);assertEquals(Page.WORKOUT_DETAIL,vm.page)
            assertEquals(open,vm.session);assertEquals(sets,vm.workoutSets.toList());assertEquals(before,store.exportJson())
        }
    }
}
