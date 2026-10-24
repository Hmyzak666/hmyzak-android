package cz.rezim.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class WorkoutToolsStoreTest {
    private lateinit var context:Context
    private lateinit var store:LocalStore
    @Before fun before() {context=RuntimeEnvironment.getApplication();context.deleteDatabase("rezim_local_v1.db");store=LocalStore(context).withLegacyTestPlans()}
    @After fun after() {store.close();context.deleteDatabase("rezim_local_v1.db")}
    private fun day():Pair<String,List<Exercise>> {
        val day=store.addDay("Supersérie")
        listOf("Curl","Extension","Abduction","Adduction").forEachIndexed {i,name ->store.addExercise(day,name,if(i==3) 1 else 4,UnitType.KG)}
        return day to store.plans().first {it.key==day}.exercises
    }
    @Test fun versionSevenMigrationKeepsAllOldColumnsAndConvertsPairs() {
        store.close();context.deleteDatabase("rezim_local_v1.db")
        val path=context.getDatabasePath("rezim_local_v1.db");path.parentFile!!.mkdirs()
        val before:Map<String,List<List<String?>>>
        val columns:Map<String,List<String>>
        SQLiteDatabase.openOrCreateDatabase(path,null).use {db ->
            javaClass.getResource("/fixtures/schema-v7.sql")!!.readText().lineSequence()
                .filterNot {it.trimStart().startsWith("--")}.joinToString("\n").split(';').filter {it.isNotBlank()}.forEach {db.execSQL(it)}
            db.execSQL("INSERT INTO plan_days VALUES ('d','D','Den',0)")
            db.execSQL("INSERT INTO plan_exercises VALUES ('a','d','A',4,'KG',0,'b','Poznámka')")
            db.execSQL("INSERT INTO plan_exercises VALUES ('b','d','B',4,'KG',1,'a','')")
            db.execSQL("INSERT INTO sessions VALUES (11,'d',1000,2000,3600)")
            db.execSQL("INSERT INTO sessions VALUES (12,'d',3000,NULL,NULL)")
            db.execSQL("INSERT INTO workout_sets VALUES (21,12,'a',1,'42.5','12',1)")
            db.execSQL("INSERT INTO measures VALUES (31,'2026-10-21',NULL,NULL)")
            db.execSQL("INSERT INTO body_metrics VALUES ('arm','Biceps',0)")
            db.execSQL("INSERT INTO body_metric_values VALUES (41,31,'arm',36.5)")
            db.version=7
            val tables=listOf("sessions","workout_sets","runs","hiit","measures","plan_days","plan_exercises","body_metrics","body_metric_values")
            columns=tables.associateWith {t ->db.rawQuery("PRAGMA table_info($t)",null).use {c ->buildList {while(c.moveToNext()) add(c.getString(1))}}}
            before=snapshot(db,columns)
        }
        store=LocalStore(context).withLegacyTestPlans()
        assertEquals(11,store.readableDatabase.version)
        assertEquals(before,snapshot(store.readableDatabase,columns))
        val ex=store.plans().single().exercises
        assertNotNull(ex[0].supersetId);assertEquals(ex[0].supersetId,ex[1].supersetId)
        assertEquals(0,store.firstOpenSession()!!.elapsedOffsetSeconds)
        assertEquals(3600,store.recentSessions().single().elapsedSeconds())
    }
    @Test fun changingGroupsAndNotesDuringWorkoutPreservesAllSetValuesAndHistory() {
        val (day,ex)=day();store.setSuperset(day,ex[0].key,ex.take(2).map {it.key})
        val session=store.beginWorkout(day)
        store.updateSet(store.sets(session.id).first().copy(weight="45",reps="11",done=true))
        val before=store.sets(session.id)
        store.setSuperset(day,ex[0].key,ex.map {it.key})
        store.updateExerciseNote(day,ex[0].key,"  Nový úchop  ")
        assertEquals(before,store.sets(session.id))
        assertEquals("Nový úchop",store.plans().first {it.key==day}.exercises[0].note)
        assertEquals(4,supersetMembers(store.plans().first {it.key==day}.exercises,ex[0].key).size)
        assertEquals(session.id,store.firstOpenSession()!!.id)
    }
    @Test fun removingMemberPreservesTheOtherThreeAndMergingGroupsDetachesOldPartners() {
        val (day,ex)=day();val keys=ex.map {it.key}
        store.setSuperset(day,keys[0],keys)
        store.setSuperset(day,keys[3],listOf(keys[3]))
        var plan=store.plans().first {it.key==day}.exercises
        assertEquals(3,supersetMembers(plan,keys[0]).size);assertEquals(1,supersetMembers(plan,keys[3]).size)
        store.setSuperset(day,keys[1],listOf(keys[1],keys[3]))
        plan=store.plans().first {it.key==day}.exercises
        assertEquals(setOf(keys[0],keys[2]),supersetMembers(plan,keys[0]).map {it.key}.toSet())
        assertEquals(setOf(keys[1],keys[3]),supersetMembers(plan,keys[1]).map {it.key}.toSet())
        store.removeExercise(day,keys[1])
        plan=store.plans().first {it.key==day}.exercises
        assertNull(plan.first {it.key==keys[3]}.supersetId);assertNull(plan.first {it.key==keys[3]}.partnerKey)
    }
    @Test fun invalidGroupCannotChangeCurrentPlan() {
        val (day,ex)=day();val before=store.exportJson()
        assertThrows(IllegalArgumentException::class.java) {store.setSuperset(day,ex[0].key,listOf(ex[0].key,"missing"))}
        assertEquals(before,store.exportJson())
        assertThrows(IllegalArgumentException::class.java) {store.setSuperset(day,ex[0].key,listOf(ex[0].key,ex[0].key))}
        assertEquals(before,store.exportJson())
    }
    @Test fun liveDurationCorrectionPreservesStartTimestampSurvivesReopenAndFinishesNormally() {
        val (day,_)=day();val session=store.beginWorkout(day)
        store.updateWorkoutDuration(session.id,600,session.created+120000)
        val corrected=store.firstOpenSession()!!
        assertEquals(session.created,corrected.created)
        assertEquals(600,corrected.elapsedSeconds(session.created+120000))
        assertEquals(610,corrected.elapsedSeconds(session.created+130000))
        assertEquals(480,corrected.elapsedOffsetSeconds)
        val backup=store.exportJson();store.importJson(backup)
        assertEquals(corrected,store.firstOpenSession())
        store.finishWorkout(session.id)
        assertNotNull(store.recentSessions().first().finished)
        store.updateWorkoutDuration(session.id,1800)
        assertEquals(1800,store.recentSessions().first().elapsedSeconds())
    }
    @Test fun schemaEightRoundTripAndInvalidGroupRollback() {
        val (day,ex)=day();store.setSuperset(day,ex[0].key,ex.map {it.key})
        val backup=store.exportJson();assertEquals(11,JSONObject(backup).getInt("schemaVersion"))
        store.importJson(backup);assertEquals(backup,store.exportJson())
        val broken=JSONObject(backup)
        broken.getJSONArray("plan_exercises").getJSONObject(0).put("superset_id","lonely")
        assertThrows(IllegalArgumentException::class.java) {store.importJson(broken.toString())}
        assertEquals(backup,store.exportJson())
    }
    private fun snapshot(db:SQLiteDatabase,columns:Map<String,List<String>>) = columns.mapValues {(table,cols) ->
        db.rawQuery("SELECT ${cols.joinToString(",")} FROM $table ORDER BY id",null).use {c ->buildList {
            while(c.moveToNext()) add(cols.indices.map {if(c.isNull(it)) null else c.getString(it)})
        }}
    }
}
