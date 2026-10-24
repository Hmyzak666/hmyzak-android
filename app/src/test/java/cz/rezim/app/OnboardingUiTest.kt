package cz.rezim.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.activity.compose.setContent
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="en-rUS-w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class OnboardingUiTest {
    val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val isolatedActivity:org.junit.rules.TestRule=org.junit.rules.RuleChain.outerRule(
        object:org.junit.rules.ExternalResource() {
            override fun before() {
                val app=org.robolectric.RuntimeEnvironment.getApplication()
                app.deleteDatabase("rezim_local_v1.db")
                app.getSharedPreferences("hmyzak_prefs",0).edit().clear().commit()
            }
        }
    ).around(compose)
    @org.junit.Before fun currentApplicationFactory() {
        // AndroidViewModelFactory's singleton survives Robolectric application resets.
        // Use this test's application explicitly, including after earlier language/tablet tests.
        compose.runOnUiThread {
            compose.activity.viewModelStore.clear()
            val model=ViewModelProvider(compose.activity,ViewModelProvider.AndroidViewModelFactory(compose.activity.application))[RezimState::class.java]
            compose.activity.setContent {HmyzakTheme {AppRoot(model)}}
        }
    }
    private fun vm()=ViewModelProvider(compose.activity)[RezimState::class.java]
    private fun preview(name:String) {
        compose.runOnIdle {
            val view=compose.activity.window.decorView
            val bitmap=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            val file=File("build/reports/liftant-public-previews/$name.png");file.parentFile!!.mkdirs()
            file.outputStream().use {bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
    }
    @Test fun phoneEmptyStartReachesUsableEmptyPlanScreenAndFreeLogging() {
        compose.onNodeWithText("Welcome to LiftAnt").assertIsDisplayed()
        compose.onNodeWithText("Your training. Your progress.").assertIsDisplayed()
        preview("phone-welcome")
        compose.onNodeWithTag("start-empty").performScrollTo().performClick()
        compose.onNodeWithTag("onboarding").assertDoesNotExist()
        compose.runOnIdle {assertTrue(vm().days.isEmpty());vm().page=Page.STRENGTH}
        compose.onNodeWithTag("empty-plans").assertIsDisplayed()
        compose.runOnIdle {
            vm().createDay("Own day");vm().addExercise("Own exercise",2,UnitType.KG);vm().start()
            assertNotNull(vm().session)
        }
        compose.onNodeWithTag("strength-workout").assertIsDisplayed()
    }
    @Test @Config(qualifiers="en-rUS-w800dp-h1280dp")
    fun tabletTemplateChoiceCreatesPlanAndRestartSkipsWelcome() {
        preview("tablet-welcome")
        compose.onNodeWithTag("choose-template").performScrollTo().performClick()
        compose.onNodeWithTag("template-upper_lower").performScrollTo().performClick()
        compose.onNodeWithTag("onboarding").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(4,vm().days.size);assertFalse(vm().onboardingRequired)
            val backup=vm().exportBackup();vm().importBackup(backup)
            assertEquals(4,vm().days.size);assertFalse(vm().onboardingRequired)
        }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("onboarding").assertDoesNotExist()
    }
    @Test fun importingEmptyBackupCompletesSetupAndKeepsEmptyHiitGeneric() {
        compose.runOnIdle {
            val backup=vm().exportBackup();vm().importBackup(backup)
            assertFalse(vm().onboardingRequired);assertTrue(vm().days.isEmpty());vm().page=Page.HIIT
        }
        compose.onNodeWithTag("onboarding").assertDoesNotExist()
        compose.onNodeWithText("Intervals for your chosen activity").assertIsDisplayed()
        compose.onNodeWithText("No interval sessions yet. Log your next workout here.").assertIsDisplayed()
    }
}
