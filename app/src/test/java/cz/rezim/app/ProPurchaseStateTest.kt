package cz.rezim.app

import android.os.Looper
import com.android.billingclient.api.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class ProPurchaseStateTest {
    private val context get()=RuntimeEnvironment.getApplication()
    private val pair=KeyPairGenerator.getInstance("RSA").apply {initialize(2048)}.generateKeyPair()
    private val key get()=Base64.getEncoder().encodeToString(pair.public.encoded)
    @Before fun clear() {context.getSharedPreferences("liftant_pro_purchase",0).edit().clear().commit()}
    private fun result(code:Int)=BillingResult.newBuilder().setResponseCode(code).build()
    private fun purchase(state:Int=0,ack:Boolean=true,product:String=PRO_PRODUCT_ID,packageName:String="cz.rezim.app"):Purchase {
        val raw=JSONObject().put("packageName",packageName).put("productId",product).put("purchaseToken","synthetic-test-token")
            .put("purchaseState",state).put("acknowledged",ack).toString()
        val sig=Signature.getInstance("SHA1withRSA").run {
            initSign(pair.private);update(raw.toByteArray());Base64.getEncoder().encodeToString(sign())
        }
        return Purchase(raw,sig)
    }
    private fun drain() {shadowOf(Looper.getMainLooper()).idle()}
    @Test fun releaseModeNeverUnlocksWithoutPurchaseAndRejectsCombinedPreviewPlay() {
        assertFalse(ProBilling(context,playBuild=false,testMode=false).hasPro)
        assertFalse(ProBilling(context,playBuild=true,testMode=false,publicKey=key).hasPro)
        assertTrue(ProBilling(context,playBuild=false,testMode=true).hasPro)
        assertThrows(IllegalArgumentException::class.java) {ProBilling(context,playBuild=true,testMode=true,publicKey=key)}
    }
    @Test fun verifiedOwnershipRestoresThenCachedSignatureSupportsOfflineAndSuccessfulEmptyQueryRevokes() {
        val billing=ProBilling(context,true,false,key)
        billing.handleOwnedPurchases(result(BillingClient.BillingResponseCode.OK),listOf(purchase()))
        assertTrue(billing.hasPro)
        val reopened=ProBilling(context,true,false,key);assertTrue(reopened.hasPro)
        reopened.handleOwnedPurchases(result(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE),emptyList());assertTrue(reopened.hasPro)
        reopened.handleOwnedPurchases(result(BillingClient.BillingResponseCode.OK),emptyList());assertFalse(reopened.hasPro)
        assertFalse(ProBilling(context,true,false,key).hasPro)
    }
    @Test fun pendingWrongPackageAndCancelledPurchasesCannotGrant() {
        val billing=ProBilling(context,true,false,key)
        assertEquals(Purchase.PurchaseState.PENDING,purchase(state=4).purchaseState)
        billing.onPurchasesUpdated(result(BillingClient.BillingResponseCode.OK),listOf(purchase(state=4)));drain();assertFalse(billing.hasPro)
        billing.onPurchasesUpdated(result(BillingClient.BillingResponseCode.USER_CANCELED),null);drain();assertFalse(billing.hasPro)
        billing.onPurchasesUpdated(result(BillingClient.BillingResponseCode.OK),listOf(purchase(packageName="other")));drain();assertFalse(billing.hasPro)
        billing.onPurchasesUpdated(result(BillingClient.BillingResponseCode.OK),listOf(purchase(product="other")));drain();assertFalse(billing.hasPro)
    }
    @Test fun onlySuccessfulAcknowledgementGrantsAndRetryWorks() {
        var success=false;var calls=0
        val billing=ProBilling(context,true,false,key) {_,done ->calls++;done(success)}
        billing.onPurchasesUpdated(result(BillingClient.BillingResponseCode.OK),listOf(purchase(ack=false)));drain()
        assertEquals(1,calls);assertFalse(billing.hasPro)
        success=true
        billing.onPurchasesUpdated(result(BillingClient.BillingResponseCode.OK),listOf(purchase(ack=false)));drain()
        assertEquals(2,calls);assertTrue(billing.hasPro);assertTrue(ProBilling(context,true,false,key).hasPro)
    }
    @Test fun copiedOrTamperedCacheCannotCreateProWithoutVerifiedPurchase() {
        context.getSharedPreferences("liftant_pro_purchase",0).edit().putBoolean("acknowledged",true)
            .putString("purchase",purchase().originalJson).putString("signature","fake").commit()
        assertFalse(ProBilling(context,true,false,key).hasPro)
    }
}
