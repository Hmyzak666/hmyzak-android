package cz.rezim.app

import android.content.Context
import org.json.JSONObject

// Whitelist only lasting user preferences; never include purchase tokens, entitlement or live timers.
internal object BackupPreferences {
    private val ints=mapOf("design" to 0..2,"rest_seconds" to 1..MAX_REST_SECONDS)
    private val booleans=setOf("haptic","auto_scroll","rest_haptic")
    fun export(context:Context):JSONObject {
        val prefs=context.getSharedPreferences("hmyzak_prefs",Context.MODE_PRIVATE)
        return JSONObject().apply {
            (ints.keys+booleans+setOf("language","rest_mode")).forEach {key ->
                if(prefs.contains(key)) put(key,prefs.all[key])
            }
            put("language",AppLanguage.selected(context))
        }
    }
    fun validate(value:JSONObject?):Map<String,Any> {
        if(value==null) return emptyMap()
        return buildMap {
            for((key,range) in ints) if(value.has(key)) {
                val v=value.get(key)
                require((v is Int || v is Long) && (v as Number).toLong() in range.first.toLong()..range.last.toLong())
                put(key,(v as Number).toInt())
            }
            for(key in booleans) if(value.has(key)) {val v=value.get(key);require(v is Boolean);put(key,v)}
            if(value.has("rest_mode")) {
                val v=value.getString("rest_mode");require(RestStartMode.entries.any {it.name==v});put("rest_mode",v)
            }
            if(value.has("language")) {
                val v=value.getString("language");require(v.isEmpty() || v in AppLanguage.supported);put("language",v)
            }
        }
    }
    fun restore(context:Context,values:Map<String,Any>) {
        val editor=context.getSharedPreferences("hmyzak_prefs",Context.MODE_PRIVATE).edit()
        values.forEach {(key,value)->when(value) {
            is Int->editor.putInt(key,value);is Boolean->editor.putBoolean(key,value);is String->editor.putString(key,value)
        }}
        check(editor.commit()) {"Could not save restored preferences"}
        (values["language"] as? String)?.let {if(AppLanguage.selected(context)!=it) AppLanguage.choose(context,it)}
    }
}

// Keep a portable copy even when the user chose the language in Android settings.
internal fun captureBackupLanguage(context:Context) {
    if(android.os.Build.VERSION.SDK_INT>=33) {
        check(context.getSharedPreferences("hmyzak_prefs",Context.MODE_PRIVATE).edit()
            .putString("language",AppLanguage.selected(context)).commit())
    }
}
internal fun finishSystemRestore(context:Context) {
    // Device-local receipts and monotonic timer deadlines never represent restored ownership/time.
    context.getSharedPreferences("liftant_pro_purchase",Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("hmyzak_rest_timer",Context.MODE_PRIVATE).edit().clear().commit()
    if(android.os.Build.VERSION.SDK_INT>=33 && AppLanguage.selected(context).isEmpty()) {
        val language=context.getSharedPreferences("hmyzak_prefs",Context.MODE_PRIVATE).getString("language","").orEmpty()
        if(language in AppLanguage.supported) AppLanguage.choose(context,language)
    }
}
