package cz.rezim.app

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.SQLiteMode
import java.io.File
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class LiftAntCompatibilityTest {
    private val context:Context get()=RuntimeEnvironment.getApplication()
    @Test fun schemaNineUpgradePreservesAllOldValuesAndAddsDumbbellConvention() {
        context.deleteDatabase("rezim_local_v1.db")
        val file=context.getDatabasePath("rezim_local_v1.db");file.parentFile!!.mkdirs()
        val old=SQLiteDatabase.openOrCreateDatabase(file,null)
        val sql=javaClass.getResource("/fixtures/schema-v9.sql")!!.readText()
        sql.split(';').map {it.trim()}.filter {it.isNotEmpty()}.forEach {old.execSQL(it)}
        old.execSQL("INSERT INTO plan_days VALUES ('d','D','Test',0)")
        old.execSQL("INSERT INTO plan_exercises VALUES ('flat_db_bench','d','DB',4,'KG',0,NULL,'Keep note',NULL,1)")
        old.execSQL("INSERT INTO sessions VALUES (12,'d',1000,61000,60,0)")
        old.execSQL("INSERT INTO workout_sets VALUES (22,12,'flat_db_bench',1,'20,5','10',1)")
        old.version=9;old.close()
        LocalStore(context).withLegacyTestPlans().use {store ->
            assertEquals(12,store.readableDatabase.version)
            assertEquals(SetEntry(22,12,"flat_db_bench",1,"20,5","10",true),store.sets(12).single())
            assertEquals("Keep note",store.plans().single().exercises.single().note)
            assertEquals(2,store.plans().single().exercises.single().loadMultiplier)
            store.setLoadMultiplier("d","flat_db_bench",1)
            val backup=store.exportJson();store.importJson(backup)
            assertEquals(1,store.plans().single().exercises.single().loadMultiplier)
            assertEquals("20,5",store.sets(12).single().weight)
            assertEquals("ok",store.readableDatabase.rawQuery("PRAGMA integrity_check",null).use {it.moveToFirst();it.getString(0)})
        }
    }
    @Test fun legacyCustomMetricNamesRemainValidInEveryLanguage() {
        context.deleteDatabase("rezim_local_v1.db")
        LocalStore(context).withLegacyTestPlans().use {store ->
            listOf("Weight","Poids","Peso","Gewicht","Вес").forEach {store.saveBodyMetric(it)}
            val before=store.exportJson()
            try {
                for(lang in AppLanguage.supported) {
                    AppLanguage.choose(context,lang)
                    store.importJson(before)
                    assertEquals(before,store.exportJson())
                }
            } finally {AppLanguage.choose(context,"")}
        }
    }
    @Test fun newUsersNeverReceivePersonalHistory() {
        context.deleteDatabase("rezim_local_v1.db")
        LocalStore(context).withLegacyTestPlans().use {store ->
            assertTrue(store.runs().isEmpty());assertTrue(store.measures().isEmpty())
            assertTrue(store.allSessions().isEmpty());assertTrue(store.hiit().isEmpty())
        }
    }
    @Test fun allTenLanguagesHaveFormattedStringsAndRenderBothShareCardSizes() {
        val previews=File("build/reports/liftant-previews").apply {mkdirs()}
        for(lang in AppLanguage.supported) {
            val config=Configuration(context.resources.configuration).apply {setLocale(Locale.forLanguageTag(lang))}
            val localized=context.createConfigurationContext(config)
            assertEquals("LiftAnt",localized.getString(R.string.app_name))
            assertTrue(localized.tr(R.string.round_range,3,4).contains("3"))
            assertFalse(localized.tr(R.string.pro_buy_price,"249 Kč").contains("%1"))
            for(story in listOf(false,true)) {
                val bitmap=createWorkoutCard(localized,WorkoutCardData("Upper · Training · Тренировка","24. 10. 2026","65 min",WorkoutTotals(28,312,12450.5)),story)
                assertEquals(1080,bitmap.width);assertEquals(if(story)1920 else 1080,bitmap.height)
                File(previews,"$lang-${if(story)"story" else "square"}.png").outputStream().use {bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
                bitmap.recycle()
            }
        }
    }
    @Test fun switchingLanguageLeavesDatabaseAndUserTextUntouched() {
        context.deleteDatabase("rezim_local_v1.db")
        LocalStore(context).withLegacyTestPlans().use {store ->
            val ex=store.plans().first().exercises.first()
            store.updateExerciseDetails("mon",ex.key,"Můj vlastní název","Moje poznámka")
            val before=store.exportJson()
            try {
                for(lang in AppLanguage.supported) {
                    AppLanguage.choose(context,lang)
                    assertEquals(lang,AppLanguage.wrap(context).resources.configuration.locales[0].toLanguageTag())
                    assertEquals(before,store.exportJson())
                }
            } finally {AppLanguage.choose(context,"")}
        }
    }
}
