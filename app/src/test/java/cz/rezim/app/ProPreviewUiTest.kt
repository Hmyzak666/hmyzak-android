package cz.rezim.app

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="en-rUS-w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProPreviewUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun model():RezimState {
        lateinit var model:RezimState
        compose.runOnUiThread {
            compose.activity.viewModelStore.clear()
            model=ViewModelProvider(compose.activity,ViewModelProvider.AndroidViewModelFactory(compose.activity.application))[RezimState::class.java]
            model.setProForTest(false)
        }
        return model
    }
    @Test fun freePreviewDoesNotExposeRealContentOrMutateBackupAndUpgradeIsExplicit() {
        val vm=model()
        var upgrades=0
        lateinit var before:String
        compose.runOnUiThread {
            before=vm.exportBackup()
            compose.activity.setContent {HmyzakTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ProFeatureContent(vm,{upgrades++},"exercise-progress") {Text("Private analytics")}
                }
            }}
        }
        compose.onNodeWithTag("pro-sample:exercise-progress").assertIsDisplayed()
        compose.onNodeWithText("Sample · illustrative data, not your records").assertIsDisplayed()
        compose.onNodeWithText("Private analytics").assertDoesNotExist()
        compose.runOnIdle {assertEquals(0,upgrades);assertEquals(before,vm.exportBackup())}
        compose.onNodeWithText(compose.activity.tr(R.string.pro_unlock_lifetime)).performScrollTo().performClick()
        compose.runOnIdle {assertEquals(1,upgrades);assertFalse(vm.billing.hasPro);vm.setProForTest(true)}
        compose.onNodeWithText("Private analytics").assertIsDisplayed()
        compose.onNodeWithTag("pro-sample:exercise-progress").assertDoesNotExist()
    }
    private fun gallery() {
        val vm=model()
        compose.runOnUiThread {compose.activity.setContent {HmyzakTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) {ProSettings(vm)}
        }}}
        for(kind in listOf("exercise-progress","body-progress","period-stats")) {
            compose.onNodeWithTag("pro-sample:$kind").performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithText(compose.activity.tr(R.string.pro_store_unavailable)).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText(compose.activity.tr(R.string.pro_lifetime)).performScrollTo().assertIsDisplayed()
        compose.runOnIdle {assertFalse(vm.billing.hasPro)}
    }
    @Test fun phoneShowsGalleryAndDoesNotOfferPurchaseWithoutPlayPrice()=gallery()
    @Test @Config(qualifiers="en-rUS-w800dp-h1280dp")
    fun tabletShowsGalleryAndDoesNotOfferPurchaseWithoutPlayPrice()=gallery()
}
