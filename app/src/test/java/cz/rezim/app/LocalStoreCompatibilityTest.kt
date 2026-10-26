package cz.rezim.app

import android.content.Context
import android.database.Cursor
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
@Config(sdk = [28])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class LocalStoreCompatibilityTest {
    private lateinit var context: Context
    private val stores = mutableListOf<LocalStore>()
    private val databaseName = "rezim_local_v1.db"
    private val tables = listOf("sessions", "workout_sets", "runs", "hiit", "measures", "plan_days", "plan_exercises")

    @Before fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.deleteDatabase(databaseName)
    }

    @After fun tearDown() {
        stores.forEach { it.close() }
        context.deleteDatabase(databaseName)
    }

    private fun store() = LocalStore(context).withLegacyTestPlans().also { stores += it }

    private fun newDatabase(): LocalStore {
        stores.forEach { it.close() }
        context.deleteDatabase(databaseName)
        return store()
    }

    private fun representativeData(db: LocalStore): String {
        val day = db.addDay("Vlastní den")
        db.addExercise(day, "Přítahy", 2, UnitType.KG)
        db.addExercise(day, "Tlaky", 2, UnitType.KG)
        val exercises = db.plans().first { it.key == day }.exercises
        db.updateExerciseDetails(day, exercises[0].key, "Přítahy", "Úchop úzký; lavička 30°")
        db.pair(day, exercises[0].key, exercises[1].key)
        val finished = db.beginWorkout(day)
        db.sets(finished.id).forEach { db.updateSet(it.copy(weight = "42.5", reps = "12", done = true)) }
        db.finishWorkout(finished.id)
        db.updateWorkoutDuration(finished.id, 2800)
        val open = db.beginWorkout(day)
        db.updateSet(db.sets(open.id).first().copy(weight = "45", reps = "10", done = true))
        db.addRun("2026-10-20", 5.25, 1800, 145, 160, "Testovací běh")
        db.addHiit(HiitEntry(0, "2026-10-20", 20, 10, 30, 90, 145, 169, "Testovací HIIT",
            km = 2.5, warmup = 5, cooldown = 3, workResistance = 8, restResistance = 3))
        db.addMeasure("2026-10-19", 83.1, 90.0)
        db.addMeasure("2026-10-20", 83.0, null)
        return day
    }

    @Test fun jsonRoundTripPreservesHistoryPlansNotesOpenWorkoutAndUnmeasuredWaist() {
        val original = store()
        val day = representativeData(original)
        val openId = original.firstOpenSession()!!.id
        val backup = original.exportJson()
        val root = JSONObject(backup)
        assertEquals(12, root.getInt("schemaVersion"))
        assertTrue(root.getJSONArray("measures").getJSONObject(1).isNull("waist"))

        val restored = newDatabase()
        restored.importJson(backup)
        assertEquals(backup, restored.exportJson())
        assertEquals(openId, restored.firstOpenSession()!!.id)
        assertEquals(3, restored.sets(openId).count { !it.done })
        assertEquals(2800, restored.recentSessions().first().durationSeconds)
        val exercises = restored.plans().first { it.key == day }.exercises
        assertEquals("Úchop úzký; lavička 30°", exercises.first().note)
        assertEquals(exercises[1].key, exercises[0].partnerKey)
        assertNull(restored.measures().first().waist)
    }

    @Test fun reopeningCurrentDatabasePreservesAllExistingData() {
        val original = store()
        representativeData(original)
        val before = original.exportJson()
        original.close()
        assertEquals(before, store().exportJson())
    }

    @Test fun versionFiveDatabaseMigratesWithoutChangingAnyExistingRow() {
        val path = context.getDatabasePath(databaseName)
        path.parentFile!!.mkdirs()
        val before: Map<String, List<List<String?>>>
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            val fixture = javaClass.getResource("/fixtures/schema-v5.sql")!!.readText()
                .lineSequence().filterNot { it.trimStart().startsWith("--") }.joinToString("\n")
            fixture.split(';').filter { it.isNotBlank() }.forEach { db.execSQL(it) }
            db.execSQL("INSERT INTO plan_days VALUES ('custom','VL','Vlastní',0)")
            db.execSQL("INSERT INTO plan_exercises VALUES ('a','custom','Přítahy',4,'KG',0,'b','Vlastní poznámka')")
            db.execSQL("INSERT INTO plan_exercises VALUES ('b','custom','Tlaky',4,'KG',1,'a','')")
            db.execSQL("INSERT INTO sessions VALUES (41,'custom',1000,2000,3600)")
            db.execSQL("INSERT INTO sessions VALUES (42,'custom',3000,NULL,NULL)")
            db.execSQL("INSERT INTO workout_sets VALUES (101,41,'a',1,'42.5','12',1)")
            db.execSQL("INSERT INTO workout_sets VALUES (102,42,'b',1,'40','10',0)")
            db.execSQL("INSERT INTO runs VALUES (51,'2026-10-15',5.2,1800,145,160,'Dřívější běh')")
            db.execSQL("INSERT INTO hiit VALUES (61,'2026-10-17',20,10,30,90,145,165,'Dřívější HIIT',2.5,5,3,8,3)")
            db.execSQL("INSERT INTO measures VALUES (71,'2026-10-19',83.1,90.0)")
            db.version = 5
            before = snapshot(db)
        }
        val upgraded = store()
        assertEquals(12, upgraded.readableDatabase.version)
        assertEquals(before, snapshot(upgraded.readableDatabase))
        upgraded.addMeasure("2026-10-20", 83.0, null)
        assertNull(upgraded.measures().first().waist)
        assertTrue(upgraded.measures().first().id > 71)
        upgraded.readableDatabase.rawQuery("PRAGMA integrity_check", null).use {
            assertTrue(it.moveToFirst()); assertEquals("ok", it.getString(0))
        }
        upgraded.readableDatabase.rawQuery("PRAGMA foreign_key_check", null).use { assertFalse(it.moveToFirst()) }
    }

    @Test fun backupsFromVersionsTwoThroughSixRemainImportable() {
        val original = store()
        val day = representativeData(original)
        val backup = original.exportJson()
        for (schema in 2..6) {
            val root = JSONObject(backup).put("schemaVersion", schema)
            root.remove("body_metrics"); root.remove("body_metric_values")
            // Older schemas only contained measurements with a circumference.
            root.getJSONArray("measures").remove(1)
            if (schema < 5) removeColumn(root, "sessions", "duration_seconds")
            if (schema < 4) for (column in listOf("km", "warmup_minutes", "cooldown_minutes", "work_resistance", "rest_resistance")) {
                removeColumn(root, "hiit", column)
            }
            if (schema < 3) removeColumn(root, "plan_exercises", "note")
            val restored = newDatabase()
            restored.importJson(root.toString())
            assertEquals(1, restored.countSessions())
            assertNotNull(restored.firstOpenSession())
            val exercises = restored.plans().first { it.key == day }.exercises
            assertEquals(if (schema < 3) "" else "Úchop úzký; lavička 30°", exercises.first().note)
            assertEquals(exercises[1].key, exercises.first().partnerKey)
            assertEquals(if (schema < 5) null else 2800, restored.recentSessions().first().durationSeconds)
            assertEquals(if (schema < 4) null else 2.5, restored.hiit().first().km)
            assertEquals(90.0, restored.measures().single().waist)
        }
    }

    @Test fun versionOneBackupRestoresDefaultPlansAndOldWorkout() {
        val original = store()
        val day = original.plans().first()
        val session = original.beginWorkout(day.key)
        original.updateSet(original.sets(session.id).first().copy(weight = "25", reps = "12", done = true))
        original.finishWorkout(session.id)
        original.addMeasure("2026-10-19", 83.1, 90.0)
        val backup = JSONObject(original.exportJson()).put("schemaVersion", 1)
        backup.remove("plan_days"); backup.remove("plan_exercises")
        removeColumn(backup, "sessions", "duration_seconds")
        val restored = newDatabase()
        restored.importJson(backup.toString())
        assertEquals(Plans.days.map { it.key }, restored.plans().map { it.key })
        assertEquals(1, restored.countSessions())
        assertEquals("25", restored.sets(session.id).first().weight)
        assertEquals(90.0, restored.measures().single().waist)
    }

    @Test fun failedImportRollsBackDeletionsAndPartiallyInsertedRows() {
        val original = store()
        representativeData(original)
        val before = original.exportJson()
        val broken = JSONObject(before)
        broken.getJSONArray("workout_sets").getJSONObject(0).put("reps", true)
        assertThrows(IllegalStateException::class.java) { original.importJson(broken.toString()) }
        assertEquals(before, original.exportJson())
    }

    @Test fun unsupportedOrIncompleteBackupCannotReplaceExistingData() {
        val original = store()
        representativeData(original)
        val before = original.exportJson()
        assertThrows(IllegalArgumentException::class.java) {
            original.importJson(JSONObject(before).put("schemaVersion", 99).toString())
        }
        val missing = JSONObject(before).apply { remove("hiit") }
        assertThrows(org.json.JSONException::class.java) { original.importJson(missing.toString()) }
        assertEquals(before, original.exportJson())
    }

    private fun removeColumn(root: JSONObject, table: String, column: String) {
        val rows = root.getJSONArray(table)
        for (i in 0 until rows.length()) rows.getJSONObject(i).remove(column)
    }

    private fun snapshot(db: SQLiteDatabase) = tables.associateWith { table ->
        db.rawQuery("SELECT " + (if(table=="sessions") "id,day,created,finished,duration_seconds" else if(table=="runs") "id,date,km,seconds,avg_hr,max_hr,note" else if(table=="hiit") "id,date,minutes,rounds,work_seconds,rest_seconds,avg_hr,max_hr,note,km,warmup_minutes,cooldown_minutes,work_resistance,rest_resistance" else if(table=="plan_exercises") "id,day_id,name,sets,unit,position,partner_id,note" else "*") + " FROM $table ORDER BY id", null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add((0 until cursor.columnCount).map { index ->
                    if (cursor.getType(index) == Cursor.FIELD_TYPE_NULL) null else cursor.getString(index)
                })
            }
        }
    }
}
