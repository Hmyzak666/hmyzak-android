package cz.rezim.app
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
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
class ProfileEnergyUiTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val isolated:org.junit.rules.TestRule=org.junit.rules.RuleChain.outerRule(object:org.junit.rules.ExternalResource() {
        override fun before() {val app=RuntimeEnvironment.getApplication();app.deleteDatabase("rezim_local_v1.db");app.getSharedPreferences("hmyzak_prefs",0).edit().clear().commit();LocalStore(app).use {it.completeSetup()}}
    }).around(compose)
    private fun vm()=ViewModelProvider(compose.activity)[RezimState::class.java]
    @Test fun profileSavesAndSurvivesRecreation() {
        compose.runOnIdle {vm().page=Page.SETTINGS}
        compose.onNodeWithTag("profile-year").performScrollTo().performTextReplacement("1993")
        compose.onNodeWithTag("save-profile").performScrollTo().performClick()
        compose.activityRule.scenario.recreate()
        compose.runOnIdle {assertEquals(1993,vm().profile.birthYear)}
    }
    @Test fun numbersSelectAllOnEntryAndStillAllowPartialCorrection() {
        val weight=androidx.compose.runtime.mutableStateOf("25,5")
        compose.runOnUiThread {compose.activity.setContent {HmyzakTheme {CompactCell(weight.value,{weight.value=it},65,true)}}}
        val field=compose.onNode(hasSetTextAction())
        field.performClick()
        field.assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.TextSelectionRange,androidx.compose.ui.text.TextRange(0,4)))
        field.performTextInput("30,5")
        compose.runOnIdle {assertEquals("30,5",weight.value)}
        field.performTextInputSelection(androidx.compose.ui.text.TextRange(0,2))
        field.performTextInput("32")
        compose.runOnIdle {assertEquals("32,5",weight.value)}
    }
    @Test fun tenLanguagesRenderEnergyAndGpsOnPhone()=languages()
    @Test @Config(qualifiers="en-rUS-w800dp-h1280dp") fun tenLanguagesRenderEnergyAndGpsOnTablet()=languages()
    private fun languages() {
        for(tag in listOf("en","cs","de","it","fr","ru","es","pt-BR","pl","tr")) {
            compose.runOnUiThread {AppLanguage.choose(compose.activity,tag)}
            val c=AppLanguage.wrap(compose.activity)
            compose.runOnUiThread {compose.activity.setContent {CompositionLocalProvider(LocalContext provides c,androidx.activity.compose.LocalActivityResultRegistryOwner provides compose.activity) {HmyzakTheme {EnergyScreen(vm())}}}}
            compose.onNodeWithTag("energy-screen").assertIsDisplayed()
            compose.runOnUiThread {compose.activity.setContent {CompositionLocalProvider(LocalContext provides c,androidx.activity.compose.LocalActivityResultRegistryOwner provides compose.activity) {HmyzakTheme {GpsCard(vm())}}}}
            compose.onNodeWithText(c.tr(R.string.gps_title)).performClick()
            compose.onNodeWithTag("gps-start").assertIsDisplayed()
            compose.runOnIdle {assertNull(GpsService.activeId);assertTrue(vm().cardio.isEmpty())}
        }
    }
}
