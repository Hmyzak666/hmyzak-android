package cz.rezim.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Before
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import android.os.SystemClock
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
// Custom dimensions break editable platform dialogs in Robolectric: issue #8460.
@Config(sdk=[34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class WorkoutToolsUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun fixedDisplayClock() {
        compose.activity.setContent {
            HmyzakTheme {
                CompositionLocalProvider(LocalWorkoutDisplayTime provides WorkoutDisplayTime(System.currentTimeMillis(),SystemClock.elapsedRealtime())) {
                    AppRoot()
                }
            }
        }
        // Clear the restored session through its owning ViewModel/SQLite connection.
        // Removing a database file cannot reset an already-created activity's state.
        compose.runOnIdle {
            vm().discardWorkout()
            LocalStore(compose.activity).withLegacyTestPlans().close()
            vm().refresh();vm().selectDay("mon");vm().clearMessage()
        }
    }
    private fun settle() {compose.waitForIdle()}
    private fun vm()=ViewModelProvider(compose.activity)[RezimState::class.java]
    private fun start() {compose.runOnIdle {vm().selectDay("tue");vm().start();vm().page=Page.STRENGTH};settle()}
    @Test @Config(qualifiers="cs-rCZ-w360dp-h800dp")
    fun settingsAreReachableAndContainExistingHapticControls() {
        compose.onNodeWithText("Nastavení",substring=false).performClick();settle()
        compose.onNodeWithText("Vibrace po odkliknutí série").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Vyzkoušet vibraci").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Nastavení vibrací").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Zpět").performClick();settle()
        compose.runOnIdle {assertEquals(Page.HOME,vm().page)}
    }
    @Test fun bottomSaveOpensTheSameSummaryAndBothShareFormats() {
        start()
        compose.runOnIdle {
            val model=vm()
            model.changeSet(model.workoutSets.first(),weight="10",reps="5")
            model.toggle(model.workoutSets.first(),UnitType.KG)
        }
        compose.onNodeWithTag("strength-workout").performScrollToNode(hasTestTag("finish-workout-bottom"));settle()
        compose.onNodeWithTag("finish-workout-bottom").performClick();settle()
        compose.runOnIdle {assertNull(vm().session)}
        compose.onNodeWithText("Trénink uložen",substring=false).assertIsDisplayed()
        compose.onNodeWithText("Nazvedáno 50 kg",substring=false).assertIsDisplayed()
        compose.onNodeWithText("Sdílet obrázek · příběh",substring=false).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Sdílet obrázek · čtverec",substring=false).performScrollTo().assertIsDisplayed()
    }
    @Test fun toolbarEditsLiveDurationAndRestLengthAtPhoneWidth() {
        start()
        compose.onNodeWithText("Pauza · nastavit").assertIsDisplayed().performClick();settle()
        compose.onNode(hasSetTextAction() and hasText("Vlastní délka · m:ss nebo sekundy"))
            .performScrollTo().performTextReplacement("0:45");settle()
        compose.onNodeWithText("Uložit délku").performClick();settle()
        compose.runOnIdle {assertEquals(45,vm().restSeconds)}
        compose.onNodeWithText("Hotovo").performClick();settle()
        compose.onNodeWithText("Trénink · upravit").assertIsDisplayed().performClick();settle()
        compose.onNode(hasSetTextAction() and hasText("Dosavadní délka v minutách")).performTextReplacement("12");settle()
        compose.onNodeWithText("Uložit",substring=false).performClick();settle()
        compose.runOnIdle {assertTrue(vm().session!!.elapsedSeconds() in 720..725)}
    }
    @Test fun specificRoundCanBeChosenAndSavedDuringWorkout() {
        val model=vm()
        var anchor:Exercise?=null;var shorter:Exercise?=null;var before=emptyList<SetEntry>()
        compose.runOnIdle {
            model.createDay("Round picker")
            model.addExercise("Bench test",4,UnitType.KG)
            model.addExercise("Push-ups test",1,UnitType.REPS)
            val ex=model.plan(model.selectedDay).exercises
            assertEquals("Round picker",model.plan(model.selectedDay).name)
            assertEquals(2,ex.size)
            anchor=ex[0];shorter=ex[1]
            model.updateSuperset(ex[0].key,ex.map {it.key});model.start()
            before=model.workoutSets.toList()
        }
        compose.activity.setContent {HmyzakTheme {SupersetDialog(model,anchor!!) {}}};settle()
        compose.onNodeWithTag("superset-round:${shorter!!.key}").performScrollTo().performClick();settle()
        compose.onNodeWithText("Poslední kola",substring=false).performClick();settle()
        compose.onNodeWithText("Kolo 4",substring=false).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("superset-round:${shorter!!.key}").performScrollTo().performClick();settle()
        compose.onNodeWithText("Od 3. kola",substring=false).performClick();settle()
        compose.onNodeWithText("Kolo 3",substring=false).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Uložit",substring=false).performClick();settle()
        compose.runOnIdle {
            assertEquals(3,model.plan(model.selectedDay).exercises[1].supersetStartRound)
            assertEquals(before,model.workoutSets.toList())
            model.discardWorkout()
        }
    }
    @Test fun permanentNoteAndThreeMemberSupersetCanBeEditedWithoutChangingSets() {
        start()
        var before=emptyList<SetEntry>()
        compose.runOnIdle {before=vm().workoutSets.toList()}
        compose.onNodeWithTag("strength-workout").performScrollToNode(hasText("Trvalá poznámka"));settle()
        compose.onAllNodesWithText("Trvalá poznámka").onFirst().performScrollTo().performClick();settle()
        compose.onNode(hasSetTextAction() and hasText("Poznámka ke cviku")).performTextReplacement("Lokty vysoko");settle()
        compose.onNodeWithText("Uložit",substring=false).performClick();settle()
        compose.runOnIdle {assertEquals("Lokty vysoko",vm().plan("tue").exercises.first().note)}
        // Let the confirmation snackbar leave the compact viewport before the next tap.
        compose.mainClock.advanceTimeBy(6000);settle()
        compose.onAllNodesWithText("Supersérie",substring=false).onFirst().performScrollTo().performClick();settle()
        compose.onNode(hasText("Romanian Deadlift") and isToggleable()).performScrollTo().performClick();settle()
        compose.onNode(hasText("Leg Curls") and isToggleable()).performScrollTo().performClick();settle()
        compose.onNodeWithText("Uložit",substring=false).performClick();settle()
        compose.runOnIdle {
            assertEquals(before,vm().workoutSets.toList())
            assertEquals(3,supersetMembers(vm().plan("tue").exercises,"front_squat").size)
        }
    }
}
