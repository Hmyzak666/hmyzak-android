package cz.rezim.app
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
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
class GpsDiscardUiTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val isolated:org.junit.rules.TestRule=org.junit.rules.RuleChain.outerRule(object:org.junit.rules.ExternalResource() {
        override fun before() {
            val app=RuntimeEnvironment.getApplication()
            app.deleteDatabase("rezim_local_v1.db")
            app.getSharedPreferences("hmyzak_prefs",0).edit().clear().commit()
            LocalStore(app).use {it.completeSetup();it.saveGps(GpsTrack("empty","RUNNING","2026-10-29"))}
        }
    }).around(compose)
    @Test fun zeroDurationRecordCanBeKeptOrDiscardedWithConfirmation() {
        compose.runOnUiThread {compose.activity.setContent {HmyzakTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) {GpsCard(ViewModelProvider(compose.activity)[RezimState::class.java])}
        }}}
        compose.onNodeWithTag("gps-finish").assertIsNotEnabled()
        compose.onNodeWithTag("gps-discard").performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.tr(R.string.gps_keep)).performClick()
        compose.runOnIdle {LocalStore(compose.activity).use {assertEquals(1,it.gpsTracks().size)}}
        compose.onNodeWithTag("gps-discard").performScrollTo().performClick()
        compose.onNodeWithTag("gps-discard-confirm").performClick()
        compose.onNodeWithTag("gps-start").assertExists()
        compose.runOnIdle {LocalStore(compose.activity).use {assertTrue(it.gpsTracks().isEmpty())}}
    }
}
