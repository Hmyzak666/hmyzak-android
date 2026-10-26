package cz.rezim.app

import android.app.Application
import androidx.activity.compose.setContent
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
class HistoryAndProUiTest {
    val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val isolatedActivity:org.junit.rules.TestRule=org.junit.rules.RuleChain.outerRule(
        object:org.junit.rules.ExternalResource() {
            override fun before() {
                val app=RuntimeEnvironment.getApplication()
                app.deleteDatabase("rezim_local_v1.db")
                app.getSharedPreferences("hmyzak_prefs",0).edit().clear().commit()
                LocalStore(app).use {it.completeSetup()}
            }
        }
    ).around(compose)
    @Before fun currentModel() {
        compose.runOnUiThread {
            compose.activity.viewModelStore.clear()
            val model=ViewModelProvider(compose.activity,ViewModelProvider.AndroidViewModelFactory(compose.activity.application))[RezimState::class.java]
            model.setProForTest(false)
            compose.activity.setContent {HmyzakTheme {AppRoot(model)}}
        }
    }
    private fun vm()=ViewModelProvider(compose.activity)[RezimState::class.java]
    private fun seed(count:Int=20):List<Long> {
        var ids=emptyList<Long>()
        compose.runOnIdle {
            LocalStore(compose.activity.application).use {store ->
                val day=store.addDay("My custom press day");store.addExercise(day,"My custom press",2,UnitType.KG)
                ids=(1..count).map {n ->
                    val session=store.beginWorkout(day)
                    store.updateSet(store.sets(session.id).first().copy(weight=(10+n).toString(),reps="8",done=true))
                    store.finishWorkout(session.id)
                    val end=System.currentTimeMillis()-(count-n)*86400000L
                    store.writableDatabase.execSQL("UPDATE sessions SET created=?,finished=?,duration_seconds=125 WHERE id=?",arrayOf(end-125000,end,session.id))
                    session.id
                }
                vm().refresh();vm().selectDay(day);vm().clearMessage();vm().page=Page.HOME
            }
        }
        return ids
    }
    private fun preview(name:String) {
        compose.runOnIdle {
            val view=compose.activity.window.decorView
            val bitmap=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            val file=File("build/reports/liftant-history-previews/$name.png");file.parentFile!!.mkdirs()
            file.outputStream().use {bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        }
    }
    @Test fun homeShowsOnlyThreeAndOpeningDetailIsReadOnlyUntilExplicitEdit() {
        val ids=seed()
        var backup="";compose.runOnIdle {backup=vm().exportBackup()}
        compose.onNodeWithTag("home-screen").performScrollToNode(hasTestTag("all-workout-history"))
        ids.takeLast(3).forEach {compose.onNodeWithTag("workout-row:$it").assertExists()}
        compose.onNodeWithTag("workout-row:${ids[16]}").assertDoesNotExist()
        compose.onNodeWithTag("workout-row:${ids.last()}").performScrollTo().performClick()
        compose.onNodeWithTag("workout-detail").assertIsDisplayed()
        compose.onNodeWithText("2 min 05 s").assertIsDisplayed()
        compose.onNodeWithText("My custom press",substring=false).assertIsDisplayed()
        compose.onNodeWithText("30 kg × 8 reps",substring=true).assertIsDisplayed()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.runOnIdle {assertNull(vm().editingSavedWorkout);assertEquals(backup,vm().exportBackup())}
        preview("phone-read-only-detail")
        compose.onNodeWithTag("edit-saved-workout").performClick()
        compose.runOnIdle {assertNotNull(vm().editingSavedWorkout)}
        compose.onNodeWithText("Cancel without saving").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(Page.WORKOUT_DETAIL,vm().page);assertEquals(backup,vm().exportBackup())}
    }
    @Test fun fullHistoryCanOpenOlderThanFifteenAndBackReturnsToHistoryThenHome() {
        val ids=seed()
        compose.onNodeWithTag("home-screen").performScrollToNode(hasTestTag("all-workout-history"))
        compose.onNodeWithTag("all-workout-history").performClick()
        compose.onNodeWithTag("completed-history").performScrollToNode(hasTestTag("workout-row:${ids.first()}"))
        compose.onNodeWithTag("workout-row:${ids.first()}").performClick()
        compose.onNodeWithText("11 kg × 8 reps",substring=true).assertIsDisplayed()
        compose.runOnIdle {assertEquals(20,vm().completedWorkouts.size);compose.activity.onBackPressedDispatcher.onBackPressed()}
        compose.onNodeWithTag("completed-history").assertIsDisplayed()
        compose.runOnIdle {compose.activity.onBackPressedDispatcher.onBackPressed();assertEquals(Page.HOME,vm().page)}
    }
    @Test fun savingWithoutEditsKeepsOriginalSecondsAndAllSetValues() {
        val ids=seed(1);var backup=""
        compose.runOnIdle {backup=vm().exportBackup();vm().openWorkout(ids.single())}
        compose.onNodeWithTag("edit-saved-workout").performClick()
        compose.onNodeWithText("Save changes").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(Page.WORKOUT_DETAIL,vm().page);assertEquals(backup,vm().exportBackup())}
        compose.onNodeWithText("2 min 05 s").assertIsDisplayed()
    }
    @Test fun freeRecordsAndAlertStayVisibleWhileProgressAndImageCardsAreLocked() {
        seed(2);compose.runOnIdle {vm().page=Page.STRENGTH}
        compose.onNodeWithTag("strength-workout").performScrollToNode(hasTestTag("personal-records"))
        compose.onNodeWithTag("new-personal-record").assertExists()
        compose.onNodeWithTag("strength-workout").performScrollToNode(hasTestTag("pro-locked:exercise-progress"))
        compose.onNodeWithTag("personal-records").assertExists()
        compose.onNodeWithTag("analytics-chart").assertDoesNotExist()
        compose.onNodeWithText("Share performance card").assertDoesNotExist()
        preview("phone-free-records-and-pro")
        compose.onNodeWithText("Unlock Pro · one-time purchase").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(Page.SETTINGS,vm().page)}
        compose.onNodeWithText("Restore purchase").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Back",substring=false).performClick()
        compose.runOnIdle {assertEquals(Page.STRENGTH,vm().page)}
    }
    @Test fun entitlementChangesImmediatelyHideChartsAndKeepFreeRecords() {
        seed(2);compose.runOnIdle {vm().page=Page.STRENGTH;vm().setProForTest(true)}
        compose.onNodeWithTag("strength-workout").performScrollToNode(hasTestTag("analytics-chart"))
        compose.onNodeWithTag("analytics-chart").assertIsDisplayed()
        compose.onNodeWithText("Share performance card").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {vm().setProForTest(false)}
        compose.onNodeWithTag("analytics-chart").assertDoesNotExist()
        compose.onNodeWithTag("strength-workout").performScrollToNode(hasTestTag("new-personal-record"))
        compose.onNodeWithTag("new-personal-record").assertIsDisplayed()
    }
    @Test fun finishingFreeWorkoutShowsNewRecordWithoutUnlockingImageSharing() {
        seed(2)
        compose.runOnIdle {
            vm().start();vm().page=Page.STRENGTH
            vm().changeSet(vm().workoutSets.first(),weight="13",reps="8")
            vm().toggle(vm().workoutSets.first(),UnitType.KG)
        }
        compose.onNodeWithTag("strength-workout").performScrollToNode(hasTestTag("finish-workout-bottom"))
        compose.onNodeWithTag("finish-workout-bottom").performClick()
        compose.onAllNodesWithText("New personal record · My custom press").onFirst().assertIsDisplayed()
        compose.onNodeWithTag("pro-locked:workout-sharing").assertExists()
        compose.onNodeWithText("Share image · story").assertDoesNotExist()
        compose.runOnIdle {assertFalse(vm().billing.hasPro);assertEquals(3,vm().totalWorkouts)}
    }
    @Test fun allOtherTrendChartsUseSameProEntitlementAndDoNotHideRecordedData() {
        compose.runOnIdle {
            LocalStore(compose.activity.application).use {store ->
                repeat(2) {n ->
                    store.addRun("2026-10-${20+n}",5.0,1800+n,140+n,160,"")
                    store.addHiit(HiitEntry(0,"2026-10-${20+n}",20,8,30,90,140+n,160,""))
                    store.addMeasure("2026-10-${20+n}",80.0+n,null)
                }
            }
            vm().refresh()
        }
        for((page,lock) in listOf(Page.RUN to "running-pace",Page.HIIT to "hiit-heart-rate",Page.BODY to "body-progress")) {
            compose.runOnIdle {vm().setProForTest(false);vm().page=page}
            compose.onNodeWithTag("pro-locked:$lock").performScrollTo().assertIsDisplayed()
            compose.onAllNodesWithTag("analytics-chart").assertCountEquals(0)
            compose.runOnIdle {vm().setProForTest(true)}
            compose.onAllNodesWithTag("analytics-chart").onFirst().performScrollTo().assertIsDisplayed()
        }
        compose.runOnIdle {assertEquals(2,vm().runs.size);assertEquals(2,vm().hiit.size);assertEquals(2,vm().measures.size)}
    }
    @Test @Config(qualifiers="en-rUS-w800dp-h1280dp")
    fun tabletHistoryAndDetailRemainUsableWithoutPro() {
        val ids=seed()
        compose.onNodeWithTag("home-screen").performScrollToNode(hasTestTag("all-workout-history"))
        preview("tablet-last-three")
        compose.onNodeWithTag("all-workout-history").performClick()
        compose.onNodeWithTag("completed-history").performScrollToNode(hasTestTag("workout-row:${ids.first()}"))
        compose.onNodeWithTag("workout-row:${ids.first()}").performClick()
        compose.onNodeWithTag("edit-saved-workout").assertIsDisplayed()
        compose.onNodeWithText("11 kg × 8 reps",substring=true).assertIsDisplayed()
        preview("tablet-read-only-detail")
    }
    @Test fun quickStartSelectsArbitraryUserDayAndPreservesAnotherOpenWorkout() {
        var chosen="";var other=""
        compose.runOnIdle {
            vm().createDay("Custom α");vm().addExercise("Own row",1,UnitType.KG);other=vm().selectedDay
            vm().createDay("Custom β");vm().addExercise("Own press",1,UnitType.KG);chosen=vm().selectedDay
            vm().saveShortcuts(listOf(QuickShortcut(id="chosen",kind="STRENGTH",target=chosen)))
            vm().page=Page.HOME;vm().clearMessage()
        }
        compose.mainClock.advanceTimeBy(6000);compose.waitForIdle()
        compose.onNodeWithTag("home-screen").performScrollToNode(hasTestTag("quick-start-section"))
        compose.onNodeWithTag("quick-start-section").performScrollTo()
        compose.onNodeWithTag("shortcut:chosen").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(chosen,vm().selectedDay);assertEquals(Page.STRENGTH,vm().page);vm().selectDay(other);vm().start();vm().page=Page.HOME;vm().clearMessage()}
        compose.mainClock.advanceTimeBy(6000);compose.waitForIdle()
        compose.onNodeWithTag("home-screen").performScrollToNode(hasTestTag("quick-start-section"))
        compose.onNodeWithTag("quick-start-section").performScrollTo()
        compose.onNodeWithTag("shortcut:chosen").performScrollTo().assertIsDisplayed()
            .performClick()
        compose.runOnIdle {assertEquals(other,vm().selectedDay);assertEquals(other,vm().session!!.day);assertEquals(Page.STRENGTH,vm().page)}
    }
}
