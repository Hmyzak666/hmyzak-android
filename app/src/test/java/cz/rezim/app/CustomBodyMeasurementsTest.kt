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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CustomBodyMeasurementsTest {
    private lateinit var context: Context
    private lateinit var store: LocalStore
    private val databaseName = "rezim_local_v1.db"

    @Before fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.deleteDatabase(databaseName)
        store = LocalStore(context)
    }
    @After fun tearDown() { store.close(); context.deleteDatabase(databaseName) }

    private fun metric(name: String): String {
        store.saveBodyMetric(name)
        return store.bodyMetrics().last().id
    }

    @Test fun versionSixUpgradePreservesHistoryIncludingWeightOnlyRows() {
        store.close()
        val path = context.getDatabasePath(databaseName)
        path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            val fixture = javaClass.getResource("/fixtures/schema-v6.sql")!!.readText()
                .lineSequence().filterNot { it.trimStart().startsWith("--") }.joinToString("\n")
            fixture.split(';').filter { it.isNotBlank() }.forEach { db.execSQL(it) }
            db.execSQL("INSERT INTO measures VALUES (71,'2026-10-19',83.1,90.0)")
            db.execSQL("INSERT INTO measures VALUES (75,'2026-10-20',83.0,NULL)")
            db.execSQL("INSERT INTO sessions VALUES (41,'mon',1000,2000,3600)")
            db.execSQL("INSERT INTO workout_sets VALUES (101,41,'row',1,'42.5','12',1)")
            db.version = 6
        }
        store = LocalStore(context)
        assertEquals(10, store.readableDatabase.version)
        assertEquals(listOf(MeasureEntry(75,"2026-10-20",83.0,null), MeasureEntry(71,"2026-10-19",83.1,90.0)), store.measures())
        assertEquals("42.5", store.sets(41).single().weight)
        assertEquals(3600, store.recentSessions().single().durationSeconds)
        assertTrue(store.bodyMetrics().isEmpty())
        store.addMeasure("2026-10-21", null, 89.5)
        assertTrue(store.measures().first().id > 75)
        store.readableDatabase.rawQuery("PRAGMA integrity_check",null).use { it.moveToFirst();assertEquals("ok",it.getString(0)) }
        store.readableDatabase.rawQuery("PRAGMA foreign_key_check",null).use { assertFalse(it.moveToFirst()) }
    }

    @Test fun independentMeasurementsAndRenamedMetricsSurviveBackupRoundTrip() {
        val arm = metric("Biceps")
        val leg = metric("Stehno")
        store.addMeasure("2026-10-20", 83.1, null)
        store.addMeasure("2026-10-21", null, null, mapOf(arm to 36.5, leg to 58.0))
        store.saveBodyMetric("Levý biceps", arm)
        val before = store.exportJson()
        store.close(); context.deleteDatabase(databaseName); store = LocalStore(context)
        store.importJson(before)
        assertEquals(before, store.exportJson())
        assertNull(store.measures().first().kg)
        assertEquals(mapOf(arm to 36.5, leg to 58.0), store.measures().first().extra)
        assertEquals("Levý biceps", store.bodyMetrics().first().name)
        assertNull(store.measures().last().waist)
        assertTrue(store.measures().last().extra.isEmpty())
    }

    @Test fun editCanAddAndClearOptionalValuesWithoutChangingOtherDays() {
        val arm = metric("Biceps")
        val leg = metric("Stehno")
        store.addMeasure("2026-10-19", 83.1, 90.0)
        val older = store.measures().single()
        store.addMeasure("2026-10-20", null, null, mapOf(arm to 36.5, leg to 58.0))
        val current = store.measures().first()
        store.updateMeasure(current.copy(kg=83.2,extra=mapOf(leg to 58.5)))
        assertEquals(older, store.measures().last())
        assertEquals(mapOf(leg to 58.5), store.measures().first().extra)
        store.updateMeasure(store.measures().first().copy(kg=null,waist=89.0,extra=emptyMap()))
        assertTrue(store.measures().first().extra.isEmpty())
        assertNull(store.measures().first().kg)
    }

    @Test fun invalidOrEmptyEditsLeaveAllDataIntact() {
        val arm = metric("Biceps")
        store.addMeasure("2026-10-20", 83.1, null, mapOf(arm to 36.5))
        val record = store.measures().single()
        val before = store.exportJson()
        for (invalid in listOf(record.copy(kg=null,extra=emptyMap()),record.copy(extra=mapOf("unknown" to 36.0)),
            record.copy(extra=mapOf(arm to Double.NaN)),record.copy(kg=-1.0))) {
            assertThrows(IllegalArgumentException::class.java) { store.updateMeasure(invalid) }
            assertEquals(before,store.exportJson())
        }
    }

    @Test fun duplicateOrReservedMetricNamesAreRejected() {
        metric("Biceps")
        for (name in listOf(" bICEPS ","Břicho","", "x".repeat(41))) {
            assertThrows(IllegalArgumentException::class.java) { store.saveBodyMetric(name) }
        }
        assertEquals(1,store.bodyMetrics().size)
    }

    @Test fun corruptNewBackupRollsBackAllOriginalTables() {
        val arm = metric("Biceps")
        store.addMeasure("2026-10-20",83.1,null,mapOf(arm to 36.5))
        val before = store.exportJson()
        for (column in listOf("measurement_id","metric_id","cm")) {
            val broken = JSONObject(before)
            broken.getJSONArray("body_metric_values").getJSONObject(0).put(column,
                when(column) { "measurement_id" -> 999L; "metric_id" -> "unknown"; else -> -1.0 })
            assertThrows(RuntimeException::class.java) { store.importJson(broken.toString()) }
            assertEquals(before,store.exportJson())
        }
        val missing = JSONObject(before).apply { remove("body_metrics") }
        assertThrows(org.json.JSONException::class.java) { store.importJson(missing.toString()) }
        assertEquals(before,store.exportJson())
    }

    @Test fun schemaSixBackupStillImportsWithoutNewArrays() {
        store.addMeasure("2026-10-20",83.1,null)
        val old = JSONObject(store.exportJson()).put("schemaVersion",6).apply {
            remove("body_metrics"); remove("body_metric_values")
        }
        metric("Biceps")
        store.importJson(old.toString())
        assertTrue(store.bodyMetrics().isEmpty())
        assertEquals(83.1,store.measures().single().kg!!,0.0)
        assertNull(store.measures().single().waist)
    }
}
