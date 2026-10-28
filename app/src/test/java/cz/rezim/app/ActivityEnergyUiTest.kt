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
    private fun prepare(weight:Boolean) {
        compose.runOnIdle {
            LocalStore(compose.activity).use {db->
                if(weight) db.addMeasure("2025-12-31",80.0,null)
                db.addMeasure("2026-01-02",100.0,null) // Must never apply a future weight.
                db.addHiit(HiitEntry(0,"2026-01-01",2,1,90,0,null,null,"",totalSeconds=90))
            }
            vm().refresh()
        }
        compose.runOnUiThread {compose.activity.setContent {HmyzakTheme {ActivityEnergy(vm(),vm().hiit.first())}}}
    }
    @Test fun exactDurationAndSavedIntensityMatchOverviewAndRefreshImmediately() {
        prepare(true)
        val record=vm().hiit.first()
        val key="hiit:${record.id}"
        compose.runOnIdle {
            val row=energyRows(vm(),compose.activity).single()
            assertEquals(key,row.key);assertEquals(90,row.seconds)
            assertEquals(80.0,energyFor(row,vm(),"")!!.weightKg,0.0)
            vm().setEnergyIntensity(key,"MODERATE")
        }
        val label=compose.activity.tr(R.string.energy_active)
        compose.onNodeWithText("$label: 12 kcal").assertIsDisplayed()
        compose.runOnIdle {vm().setEnergyIntensity(key,"VIGOROUS")}
        compose.onNodeWithText("$label: 20 kcal").assertIsDisplayed()
        compose.runOnIdle {assertEquals(1,vm().hiit.size);assertEquals(90,vm().hiit.first().totalSeconds)}
    }
    @Test fun missingHistoricalWeightDoesNotInventCalories() {
        prepare(false)
        compose.onNodeWithText(compose.activity.tr(R.string.energy_missing)).assertIsDisplayed()
        compose.onNodeWithText("kcal",substring=true).assertDoesNotExist()
    }
}
