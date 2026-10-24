package cz.rezim.app

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import com.android.billingclient.api.*
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

internal const val PRO_PRODUCT_ID="liftant_pro"

internal fun verifyPlaySignature(publicKey:String,data:String,signature:String):Boolean = runCatching {
    if(publicKey.isBlank() || data.isBlank() || signature.isBlank()) return false
    val key=KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)))
    Signature.getInstance("SHA1withRSA").run {
        initVerify(key);update(data.toByteArray(Charsets.UTF_8));verify(Base64.getDecoder().decode(signature))
    }
}.getOrDefault(false)

internal fun isPurchasedPro(products:List<String>,state:Int,signatureVerified:Boolean):Boolean =
    PRO_PRODUCT_ID in products && state==Purchase.PurchaseState.PURCHASED && signatureVerified

// One non-consumable purchase. Only the explicit TEST_PRO_UNLOCK build can bypass payment.
// Production requires the application's real Play public key; no fake unlock or local SKU is sold.
internal class ProBilling(base:Context,
    val playBuild:Boolean=BuildConfig.PLAY_DISTRIBUTION,
    val testMode:Boolean=BuildConfig.TEST_PRO_UNLOCK,
    private val publicKey:String=BuildConfig.PLAY_BILLING_PUBLIC_KEY,
    private val acknowledgeOverride:((String,(Boolean)->Unit)->Unit)?=null
):PurchasesUpdatedListener {
    private val app=base.applicationContext
    private val context:Context get()=AppLanguage.wrap(app)
    private val prefs=app.getSharedPreferences("liftant_pro_purchase",Context.MODE_PRIVATE)
    private val main=Handler(Looper.getMainLooper())
    private val processing=mutableSetOf<String>()
    private var connecting=false
    init {require(!(playBuild && testMode)) {"Test unlock cannot run in Play mode"}}
    var hasPro by mutableStateOf(testMode || (playBuild && cachedPurchaseIsValid()))
        private set
    var price by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    private val client by lazy {
        BillingClient.newBuilder(app).setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection().build()
    }
    private fun valid(p:Purchase)=isPurchasedPro(p.products,p.purchaseState,
        p.packageName==app.packageName && p.purchaseToken.isNotBlank() && verifyPlaySignature(publicKey,p.originalJson,p.signature))
    internal fun cachedPurchaseIsValid():Boolean=runCatching {
        val raw=prefs.getString("purchase",null)?:return false
        val signature=prefs.getString("signature",null)?:return false
        prefs.getBoolean("acknowledged",false) && valid(Purchase(raw,signature))
    }.getOrDefault(false)
    private var foreground=false
    private val refreshTask=object:Runnable {
        override fun run() {
            if(!foreground) return
            connect()
            main.postDelayed(this,60_000)
        }
    }
    fun resume() {
        if(!playBuild || foreground) return
        foreground=true;main.post(refreshTask)
    }
    fun pause() {foreground=false;main.removeCallbacks(refreshTask)}
    fun connect() {
        if(!playBuild || publicKey.isBlank()) return
        if(client.isReady) {refresh();return}
        if(connecting) return
        connecting=true
        client.startConnection(object:BillingClientStateListener {
            override fun onBillingSetupFinished(result:BillingResult) {main.post {
                connecting=false
                if(result.responseCode==BillingClient.BillingResponseCode.OK) refresh()
                else message=context.tr(R.string.pro_store_unavailable)
            }}
            override fun onBillingServiceDisconnected() {connecting=false}
        })
    }
    private fun queryProduct(then:(ProductDetails?)->Unit) {
        val query=QueryProductDetailsParams.newBuilder().setProductList(listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId(PRO_PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build()
        )).build()
        client.queryProductDetailsAsync(query) {result,details -> main.post {
            val product=if(result.responseCode==BillingClient.BillingResponseCode.OK)
                details.productDetailsList.firstOrNull {it.productId==PRO_PRODUCT_ID} else null
            price=product?.oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice
            then(product)
        }}
    }
    fun refresh() {
        if(!playBuild || publicKey.isBlank()) return
        if(!client.isReady) {connect();return}
        queryProduct {}
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) {result,purchases -> main.post {
            handleOwnedPurchases(result,purchases)
            // An offline query must never revoke an acknowledged, verified cached purchase.
        }}
    }
    internal fun handleOwnedPurchases(result:BillingResult,purchases:List<Purchase>) {
        if(!playBuild || result.responseCode!=BillingClient.BillingResponseCode.OK) return
        val owned=purchases.filter {valid(it)}
        if(owned.isEmpty()) {prefs.edit().clear().apply();hasPro=false}
        owned.forEach(::process)
        if(purchases.any {PRO_PRODUCT_ID in it.products && it.purchaseState==Purchase.PurchaseState.PENDING})
            message=context.tr(R.string.pro_payment_pending)
    }
    fun buy(activity:Activity) {
        if(!playBuild || publicKey.isBlank() || hasPro || busy) return
        if(!client.isReady) {connect();message=context.tr(R.string.pro_store_unavailable);return}
        busy=true
        // Query again at checkout; product details and offer tokens are not persisted.
        queryProduct {details ->
            val token=details?.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken
            if(details==null || token==null) {busy=false;message=context.tr(R.string.pro_store_unavailable);return@queryProduct}
            val params=BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).setOfferToken(token).build()
            val result=client.launchBillingFlow(activity,BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build())
            if(result.responseCode!=BillingClient.BillingResponseCode.OK) {busy=false;message=context.tr(R.string.pro_store_unavailable)}
        }
    }
    override fun onPurchasesUpdated(result:BillingResult,purchases:List<Purchase>?) {main.post {
        busy=false
        when(result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach {p ->
                if(p.purchaseState==Purchase.PurchaseState.PENDING) message=context.tr(R.string.pro_payment_pending)
                else process(p)
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> message=context.tr(R.string.pro_store_unavailable)
        }
    }}
    private fun process(p:Purchase) {
        if(!valid(p)) {message=context.tr(R.string.pro_purchase_invalid);return}
        fun grant() {
            prefs.edit().putString("purchase",p.originalJson).putString("signature",p.signature).putBoolean("acknowledged",true).apply()
            val newlyGranted=!hasPro
            hasPro=true
            if(newlyGranted) message=context.tr(R.string.pro_active)
        }
        if(p.isAcknowledged) {grant();return}
        if(!processing.add(p.purchaseToken)) return
        val finished:(Boolean)->Unit={ok -> main.post {
            processing.remove(p.purchaseToken)
            if(ok) grant() else message=context.tr(R.string.pro_ack_retry)
        }}
        if(acknowledgeOverride!=null) acknowledgeOverride.invoke(p.purchaseToken,finished)
        else client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) {result ->
            finished(result.responseCode==BillingClient.BillingResponseCode.OK)
        }
    }
    fun clearMessage() {message=null}
    fun close() {pause();if(playBuild && publicKey.isNotBlank()) client.endConnection()}
}
