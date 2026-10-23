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
class SupersetRoundsStoreTest {
    private lateinit var context:Context
    private lateinit var store:LocalStore
    @Before fun before() {context=RuntimeEnvironment.getApplication();context.deleteDatabase("rezim_local_v1.db");store=LocalStore(context)}
    @After fun after() {store.close();context.deleteDatabase("rezim_local_v1.db")}
    private fun create():Pair<String,List<Exercise>> {
        val day=store.addDay("Rounds")
        store.addExercise(day,"Bench",4,UnitType.KG);store.addExercise(day,"Push-ups",1,UnitType.REPS)
        val ex=store.plans().first {it.key==day}.exercises
        store.setSuperset(day,ex[0].key,ex.map {it.key})
        return day to ex
    }
    @Test fun installedVersionEightUpgradePreservesEveryExistingColumn() {
        store.close();val path=context.getDatabasePath("rezim_local_v1.db");path.parentFile!!.mkdirs()
        val columns:Map<String,List<String>>;val before:Map<String,List<List<String?>>>
        SQLiteDatabase.openOrCreateDatabase(path,null).use {db ->
            javaClass.getResource("/fixtures/schema-v8.sql")!!.readText().lineSequence()
                .filterNot {it.trimStart().startsWith("--")}.joinToString("\n").split(';').filter {it.isNotBlank()}.forEach {db.execSQL(it)}
            db.execSQL("INSERT INTO plan_days VALUES ('d','D','Day',0)")
            db.execSQL("INSERT INTO plan_exercises VALUES ('a','d','Bench',4,'KG',0,'b','Keep note','g')")
            db.execSQL("INSERT INTO plan_exercises VALUES ('b','d','Push-ups',1,'REPS',1,'a','','g')")
            db.execSQL("INSERT INTO sessions VALUES (11,'d',1000,NULL,NULL,120)")
            db.execSQL("INSERT INTO workout_sets VALUES (21,11,'a',1,'45','12',1)")
            db.execSQL("INSERT INTO measures VALUES (31,'2026-10-23',NULL,NULL)")
            db.execSQL("INSERT INTO body_metrics VALUES ('arm','Biceps',0)")
            db.execSQL("INSERT INTO body_metric_values VALUES (41,31,'arm',36.5)")
            val names=listOf("sessions","workout_sets","runs","hiit","measures","plan_days","plan_exercises","body_metrics","body_metric_values")
            columns=names.associateWith {t ->db.rawQuery("PRAGMA table_info($t)",null).use {c ->buildList {while(c.moveToNext()) add(c.getString(1))}}}
            before=snapshot(db,columns);db.version=8
        }
        store=LocalStore(context);assertEquals(10,store.readableDatabase.version)
        assertEquals(before,snapshot(store.readableDatabase,columns))
        assertTrue(store.plans().single().exercises.all {it.supersetStartRound==1})
        store.readableDatabase.rawQuery("PRAGMA integrity_check",null).use {assertTrue(it.moveToFirst());assertEquals("ok",it.getString(0))}
        store.readableDatabase.rawQuery("PRAGMA foreign_key_check",null).use {assertFalse(it.moveToFirst())}
    }
    @Test fun roundChangesSurviveReopenAndBackupWithoutChangingWorkoutSets() {
        val (day,ex)=create();val keys=ex.map {it.key};val session=store.beginWorkout(day)
        store.updateSet(store.sets(session.id).first().copy(weight="45",reps="12",done=true))
        val before=store.sets(session.id)
        for(round in listOf(0,3,1)) {
            store.setSuperset(day,keys[0],keys,mapOf(keys[1] to round))
            assertEquals(before,store.sets(session.id))
            val backup=store.exportJson();assertEquals(10,JSONObject(backup).getInt("schemaVersion"))
            store.close();store=LocalStore(context);store.importJson(backup)
            assertEquals(backup,store.exportJson())
            assertEquals(round,store.plans().first {it.key==day}.exercises[1].supersetStartRound)
            assertEquals(before,store.sets(session.id))
        }
    }
    @Test fun schemaSevenAndEightBackupsDefaultToFirstRound() {
        val (day,ex)=create();store.setSuperset(day,ex[0].key,ex.map {it.key},mapOf(ex[1].key to 0))
        val original=store.exportJson()
        for(schema in 7..8) {
            val backup=JSONObject(original).put("schemaVersion",schema)
            val exercises=backup.getJSONArray("plan_exercises")
            for(i in 0 until exercises.length()) exercises.getJSONObject(i).remove("superset_start_round")
            store.importJson(backup.toString())
            assertTrue(store.plans().flatMap {it.exercises}.all {it.supersetStartRound==1})
            assertEquals(2,supersetMembers(store.plans().first {it.key==day}.exercises,ex[0].key).size)
        }
    }
    @Test fun invalidRoundChangesCannotPartiallyReplaceGroup() {
        val (day,ex)=create();val before=store.exportJson();val keys=ex.map {it.key}
        for(round in listOf(-1,16,Int.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) {store.setSuperset(day,keys[0],keys,mapOf(keys[1] to round))}
            assertEquals(before,store.exportJson())
        }
        assertThrows(IllegalArgumentException::class.java) {store.setSuperset(day,keys[0],keys,mapOf("missing" to 3))}
        assertEquals(before,store.exportJson())
    }
    @Test fun invalidRoundBackupRollsBackEveryTable() {
        create();val before=store.exportJson()
        for(value in listOf<Any>(-1,16,1.5,"3",Long.MAX_VALUE)) {
            val bad=JSONObject(before)
            bad.getJSONArray("plan_exercises").getJSONObject(0).put("superset_start_round",value)
            assertThrows(IllegalArgumentException::class.java) {store.importJson(bad.toString())}
            assertEquals(before,store.exportJson())
        }
    }
    private fun snapshot(db:SQLiteDatabase,columns:Map<String,List<String>>) = columns.mapValues {(t,cols) ->
        db.rawQuery("SELECT ${cols.joinToString(",")} FROM $t ORDER BY id",null).use {c ->buildList {
            while(c.moveToNext()) add(cols.indices.map {if(c.isNull(it)) null else c.getString(it)})
        }}
    }
}
