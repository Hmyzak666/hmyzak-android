package cz.rezim.app

import org.json.JSONObject
import org.json.JSONArray
import kotlin.math.*

internal data class TrackPoint(val latitude:Double,val longitude:Double,val accuracy:Double,val elapsedMs:Long,val segment:Int) {
    fun valid()=latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0 && accuracy.isFinite() && accuracy in 0.1..35.0 && elapsedMs>=0 && segment>=0
    fun distance(other:TrackPoint):Double {
        val a=Math.toRadians(latitude);val b=Math.toRadians(other.latitude)
        val h=sin((b-a)/2).pow(2)+cos(a)*cos(b)*sin(Math.toRadians(other.longitude-longitude)/2).pow(2)
        return 6371000*2*asin(sqrt(h.coerceIn(0.0,1.0)))
    }
}
internal data class GpsTrack(val id:String,val activity:String,val date:String,val elapsedMs:Long=0,val meters:Double=0.0,
    val phase:String="PAUSED",val segment:Int=0,val points:List<TrackPoint> = emptyList(),val runId:Long?=null) {
    fun validate() {
        require(id.matches(Regex("[a-zA-Z0-9-]{1,80}")) && activity in setOf("RUNNING","WALKING","CYCLING") && validDate(date))
        require(elapsedMs in 0..359999000 && meters.isFinite() && meters in 0.0..500000.0)
        require(phase in setOf("RUNNING","PAUSED","FINISHED") && segment>=0 && points.size<=100000)
        require((phase=="FINISHED")== (runId!=null));require(runId==null || runId>0)
        require(points.all {it.valid() && it.segment<=segment} && points.zipWithNext().all {(a,b)-> b.segment>=a.segment && (b.segment!=a.segment || b.elapsedMs>a.elapsedMs)})
    }
    fun encode():String {
        validate()
        return JSONObject().put("id",id).put("activity",activity).put("date",date).put("elapsedMs",elapsedMs)
            .put("meters",meters).put("phase",phase).put("segment",segment).put("runId",runId)
            .put("points",JSONArray().apply {points.forEach {put(JSONArray(listOf(it.latitude,it.longitude,it.accuracy,it.elapsedMs,it.segment)))}}).toString()
    }
    companion object {
        fun decode(raw:String):GpsTrack {
            val j=JSONObject(raw);val a=j.getJSONArray("points");require(a.length()<=100000)
            return GpsTrack(j.getString("id"),j.getString("activity"),j.getString("date"),j.getLong("elapsedMs"),j.getDouble("meters"),
                j.getString("phase"),j.getInt("segment"),List(a.length()) {val p=a.getJSONArray(it);TrackPoint(p.getDouble(0),p.getDouble(1),p.getDouble(2),p.getLong(3),p.getInt(4))},
                if(j.has("runId") && !j.isNull("runId")) j.getLong("runId") else null).also {it.validate()}
        }
    }
}
internal object GpsFilter {
    // Reject inaccurate, stale, impossible or stationary jitter fixes. A gap starts a new segment:
    // never bridge a tunnel, a pause, a process restart or a removed permission with a straight line.
    fun accept(track:GpsTrack,point:TrackPoint,nowElapsedMs:Long):GpsTrack {
        if(track.phase!="RUNNING" || !point.valid() || point.segment!=track.segment || nowElapsedMs-point.elapsedMs !in 0..15000) return track
        val last=track.points.lastOrNull()?.takeIf {it.segment==point.segment}
        if(last==null) return track.copy(points=track.points+point)
        val dt=point.elapsedMs-last.elapsedMs
        if(dt<=0) return track
        if(dt>30000) return track.copy(segment=track.segment+1,points=track.points+point.copy(segment=track.segment+1))
        val distance=last.distance(point)
        val maxSpeed=when(track.activity) {"WALKING"->4.5;"RUNNING"->12.0;else->35.0}
        if(distance/(dt/1000.0)>maxSpeed || distance<max(3.0,max(last.accuracy,point.accuracy)*0.5)) return track
        if(track.meters+distance>500000 || track.points.size>=100000) return track
        return track.copy(meters=track.meters+distance,points=track.points+point)
    }
}
