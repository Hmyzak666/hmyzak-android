package cz.rezim.app

import org.junit.Assert.*
import org.junit.Test
import com.android.billingclient.api.Purchase
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import java.time.LocalDate
import java.time.ZoneId

class WorkoutVolumeAndProTest {
    private fun set(id:Long,key:String,weight:String,reps:String,done:Boolean=true)=SetEntry(id,1,key,1,weight,reps,done)
    @Test fun completedExternalLoadCountsBothDumbbellsButNoBodyMass() {
        val ex=listOf(Exercise("bar","Bar",4,UnitType.KG),Exercise("db","DB",4,UnitType.KG,loadMultiplier=2),
            Exercise("pull","Pull",4,UnitType.ADDED_KG),Exercise("push","Push",1,UnitType.REPS))
        val result=workoutTotals(listOf(set(1,"bar","50","10"),set(2,"db","20","10"),
            set(3,"pull","5","8"),set(4,"push","0","20"),set(5,"bar","100","10",false)),ex)
        assertEquals(940.0,result.liftedKg,0.0001);assertEquals(4,result.sets);assertEquals(48L,result.reps)
    }
    @Test fun decimalCommaAndInvalidValuesCannotInventVolume() {
        val rows=listOf(set(1,"x","12,5","8"),set(2,"x","NaN","9"),set(3,"x","Infinity","9"),
            set(4,"x","-5","10"),set(5,"x","10",""),set(6,"x","10","-2"),set(7,"x","10","abc"))
        assertEquals(100.0,workoutTotals(rows,emptyList()).liftedKg,0.0001)
    }
    @Test fun changingDumbbellConventionDoesNotRewriteRecordedSets() {
        val rows=listOf(set(1,"x","8","12"));val before=rows.toList()
        assertEquals(96.0,workoutTotals(rows,listOf(Exercise("x","X",1,UnitType.KG))).liftedKg,0.0)
        assertEquals(192.0,workoutTotals(rows,listOf(Exercise("x","X",1,UnitType.KG,loadMultiplier=2))).liftedKg,0.0)
        assertEquals(before,rows)
    }
    @Test fun comparingAdjacentPeriodsDoesNotCountUnfinishedOrOutOfRangeWorkouts() {
        fun record(date:String,id:Long,finished:Boolean=true):TrainingSnapshot {
            val epoch=LocalDate.parse(date).atTime(12,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            return TrainingSnapshot(Session(id,"d",epoch-600000,if(finished)epoch else null,600),WorkoutTotals(4,40,1000.0))
        }
        val records=listOf(record("2026-10-01",1),record("2026-10-07",2),record("2026-10-08",3),
            record("2026-10-14",4),record("2026-10-15",5),record("2026-10-10",6,false))
        assertEquals(PeriodStats(2,8,80,2000.0,1200),periodStats(records,LocalDate.parse("2026-10-08"),LocalDate.parse("2026-10-14")))
        assertEquals(2,periodStats(records,LocalDate.parse("2026-10-01"),LocalDate.parse("2026-10-07")).workouts)
    }
    @Test fun csvProtectsQuotesNewlinesAndSpreadsheetFormulaNames() {
        assertEquals("\"A, B\"",csvCell("A, B"));assertEquals("\"A\"\"B\nC\"",csvCell("A\"B\nC"))
        assertEquals("\"'=HYPERLINK(1)\"",csvCell("=HYPERLINK(1)"))
        assertEquals("\"'  +SUM(1)\"",csvCell("  +SUM(1)"))
        assertEquals("\"12.5\"",csvCell("12.5"))
    }
    @Test fun pendingCancelledOrWrongProductsNeverUnlockPro() {
        assertFalse(isPurchasedPro(listOf(PRO_PRODUCT_ID),Purchase.PurchaseState.PENDING,true))
        assertFalse(isPurchasedPro(listOf(PRO_PRODUCT_ID),Purchase.PurchaseState.UNSPECIFIED_STATE,true))
        assertFalse(isPurchasedPro(listOf("other"),Purchase.PurchaseState.PURCHASED,true))
        assertFalse(isPurchasedPro(listOf(PRO_PRODUCT_ID),Purchase.PurchaseState.PURCHASED,false))
        assertTrue(isPurchasedPro(listOf(PRO_PRODUCT_ID),Purchase.PurchaseState.PURCHASED,true))
    }
    @Test fun forgedModifiedOrMissingPurchaseSignaturesAreRejected() {
        val pair=KeyPairGenerator.getInstance("RSA").apply {initialize(2048)}.generateKeyPair()
        val publicKey=Base64.getEncoder().encodeToString(pair.public.encoded)
        val data="{\"productId\":\"liftant_pro\",\"purchaseToken\":\"synthetic-test-token\"}"
        val signed=Signature.getInstance("SHA1withRSA").run {initSign(pair.private);update(data.toByteArray());Base64.getEncoder().encodeToString(sign())}
        assertTrue(verifyPlaySignature(publicKey,data,signed))
        assertFalse(verifyPlaySignature(publicKey,data+" ",signed))
        assertFalse(verifyPlaySignature("",data,signed));assertFalse(verifyPlaySignature(publicKey,data,"invalid"))
    }
}
