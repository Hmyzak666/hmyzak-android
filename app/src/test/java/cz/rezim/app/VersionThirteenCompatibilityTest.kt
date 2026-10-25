package cz.rezim.app

import android.app.Application
import android.content.Context
import android.content.res.Configuration
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
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28,34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class VersionThirteenCompatibilityTest {
    private lateinit var app:Application
    @Before fun before() {
        app=RuntimeEnvironment.getApplication();app.deleteDatabase("rezim_local_v1.db")
        app.getSharedPreferences("hmyzak_prefs",0).edit().clear().commit()
        app.getSharedPreferences("hmyzak_rest_timer",0).edit().clear().commit()
        AppLanguage.choose(app,"en")
    }
    @After fun after() {AppLanguage.choose(app,"");app.deleteDatabase("rezim_local_v1.db")}
    @Test fun wakeOptionDefaultsOffAndSurvivesModelRestartJsonAndOldBackups() {
        val initial=LocalStore(app);try {initial.completeSetup()} finally {initial.close()}
        val vm=RezimState(app)
        assertFalse(vm.restWakeScreenEnabled)
        vm.setRestWakeScreen(true)
        assertTrue(RezimState(app).restWakeScreenEnabled)
        val backup=vm.exportBackup()
        assertEquals(11,JSONObject(backup).getInt("schemaVersion"))
        assertTrue(JSONObject(backup).getJSONObject("settings").getBoolean("rest_wake_screen"))
        vm.setRestWakeScreen(false);vm.importBackup(backup)
        assertTrue(vm.restWakeScreenEnabled)
        val older=JSONObject(backup);older.getJSONObject("settings").remove("rest_wake_screen")
        vm.setRestWakeScreen(false);vm.importBackup(older.toString())
        assertFalse(vm.restWakeScreenEnabled);assertFalse(vm.onboardingRequired)
        assertThrows(IllegalArgumentException::class.java) {
            BackupPreferences.validate(JSONObject().put("rest_wake_screen","true"))
        }
    }
    @Test fun allTenLocalesAndWakePreferenceRoundTripWithoutAnyUserDataChanges() {
        val store=LocalStore(app).withLegacyTestPlans();try {
            val ex=store.plans().first().exercises.first()
            store.updateExerciseDetails("mon",ex.key,"İpek – můj cvik","Żółć · minha nota")
            store.setSuperset("mon",ex.key,listOf(ex.key,store.plans().first().exercises[1].key))
            store.saveBodyMetric("Levý biceps İı")
            store.addMeasure("2026-10-24",80.5,null)
            val before=store.exportJson()
            assertEquals(10,AppLanguage.supported.size)
            for(tag in AppLanguage.supported) {
                AppLanguage.choose(app,tag)
                val wrapped=AppLanguage.wrap(app)
                assertEquals(tag,wrapped.resources.configuration.locales[0].toLanguageTag())
                assertEquals(tag,BackupPreferences.export(app).getString("language"))
                val prefs=BackupPreferences.validate(BackupPreferences.export(app).put("rest_wake_screen",true))
                AppLanguage.choose(app,"en");BackupPreferences.restore(app,prefs)
                assertEquals(tag,AppLanguage.selected(app))
                assertTrue(app.getSharedPreferences("hmyzak_prefs",0).getBoolean("rest_wake_screen",false))
                assertEquals(before,store.exportJson())
                assertFalse(store.needsOnboarding())
            }
        } finally {store.close()}
    }
    @Test fun brazilianRegionSystemFallbackAndTurkishCaseAreExplicit() {
        assertEquals("pt-BR",AppLanguage.supportedLocale(Locale.forLanguageTag("pt-BR"))!!.toLanguageTag())
        assertEquals("pt-BR",AppLanguage.supportedLocale(Locale.forLanguageTag("pt"))!!.toLanguageTag())
        assertNull(AppLanguage.supportedLocale(Locale.forLanguageTag("pt-PT")))
        assertNull(AppLanguage.supportedLocale(Locale.JAPANESE))
        assertEquals("es",AppLanguage.supportedLocale(Locale.forLanguageTag("es-MX"))!!.toLanguageTag())
        AppLanguage.choose(app,"tr")
        assertEquals("İSTANBUL", "istanbul".uppercase(AppLanguage.locale(app)))
        val store=LocalStore(app);try {
            store.completeSetup();val day=store.addDay("istanbul")
            assertEquals("İST",store.plans().single {it.key==day}.short)
        } finally {store.close()}
        assertEquals("ii\u0307",bodyMetricNameKey("Iİ")) // Identifiers use locale-independent normalization.
    }
    @Test fun decimalInputAndFormattedMessagesWorkOfflineForEveryLocale() {
        for(tag in AppLanguage.supported) {
            AppLanguage.choose(app,tag)
            val localized=AppLanguage.wrap(app)
            assertEquals(80.5,"80,5".asNumber()!!,0.0)
            assertEquals(80.5,"80.5".asNumber()!!,0.0)
            assertEquals(80.5,parseBodyMeasurement("80,5","",emptyMap())!!.kg!!,0.0)
            val separator=java.text.DecimalFormatSymbols.getInstance(Locale.forLanguageTag(tag)).decimalSeparator
            assertTrue(fmtDisplay(80.5).contains(separator))
            assertFalse(localized.tr(R.string.workout_set_value,1,"20,5 kg","8").contains("%"))
            assertFalse(localized.tr(R.string.workout_duration_value,2,5).contains("%"))
            assertFalse(localized.tr(R.string.pro_buy_price,"8,99 €").contains("%"))
            for(n in listOf(1,2,5,21)) for(id in listOf(R.plurals.period_months,R.plurals.period_days,R.plurals.set_count))
                assertTrue(localized.quantity(id,n).contains(n.toString()))
        }
    }
    @Test fun newLocalesHaveActualTranslationsAndCorrectQuantityForms() {
        for((tag,welcome) in mapOf("es" to "Te damos la bienvenida a LiftAnt","pt-BR" to "Boas-vindas ao LiftAnt",
            "pl" to "Witaj w LiftAnt","tr" to "LiftAnt’a hoş geldin")) {
            AppLanguage.choose(app,tag);val localized=AppLanguage.wrap(app)
            assertEquals(welcome,localized.tr(R.string.setup_welcome))
            assertNotEquals("Body measurements",localized.tr(R.string.ui_telesne_miry_8809a))
            assertNotEquals("Rest timer",localized.tr(R.string.ui_casovac_pauz_7cfc6))
        }
        AppLanguage.choose(app,"pl");var context=AppLanguage.wrap(app)
        assertEquals("1 seria",context.quantity(R.plurals.set_count,1))
        assertEquals("2 serie",context.quantity(R.plurals.set_count,2))
        assertEquals("5 serii",context.quantity(R.plurals.set_count,5))
        AppLanguage.choose(app,"pt-BR");context=AppLanguage.wrap(app)
        assertEquals("1 mês",context.quantity(R.plurals.period_months,1))
        assertEquals("3 meses",context.quantity(R.plurals.period_months,3))
    }
}
