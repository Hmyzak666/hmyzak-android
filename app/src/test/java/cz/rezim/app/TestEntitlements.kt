package cz.rezim.app

import androidx.compose.runtime.MutableState

// UI tests control the observed entitlement only. This helper is never in the APK;
// real purchase/signature/acknowledgement flows are covered by ProPurchaseStateTest.
// Keep the production setter private rather than adding an application unlock API.
internal fun RezimState.setProForTest(enabled:Boolean) {
    val state=ProBilling::class.java.getDeclaredField("hasPro\$delegate").apply {isAccessible=true}
    @Suppress("UNCHECKED_CAST")
    (state.get(billing) as MutableState<Boolean>).value=enabled
}
