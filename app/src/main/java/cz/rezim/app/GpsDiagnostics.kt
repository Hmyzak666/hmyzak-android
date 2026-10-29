package cz.rezim.app

import android.content.Context
import android.os.Build

/** Bounded local diagnostics. Never include exception messages, locations or activity payloads. */
internal object GpsDiagnostics {
    @Synchronized fun record(context:Context,event:String,error:Throwable?=null) {
        runCatching {
            val prefs=context.getSharedPreferences("gps_diagnostics",0)
            val frames=error?.stackTrace?.take(8)?.joinToString(" > ") {"${it.className}.${it.methodName}:${it.lineNumber}"}.orEmpty()
            val line="${System.currentTimeMillis()} sdk=${Build.VERSION.SDK_INT} version=${BuildConfig.VERSION_NAME} $event ${error?.javaClass?.simpleName.orEmpty()} $frames"
            val lines=(prefs.getString("events","").orEmpty().lines().filter {it.isNotBlank()}+line).takeLast(40)
            prefs.edit().putString("events",lines.joinToString("\n")).apply()
        }
    }
    fun read(context:Context):String=context.getSharedPreferences("gps_diagnostics",0).getString("events","").orEmpty()
}
