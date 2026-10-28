package cz.rezim.app
import kotlin.math.abs
/** 2024 Adult Compendium https://pacompendium.com/, adults 19–59.
 * 1 MET = 1 kcal/kg/hour. Ranges are category scenarios, NOT confidence bounds.
 * No inferred HR, watts, sex, age or missing intensity. */
internal data class EnergyEstimate(val totalLow:Double,val totalHigh:Double,val activeLow:Double,val activeHigh:Double,val codes:String,val weightKg:Double,val speedBased:Boolean)
internal object EnergyCalculator {
    fun estimate(activity:String,seconds:Int,weightKg:Double?,km:Double?=null,intensity:String=""):EnergyEstimate? {
        if(seconds<=0 || weightKg==null || !weightKg.isFinite() || weightKg !in 20.0..400.0) return null
        if(intensity !in setOf("","MODERATE","VIGOROUS")) return null
        val speed=km?.takeIf {it.isFinite() && it>0}?.let {it*3600/seconds/1.609344}
        var speedBased=false
        val codes:String
        val mets:Pair<Double,Double>
        when(activity) {
            "RUNNING","TREADMILL" -> {
                val speeds=listOf(4.1,4.55,5.1,5.65,6.15,6.7,7.0,7.5,8.0,8.6,9.0,10.0,11.0,12.0,13.0,14.0)
                val values=listOf(6.5,7.8,8.5,9.0,9.3,10.5,11.0,11.8,12.0,12.5,13.0,14.8,16.8,18.5,19.8,23.0)
                val ids=listOf("12028","12029","12030","12045","12050","12060","12070","12080","12090","12100","12110","12120","12130","12132","12134","12135")
                if(speed!=null && speed in 4.0..14.0) {val i=speeds.indices.minBy {abs(speeds[it]-speed)};mets=values[i] to values[i];codes=ids[i];speedBased=true}
                else {mets=7.5 to 10.5;codes="12020 / 12145"}
            }
            "WALKING" -> {
                if(speed!=null && speed in 0.5..5.5) {
                    val i=when {speed<2->0;speed<2.5->1;speed<2.8->2;speed<3.5->3;speed<4->4;speed<4.5->5;speed<5->6;else->7}
                    val met=listOf(2.3,2.8,3.0,3.8,4.8,5.5,7.0,8.5)[i];mets=met to met
                    codes=listOf("17151","17152","17170","17190","17200","17220","17230","17231")[i];speedBased=true
                } else {mets=2.8 to 4.8;codes="17152 / 17200"}
            }
            "CYCLING" -> {
                if(speed!=null && speed in 1.0..40.0) {
                    val i=when {speed<10->0;speed<12->1;speed<14->2;speed<16->3;speed<20->4;else->5}
                    val met=listOf(4.0,6.8,8.0,10.0,12.0,16.8)[i];mets=met to met
                    codes=listOf("01010","01020","01030","01040","01050","01060")[i];speedBased=true
                } else {mets=when(intensity) {"MODERATE"->7.0 to 7.0;"VIGOROUS"->9.0 to 9.0;else->4.3 to 9.0};codes="01015–01017"}
            }
            "ELLIPTICAL" -> {mets=effort(5.0,9.0,intensity);codes="02048 / 02049"}
            "STATIONARY_BIKE" -> {mets=6.8 to 6.8;codes="01200"}
            "ROWING" -> {mets=effort(5.0,7.3,intensity);codes="02071 / 02070"}
            "HIIT" -> {mets=effort(7.0,11.0,intensity);codes="02210 / 02214"}
            "STRENGTH" -> {mets=effort(3.5,6.0,intensity);codes="02054 / 02050"}
            else -> return null
        }
        val factor=weightKg*seconds/3600.0
        return EnergyEstimate(mets.first*factor,mets.second*factor,(mets.first-1).coerceAtLeast(0.0)*factor,(mets.second-1).coerceAtLeast(0.0)*factor,codes,weightKg,speedBased)
    }
    private fun effort(m:Double,v:Double,i:String)=when(i) {"MODERATE"->m to m;"VIGOROUS"->v to v;else->m to v}
}
