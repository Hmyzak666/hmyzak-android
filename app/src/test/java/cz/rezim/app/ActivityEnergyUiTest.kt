package cz.rezim.app
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.*
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="en-rUS-w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ActivityEnergyUiTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val isolated:org.junit.rules.TestRule=org.junit.rules.RuleChain.outerRule(object:org.junit.rules.ExternalResource() {
        override fun before() {
            val app=RuntimeEnvironment.getApplication()
            app.deleteDatabase("rezim_local_v1.db")
            app.getSharedPreferences("hmyzak_prefs",0).edit().clear().commit()
            LocalStore(app).use {it.completeSetup()}
        }
    }).around(compose)
    private fun vm()=ViewModelProvider(compose.activity)[RezimState::class.java]
    private fun prepare() {
        compose.runOnIdle {
            assertTrue(vm().addMeasure("2026-01-02",BodyMeasurementInput(100.0,null)))
            vm().addHiit(HiitEntry(0,"2026-01-01",2,1,90,0,null,null,"",totalSeconds=90))
        }
        compose.runOnUiThread {compose.activity.setContent {HmyzakTheme {ActivityEnergy(vm(),vm().hiit.first())}}}
    }
    @Test fun exactDurationAndSavedIntensityMatchOverviewAndRefreshImmediately() {
        prepare()
        val record=vm().hiit.first()
        val key="hiit:${record.id}"
        val originalRecords=vm().hiit
        compose.runOnIdle {
            assertTrue(vm().addMeasure("2025-12-31",BodyMeasurementInput(80.0,null)))
            assertTrue(vm().measures.any {it.date=="2025-12-31" && it.kg==80.0})
            assertTrue(vm().measures.any {it.date=="2026-01-02" && it.kg==100.0})
            val row=energyRows(vm(),compose.activity).single {it.key==key}
            assertEquals(key,row.key);assertEquals(90,row.seconds)
            val estimate=energyFor(row,vm(),"")
            assertNotNull("row=$row measures=${vm().measures}",estimate)
            assertEquals(80.0,estimate!!.weightKg,0.0)
            vm().setEnergyIntensity(key,"MODERATE")
        }
        val label=compose.activity.tr(R.string.energy_active)
        compose.onNodeWithText("$label: 12 kcal").assertIsDisplayed()
        compose.runOnIdle {vm().setEnergyIntensity(key,"VIGOROUS")}
        compose.onNodeWithText("$label: 20 kcal").assertIsDisplayed()
        compose.runOnIdle {assertEquals(originalRecords,vm().hiit);assertEquals(90,vm().hiit.first().totalSeconds)}
    }
    @Test fun missingHistoricalWeightDoesNotInventCalories() {
        prepare()
        compose.onNodeWithText(compose.activity.tr(R.string.energy_missing)).assertIsDisplayed()
        compose.onNodeWithText("kcal",substring=true).assertDoesNotExist()
    }
}
