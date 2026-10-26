package cz.rezim.app

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal data class QuickShortcut(val id:String=UUID.randomUUID().toString(),val kind:String,val target:String="",
    val name:String="",val rounds:Int=5,val work:Int=60,val rest:Int=120)
internal object QuickShortcuts {
    fun defaults()=listOf(QuickShortcut(id="cardio",kind="CARDIO",target="RUNNING"),QuickShortcut(id="hiit",kind="HIIT"),QuickShortcut(id="body",kind="BODY"))
    fun read(prefs:SharedPreferences):List<QuickShortcut> = prefs.getString("quick_shortcuts",null)?.let {
        runCatching {decode(it)}.getOrElse {defaults()}
    } ?: defaults()
    fun encode(items:List<QuickShortcut>):String = JSONArray().apply {
        items.forEach {put(JSONObject().put("id",it.id).put("kind",it.kind).put("target",it.target).put("name",it.name)
            .put("rounds",it.rounds).put("work",it.work).put("rest",it.rest))}
    }.toString().also {decode(it)}
    fun decode(raw:String):List<QuickShortcut> {
        require(raw.length<=100000)
        val a=JSONArray(raw);require(a.length()<=100)
        return (0 until a.length()).map {i->
            val o=a.getJSONObject(i)
            fun integer(key:String):Int {val v=o.get(key);require(v is Int || v is Long);require((v as Number).toLong() in 0..100000);return v.toInt()}
            QuickShortcut(o.getString("id"),o.getString("kind"),o.getString("target"),o.getString("name"),integer("rounds"),integer("work"),integer("rest")).also {
                require(it.id.isNotBlank() && it.id.length<=128 && it.name.length<=80 && it.target.length<=128)
                require(it.kind in setOf("STRENGTH","CARDIO","HIIT","BODY"))
                if(it.kind=="STRENGTH") require(it.target.isNotBlank())
                if(it.kind=="CARDIO") require(CardioActivity.entries.any {a->a.name==it.target})
                require(it.rounds in 1..1000 && it.work in 1..3600 && it.rest in 0..3600)
            }
        }.also {require(it.map {s->s.id}.distinct().size==it.size)}
    }
}
