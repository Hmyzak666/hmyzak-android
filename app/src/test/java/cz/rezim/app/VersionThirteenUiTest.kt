package cz.rezim.app

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.SQLiteMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="en-rUS-w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class VersionThirteenUiTest {
    val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val isolatedActivity:org.junit.rules.TestRule=org.junit.rules.RuleChain.outerRule(
        object:org.junit.rules.ExternalResource() {
            override fun before() {
                val app=RuntimeEnvironment.getApplication()
                app.deleteDatabase("rezim_local_v1.db")
                app.getSharedPreferences("hmyzak_prefs",0).edit().clear().commit()
                app.getSharedPreferences("hmyzak_rest_timer",0).edit().clear().commit()
                LocalStore(app).use {it.completeSetup()}
            }
        }).around(compose)
    @Before fun currentModel() {
        compose.runOnUiThread {
            compose.activity.viewModelStore.clear()
            val model=ViewModelProvider(compose.activity,ViewModelProvider.AndroidViewModelFactory(compose.activity.application))[RezimState::class.java]
            model.setProForTest(false)
            compose.activity.renderApp()
        }
    }
    private fun vm()=ViewModelProvider(compose.activity)[RezimState::class.java]
    private fun preview(name:String) {
        compose.runOnIdle {
            val view=compose.activity.window.decorView
            val bitmap=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            val file=File("build/reports/liftant-v013-previews/$name.png");file.parentFile!!.mkdirs()
            file.outputStream().use {bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        }
    }
    @Test fun homeShortcutOpensExistingFormAndSavesWeightAloneWithoutChangingCustomMetrics() {
        compose.runOnIdle {vm().saveBodyMetric("My left biceps");vm().clearMessage()}
        compose.mainClock.advanceTimeBy(6000);compose.waitForIdle()
        compose.onNodeWithTag("home-screen").performScrollToNode(hasTestTag("quick-start-section"))
        compose.onNodeWithTag("quick-start-section").performScrollTo()
        compose.onNodeWithTag("quick-measurement").performClick()
        compose.onNodeWithTag("measurement-form").assertIsDisplayed()
        compose.onNodeWithText("Weight (kg, optional)").performTextInput("80,5")
        compose.onNodeWithText("My left biceps (cm)").assertExists()
        preview("phone-measurement-form")
        compose.onNodeWithText("Save measurement").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(Page.BODY,vm().page);assertEquals(80.5,vm().measures.single().kg!!,0.0)
            assertNull(vm().measures.single().waist);assertTrue(vm().measures.single().extra.isEmpty())
            assertEquals("My left biceps",vm().bodyMetrics.single().name);assertFalse(vm().billing.hasPro)
        }
    }
    @Test fun measurementDraftSurvivesActivityRecreationAndExistingValidationRejectsInvalidValues() {
        compose.runOnIdle {vm().startBodyForm()}
        compose.onNodeWithText("Weight (kg, optional)").performTextInput("401")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("401").assertExists()
        compose.onNodeWithText("Save measurement").performScrollTo().performClick()
        compose.runOnIdle {assertTrue(vm().measures.isEmpty())}
        compose.onNodeWithText("401").performTextReplacement("81.2")
        compose.onNodeWithText("Save measurement").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(81.2,vm().measures.single().kg!!,0.0)}
    }
    @Test fun wakeSettingCanBeToggledAndPersistsAfterActivityRestart() {
        compose.runOnIdle {vm().page=Page.SETTINGS}
        compose.onNodeWithTag("rest-wake-screen").performScrollTo().assertIsOff().performClick()
        compose.runOnIdle {assertTrue(vm().restWakeScreenEnabled)}
        compose.onNodeWithText("Rest-end notification settings").assertExists()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("rest-wake-screen").performScrollTo().assertIsOn().performClick()
        compose.runOnIdle {assertFalse(vm().restWakeScreenEnabled)}
    }
    @Test @Config(qualifiers="en-rUS-w320dp-h700dp")
    fun narrowPhoneQuickActionsAndMeasurementWorkInEveryLanguage() {checkLanguages("phone")}

    @Test @Config(qualifiers="en-rUS-w800dp-h1280dp")
    fun tabletQuickActionsAndMeasurementWorkInEveryLanguage() {checkLanguages("tablet")}

    private fun checkLanguages(size:String) {
        val app=compose.activity.application
        compose.runOnIdle {vm().createDay("My own İpek plan");vm().addExercise("My own exercise",1,UnitType.KG);vm().clearMessage()}
        var before="";compose.runOnIdle {LocalStore(app).use {before=it.exportJson()}}
        for(tag in AppLanguage.supported) {
            compose.runOnUiThread {
                AppLanguage.choose(app,tag)
                val localized=AppLanguage.wrap(compose.activity)
                compose.activity.setContent {
                    CompositionLocalProvider(LocalContext provides localized, androidx.activity.compose.LocalActivityResultRegistryOwner provides compose.activity) {HmyzakTheme {AppRoot(vm())}}
                }
                vm().page=Page.HOME
            }
            compose.onNodeWithTag("home-screen").performScrollToNode(hasTestTag("quick-start-section"))
            compose.onNodeWithTag("quick-start-section").performScrollTo()
            compose.onNodeWithTag("quick-measurement").assertIsDisplayed()
            preview("$size-$tag-quick-start")
            compose.onNodeWithTag("quick-measurement").performClick()
            compose.onNodeWithTag("measurement-form").assertIsDisplayed()
            val localized=AppLanguage.wrap(app)
            compose.onNodeWithText(localized.tr(R.string.ui_hmotnost_kg_volitelne_9b066)).assertExists()
            compose.runOnIdle {LocalStore(app).use {assertEquals(before,it.exportJson())};assertFalse(vm().billing.hasPro)}
        }
        compose.runOnUiThread {AppLanguage.choose(app,"")}
    }
}
