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
class CardioUiTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val isolated:org.junit.rules.TestRule=org.junit.rules.RuleChain.outerRule(object:org.junit.rules.ExternalResource() {
        override fun before() {
            val app=RuntimeEnvironment.getApplication();app.deleteDatabase("rezim_local_v1.db")
            app.getSharedPreferences("hmyzak_prefs",0).edit().clear().commit()
            LocalStore(app).use {it.completeSetup()}
        }
    }).around(compose)
    @Before fun currentModel() {
        compose.runOnUiThread {
            compose.activity.viewModelStore.clear()
            val model=ViewModelProvider(compose.activity,ViewModelProvider.AndroidViewModelFactory(compose.activity.application))[RezimState::class.java]
            model.setProForTest(false);compose.activity.renderApp()
        }
    }
    private fun vm()=ViewModelProvider(compose.activity)[RezimState::class.java]
    private fun preview(name:String) {compose.runOnIdle {
        val view=compose.activity.window.decorView
        val bitmap=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        val file=File("build/reports/cardio-previews/$name.png");file.parentFile!!.mkdirs()
        file.outputStream().use {bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }}
    @Test fun ellipticalWithoutDistanceSavesNinetySecondsAndRetainsDraftOnRecreation() {
        compose.runOnIdle {vm().setProForTest(false);vm().startCardioForm("ELLIPTICAL")}
        compose.onNodeWithTag("cardio-time:1").performScrollTo().performTextReplacement("01")
        compose.onNodeWithTag("cardio-time:2").performTextReplacement("30")
        compose.onNodeWithTag("cardio-time:2").assertTextContains("30")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("cardio-time:1").assertTextContains("01")
        compose.onNodeWithTag("cardio-time:2").assertTextContains("30")
        preview("phone-cardio")
        compose.onNodeWithTag("save-cardio").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(90,vm().cardio.single().seconds);assertEquals(0.0,vm().cardio.single().km,0.0)
            assertEquals("ELLIPTICAL",vm().cardio.single().activity);assertTrue(vm().runs.isEmpty());assertFalse(vm().billing.hasPro)
        }
    }
    @Test fun hiitShortcutOnlyPrefillsAndSavesExactSecondsWhenExplicitlyConfirmed() {
        compose.runOnIdle {vm().launchShortcut(QuickShortcut(kind="HIIT",rounds=5,work=60,rest=120))}
        compose.onNodeWithTag("hiit-work:0").performScrollTo().assertTextContains("01")
        compose.onNodeWithTag("hiit-rest:0").performScrollTo().assertTextContains("02")
        compose.runOnIdle {assertTrue(vm().hiit.isEmpty());assertNull(vm().session)}
        compose.onNodeWithTag("hiit-total:1").performScrollTo().performTextReplacement("01")
        compose.onNodeWithTag("hiit-total:2").performTextReplacement("30")
        compose.onNodeWithText("Save HIIT").performScrollTo().performClick()
        compose.runOnIdle {val r=vm().hiit.single();assertEquals(90,r.totalSeconds);assertEquals(60,r.work);assertEquals(120,r.rest)}
    }
    @Test @Config(qualifiers="en-rUS-w320dp-h700dp")
    fun narrowPhoneAndTenLanguagesRenderCardioAndQuickEditorWithoutChangingData()=languages("phone")
    @Test @Config(qualifiers="en-rUS-w800dp-h1280dp")
    fun tabletAndTenLanguagesRenderCardioAndQuickEditorWithoutChangingData()=languages("tablet")
    private fun languages(size:String) {
        val app=compose.activity.application
        for(tag in listOf("en","cs","de","it","fr","ru","es","pt-BR","pl","tr")) {
            compose.runOnUiThread {
                AppLanguage.choose(app,tag)
                val localized=AppLanguage.wrap(compose.activity)
                compose.activity.setContent {
                    CompositionLocalProvider(LocalContext provides localized,androidx.compose.ui.platform.LocalConfiguration provides localized.resources.configuration,androidx.activity.compose.LocalActivityResultRegistryOwner provides compose.activity) {HmyzakTheme {AppRoot(vm())}}
                }
                vm().startCardioForm("TREADMILL")
            }
            compose.onNodeWithTag("cardio-time:0").performScrollTo().assertIsDisplayed()
            preview("$size-$tag-cardio")
            compose.runOnIdle {assertTrue(vm().cardio.isEmpty());vm().page=Page.HOME}
            compose.onNodeWithTag("edit-shortcuts").performScrollTo().performClick()
            preview("$size-$tag-shortcuts")
            val localized=AppLanguage.wrap(app)
            compose.onNodeWithText(localized.tr(R.string.ui_zavrit_ae288)).performClick()
        }
        compose.runOnUiThread {AppLanguage.choose(app,"")}
    }
}
