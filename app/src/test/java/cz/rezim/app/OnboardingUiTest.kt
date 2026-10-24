package cz.rezim.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
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
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun vm()=ViewModelProvider(compose.activity)[RezimState::class.java]
    @Test fun phoneEmptyStartReachesUsableEmptyPlanScreenAndFreeLogging() {
        compose.onNodeWithText("Welcome to LiftAnt").assertIsDisplayed()
        compose.onNodeWithText("Your training. Your progress.").assertIsDisplayed()
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
