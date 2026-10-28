package cz.rezim.app

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class CardioCompatibilityTest {
    private lateinit var app:Application
    @Before fun clean() {
        app=RuntimeEnvironment.getApplication();app.deleteDatabase("rezim_local_v1.db")
        app.getSharedPreferences("hmyzak_prefs",0).edit().clear().commit()
    }
    @Test fun clocksCannotConfuseNinetySecondsWithNinetyMinutes() {
        assertEquals(90,durationSeconds("01:30",false))
        assertEquals(90,durationSeconds("00:01:30"))
        assertEquals(5400,durationSeconds("01:30:00"))
        listOf("01:30","1","00:60:00","00:00:60","99:99:99","-1:00:00","00:00:0x").forEach {assertNull(it,durationSeconds(it))}
        for(n in listOf(0,1,59,60,90,3599,3600,3601,172800,359999)) assertEquals(n,durationSeconds(durationText(n)))
        for(n in listOf(0,1,59,90,3600)) assertEquals(n,durationSeconds(durationText(n,false),false))
    }
    @Test fun installedSchemaElevenPreservesEveryRunningValueAndHiitInterval() {
        val file=app.getDatabasePath("rezim_local_v1.db");file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file,null).use {db->
            javaClass.getResource("/fixtures/schema-v10.sql")!!.readText().lineSequence().filterNot {it.trim().startsWith("--")}.joinToString("\n").split(';').filter {it.isNotBlank()}.forEach {db.execSQL(it)}
            db.execSQL("ALTER TABLE hiit ADD COLUMN activity TEXT NOT NULL DEFAULT 'OTHER' CHECK(activity IN ('OTHER','ELLIPTICAL','RUNNING','CYCLING','ROWING'))")
            db.execSQL("CREATE TABLE app_state (id TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)")
            db.execSQL("INSERT INTO app_state VALUES ('setup_complete','1')")
            db.execSQL("INSERT INTO runs VALUES (71,'2026-10-20',10.03,3619,146,162,'Keep this run')")
            db.execSQL("INSERT INTO hiit VALUES (81,'2026-10-21',15,5,60,120,130,150,'Keep HIIT',NULL,NULL,NULL,NULL,NULL,'ELLIPTICAL')")
            db.version=11
        }
        LocalStore(app).use {store->
            assertEquals(13,store.readableDatabase.version)
            val run=store.runs().single();assertEquals(71L,run.id);assertEquals(10.03,run.km,0.0);assertEquals(3619,run.seconds)
            assertEquals("RUNNING",run.activity);assertEquals("Keep this run",run.note);assertTrue(run.isRunning)
            assertEquals(60,store.hiit().single().work);assertNull(store.hiit().single().totalSeconds)
            val old=JSONObject(store.exportJson()).put("schemaVersion",11)
            val row=old.getJSONArray("runs").getJSONObject(0)
            listOf("activity","custom_name","resistance","incline").forEach {row.remove(it)}
            old.getJSONArray("hiit").getJSONObject(0).remove("total_seconds")
            store.importJson(old.toString());assertEquals(run,store.runs().single())
        }
    }
    @Test fun allCardioTypesRoundTripWithoutDistanceAndWithoutPollutingRunningGraphs() {
        val vm=RezimState(app)
        for(a in CardioActivity.entries) vm.addCardio(RunEntry(0,"2026-10-26",0.0,90,130,150,"note",a.name,if(a==CardioActivity.OTHER) "Boxing" else ""))
        assertEquals(8,vm.cardio.size);assertTrue(vm.runs.isEmpty())
        vm.addCardio(RunEntry(0,"2026-10-26",5.0,1800,null,null,"","TREADMILL"))
        assertEquals(1,vm.runs.size)
        val backup=vm.exportBackup();vm.importBackup(backup)
        assertEquals(9,vm.cardio.size);assertEquals(1,vm.runs.size);assertTrue(vm.cardio.all {it.seconds in listOf(90,1800)})
        assertTrue(vm.shareSummary(RezimState.ExportScope.ALL).contains("Boxing"))
    }
    @Test fun invalidCardioOrCorruptBackupCannotReplaceExistingHistory() {
        LocalStore(app).use {store->
            val valid=RunEntry(0,"2026-10-26",0.0,90,null,null,"","ELLIPTICAL",resistance=8.0)
            store.addCardio(valid);val before=store.exportJson()
            for(bad in listOf(valid.copy(seconds=0),valid.copy(km=Double.NaN),valid.copy(avg=180,max=130),valid.copy(activity="OTHER"),valid.copy(incline=101.0))) {
                assertTrue(runCatching {store.addCardio(bad)}.isFailure);assertEquals(before,store.exportJson())
            }
            val bad=JSONObject(before);bad.getJSONArray("runs").getJSONObject(0).put("seconds",-90)
            assertTrue(runCatching {store.importJson(bad.toString())}.isFailure);assertEquals(before,store.exportJson())
        }
    }
    @Test fun shortcutsRestoreOrderNamesAndHiitPresetAndMissingPlansAreSafe() {
        val vm=RezimState(app)
        val presets=listOf(QuickShortcut(id="ell",kind="CARDIO",target="ELLIPTICAL",name="Easy"),
            QuickShortcut(id="hiit-a",kind="HIIT",name="5 rounds",rounds=5,work=60,rest=120),
            QuickShortcut(id="hiit-b",kind="HIIT",rounds=8,work=30,rest=90),QuickShortcut(id="missing",kind="STRENGTH",target="deleted"))
        vm.saveShortcuts(presets);vm.moveShortcut(0,1)
        val before=vm.quickShortcuts;val backup=vm.exportBackup()
        vm.saveShortcuts(emptyList());vm.importBackup(backup);assertEquals(before,vm.quickShortcuts)
        val restart=RezimState(app);assertEquals(before,restart.quickShortcuts)
        restart.launchShortcut(presets[0]);assertEquals("ELLIPTICAL",restart.cardioActivity);assertEquals(Page.RUN,restart.page);assertTrue(restart.cardio.isEmpty())
        restart.launchShortcut(presets[1]);assertEquals(presets[1],restart.hiitPreset);assertTrue(restart.hiit.isEmpty());assertNull(restart.session)
        restart.launchShortcut(presets[3]);assertNotNull(restart.message);assertNull(restart.session)
        val bad=JSONObject(backup);bad.getJSONObject("settings").put("quick_shortcuts","[{}]")
        val saved=restart.exportBackup();assertTrue(runCatching {restart.importBackup(bad.toString())}.isFailure);assertEquals(saved,restart.exportBackup())
    }
    @Test fun exactHiitDurationSurvivesEditingAndBackupWithoutChangingLegacyMinutes() {
        LocalStore(app).use {store->
            store.addHiit(HiitEntry(0,"2026-10-26",2,3,15,30,null,null,"",totalSeconds=90))
            val old=store.hiit().single();store.updateHiit(old.copy(note="Edited"));val json=store.exportJson();store.importJson(json)
            assertEquals(90,store.hiit().single().totalSeconds);assertEquals(2,store.hiit().single().minutes)
        }
    }
}
