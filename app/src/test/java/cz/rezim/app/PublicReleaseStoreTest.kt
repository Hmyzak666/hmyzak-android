package cz.rezim.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class PublicReleaseStoreTest {
    private lateinit var context:Context
    @Before fun before() {
        context=RuntimeEnvironment.getApplication();context.deleteDatabase("rezim_local_v1.db")
        context.getSharedPreferences("hmyzak_prefs",0).edit().clear().commit()
    }
    @Test fun cleanInstallHasNoPlansOrHistoryAndWaitsForChoiceAcrossReopening() {
        repeat(2) {LocalStore(context).use {store ->
            assertTrue(store.needsOnboarding());assertTrue(store.plans().isEmpty())
            assertTrue(store.allSessions().isEmpty());assertTrue(store.runs().isEmpty())
            assertTrue(store.hiit().isEmpty());assertTrue(store.measures().isEmpty())
        }}
    }
    @Test fun emptyChoiceStaysEmptyAcrossRestartAndJsonRestore() {
        val backup=LocalStore(context).use {store ->
            store.completeSetup();assertFalse(store.needsOnboarding());assertTrue(store.plans().isEmpty());store.exportJson()
        }
        LocalStore(context).use {assertFalse(it.needsOnboarding());assertTrue(it.plans().isEmpty())}
        context.deleteDatabase("rezim_local_v1.db")
        LocalStore(context).use {it.importJson(backup);assertFalse(it.needsOnboarding());assertTrue(it.plans().isEmpty())}
    }
    @Test fun everyTemplateCreatesIndependentEditablePlansWithoutPersonalIds() {
        for(template in TrainingTemplates.all) {
            context.deleteDatabase("rezim_local_v1.db")
            LocalStore(context).use {store ->
                store.completeSetup(template)
                val first=store.plans();assertEquals(template.days.size,first.size)
                assertFalse(store.needsOnboarding())
                assertTrue(first.all {day -> day.exercises.all {it.sets==3 && it.note.isBlank()}})
                assertTrue(first.none {it.key in Plans.days.map {it.key}})
                store.completeSetup(template);assertEquals(first,store.plans())
                val day=first.first();val ex=day.exercises.first()
                store.renameDay(day.key,"My day");store.setExerciseSets(day.key,ex.key,4)
                store.addExercise(day.key,"Custom",2,UnitType.REPS)
                store.moveExercise(day.key,ex.key,1)
                val latest=store.plans().first();assertEquals("My day",latest.name)
                assertEquals(4,latest.exercises.first {it.key==ex.key}.sets)
                store.setSuperset(day.key,ex.key,latest.exercises.take(2).map {it.key})
                val session=store.beginWorkout(day.key);val saved=store.sets(session.id)
                store.finishWorkout(session.id)
                store.removeDay(day.key)
                assertEquals(saved,store.sets(session.id));assertEquals(1,store.countSessions())
                val ids=store.plans().map {it.key}
                store.insertTemplate(TrainingTemplates.all.last())
                assertTrue(store.plans().map {it.key}.containsAll(ids));assertEquals(1,store.countSessions())
            }
        }
    }
    @Test fun upgradeTenPreservesEveryValueAndSkipsOnboardingEvenWithEmptyOldPlans() {
        val sql=javaClass.getResource("/fixtures/schema-v9.sql")!!.readText()
        val file=context.getDatabasePath("rezim_local_v1.db");file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file,null).use {old ->
            sql.split(';').map {it.trim()}.filter {it.isNotEmpty()}.forEach {old.execSQL(it)}
            old.execSQL("ALTER TABLE plan_exercises ADD COLUMN load_multiplier INTEGER NOT NULL DEFAULT 1 CHECK(load_multiplier IN (1,2))")
            old.execSQL("INSERT INTO plan_days VALUES ('d','D','My own',0)")
            old.execSQL("INSERT INTO plan_exercises VALUES ('x','d','Bench',4,'KG',0,NULL,'Note','g',4,2)")
            old.execSQL("INSERT INTO plan_exercises VALUES ('y','d','Push ups',1,'REPS',1,NULL,'Note 2','g',0,1)")
            old.execSQL("INSERT INTO sessions VALUES (12,'d',1000,61000,60,4)")
            old.execSQL("INSERT INTO workout_sets VALUES (22,12,'x',1,'20,5','10',1)")
            old.execSQL("INSERT INTO hiit VALUES (21,'2026-10-20',20,8,30,90,140,160,'Keep',2,3,2,8,3)")
            old.version=10
        }
        LocalStore(context).use {store ->
            assertFalse(store.needsOnboarding());assertEquals(11,store.readableDatabase.version)
            assertEquals(SetEntry(22,12,"x",1,"20,5","10",true),store.sets(12).single())
            val ex=store.plans().single().exercises
            assertEquals(listOf(4,0),ex.map {it.supersetStartRound});assertEquals(listOf(2,1),ex.map {it.loadMultiplier})
            assertEquals("Keep",store.hiit().single().note);assertEquals("OTHER",store.hiit().single().activity)
            store.removeDay("d");assertFalse(store.needsOnboarding())
        }
    }
    @Test fun hiitActivitiesRoundTripAndLegacyBackupsStayUnspecified() {
        LocalStore(context).use {store ->
            for(type in HiitActivity.entries) store.addHiit(HiitEntry(0,"2026-10-20",20,8,30,90,null,null,"",activity=type.name))
            val before=store.hiit();val backup=store.exportJson();store.importJson(backup);assertEquals(before,store.hiit())
            val legacy=JSONObject(backup).put("schemaVersion",10)
            legacy.getJSONArray("hiit").let {rows ->for(i in 0 until rows.length()) rows.getJSONObject(i).remove("activity")}
            store.importJson(legacy.toString());assertTrue(store.hiit().all {it.activity=="OTHER"})
            assertFalse(store.needsOnboarding())
            val corrupt=JSONObject(store.exportJson());corrupt.getJSONArray("hiit").getJSONObject(0).put("activity","BAD")
            val safe=store.exportJson();assertThrows(Exception::class.java) {store.importJson(corrupt.toString())}
            assertEquals(safe,store.exportJson())
        }
    }
    @Test fun checkpointProducesStandaloneDatabaseIncludingCommittedWalRows() {
        LocalStore(context).use {store ->
            store.writableDatabase.enableWriteAheadLogging()
            store.completeSetup(TrainingTemplates.all.first())
            val file=context.getDatabasePath("rezim_local_v1.db")
            checkpointForBackup(file)
            val copy=File(context.cacheDir,"checkpoint.db");file.copyTo(copy,true)
            SQLiteDatabase.openDatabase(copy.path,null,SQLiteDatabase.OPEN_READONLY).use {db ->
                assertEquals("ok",db.rawQuery("PRAGMA integrity_check",null).use {it.moveToFirst();it.getString(0)})
                assertEquals(3,db.rawQuery("SELECT count(*) FROM plan_days",null).use {it.moveToFirst();it.getInt(0)})
                assertEquals("1",db.rawQuery("SELECT value FROM app_state WHERE id='setup_complete'",null).use {it.moveToFirst();it.getString(0)})
            }
        }
    }
    @Test fun settingsBackupWhitelistsNoPurchaseOrLiveTimerAndRejectsInvalidValues() {
        val prefs=context.getSharedPreferences("hmyzak_prefs",0)
        prefs.edit().putInt("design",1).putInt("rest_seconds",120).putBoolean("haptic",false)
            .putString("rest_mode","AFTER_SET").putString("purchaseToken","secret").commit()
        val exported=BackupPreferences.export(context);assertFalse(exported.has("purchaseToken"))
        // Use an actual supported mode to avoid coupling this test to a display label.
        exported.put("rest_mode",RestStartMode.entries.last().name)
        val valid=BackupPreferences.validate(exported)
        prefs.edit().clear().commit();BackupPreferences.restore(context,valid)
        assertEquals(1,prefs.getInt("design",-1));assertEquals(120,prefs.getInt("rest_seconds",-1));assertFalse(prefs.getBoolean("haptic",true))
        assertThrows(IllegalArgumentException::class.java) {BackupPreferences.validate(JSONObject().put("design",999))}
        assertThrows(IllegalArgumentException::class.java) {BackupPreferences.validate(JSONObject().put("rest_mode","BAD"))}
        assertThrows(IllegalArgumentException::class.java) {BackupPreferences.validate(JSONObject().put("language","xx"))}
    }
    @Test fun restoringOldAndroidLanguageSetsNewAndroidLocaleWithoutTouchingTrainingData() {
        LocalStore(context).use {store ->
            store.completeSetup(TrainingTemplates.all.first());val before=store.exportJson()
            AppLanguage.choose(context,"")
            context.getSharedPreferences("hmyzak_prefs",0).edit().putString("language","de").putBoolean("haptic",false).commit()
            try {
                finishSystemRestore(context)
                assertEquals("de",AppLanguage.selected(context));assertFalse(store.needsOnboarding())
                assertEquals(before,store.exportJson());assertFalse(context.getSharedPreferences("hmyzak_prefs",0).getBoolean("haptic",true))
            } finally {AppLanguage.choose(context,"")}
        }
    }
    @Test fun backupCapturesActualSystemLocaleAndSystemLanguageWinsWhenAlreadyRestored() {
        try {
            AppLanguage.choose(context,"fr")
            context.getSharedPreferences("hmyzak_prefs",0).edit().putString("language","de").commit()
            captureBackupLanguage(context)
            assertEquals("fr",context.getSharedPreferences("hmyzak_prefs",0).getString("language",null))
            finishSystemRestore(context);assertEquals("fr",AppLanguage.selected(context))
        } finally {AppLanguage.choose(context,"")}
        captureBackupLanguage(context)
        assertEquals("",context.getSharedPreferences("hmyzak_prefs",0).getString("language",null))
    }
    @Test fun systemRestoreClearsOnlyDeviceLocalPurchaseAndTimerCaches() {
        context.getSharedPreferences("liftant_pro_purchase",0).edit().putString("purchase","test-token").commit()
        context.getSharedPreferences("hmyzak_rest_timer",0).edit().putLong("end",123).commit()
        context.getSharedPreferences("hmyzak_prefs",0).edit().putInt("rest_seconds",120).commit()
        finishSystemRestore(context)
        assertTrue(context.getSharedPreferences("liftant_pro_purchase",0).all.isEmpty())
        assertTrue(context.getSharedPreferences("hmyzak_rest_timer",0).all.isEmpty())
        assertEquals(120,context.getSharedPreferences("hmyzak_prefs",0).getInt("rest_seconds",0))
    }

}
