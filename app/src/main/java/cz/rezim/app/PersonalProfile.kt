package cz.rezim.app
import org.json.JSONObject
import java.time.LocalDate
internal data class PersonalProfile(val birthYear:Int?=null,val heightCm:Double?=null,val sex:String="") {
    fun ageRange(today:LocalDate=LocalDate.now()):IntRange?=birthYear?.let {(today.year-it-1).coerceAtLeast(0)..(today.year-it)}
    fun validate(today:LocalDate=LocalDate.now()) {
        require(birthYear==null || birthYear in (today.year-120)..today.year)
        require(heightCm==null || heightCm.isFinite() && heightCm in 30.0..260.0)
        require(sex in setOf("","FEMALE","MALE"))
    }
    fun encode():String {validate();return JSONObject().put("birthYear",birthYear).put("heightCm",heightCm).put("sex",sex).toString()}
    companion object {
        fun decode(raw:String):PersonalProfile {
            val j=JSONObject(raw)
            if(j.has("birthYear")) require(j.get("birthYear") is Int || j.get("birthYear") is Long)
            if(j.has("heightCm")) require(j.get("heightCm") is Number)
            return PersonalProfile(if(j.has("birthYear")) j.getInt("birthYear") else null,
                if(j.has("heightCm")) j.getDouble("heightCm") else null,j.optString("sex")).also {it.validate()}
        }
    }
}
