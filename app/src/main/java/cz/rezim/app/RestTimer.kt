package cz.rezim.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.edit

internal enum class RestPhase { IDLE, RUNNING, PAUSED, FINISHED }

internal data class RestCountdown(
    val phase: RestPhase = RestPhase.IDLE,
    val sessionId: Long = 0,
    val endElapsed: Long = 0,
    val pausedMillis: Long = 0,
    val sourceSetId: Long = 0
) {
    fun remainingMillis(now: Long): Long = when(phase) {
        RestPhase.RUNNING -> (endElapsed-now).coerceAtLeast(0)
        RestPhase.PAUSED -> pausedMillis
        else -> 0
    }
    fun pause(now:Long):RestCountdown = if(phase == RestPhase.RUNNING && remainingMillis(now)>0)
        copy(phase=RestPhase.PAUSED,pausedMillis=remainingMillis(now),endElapsed=0) else this
    fun resume(now:Long):RestCountdown = if(phase == RestPhase.PAUSED && pausedMillis > 0)
        copy(phase=RestPhase.RUNNING,endElapsed=now+pausedMillis,pausedMillis=0) else this
    fun adjust(seconds:Int, now:Long):RestCountdown {
        if(phase !in setOf(RestPhase.RUNNING,RestPhase.PAUSED)) return this
        val remaining = (remainingMillis(now)+seconds*1000L).coerceIn(0,MAX_REST_SECONDS*1000L)
        if(phase == RestPhase.PAUSED && remaining == 0L) return RestCountdown()
        return if(phase == RestPhase.RUNNING) copy(endElapsed=now+remaining) else copy(pausedMillis=remaining)
    }
    fun finished() = copy(phase=RestPhase.FINISHED,endElapsed=0,pausedMillis=0)
    companion object {
        fun start(sessionId:Long, seconds:Int, now:Long, sourceSetId:Long=0):RestCountdown {
            require(sessionId > 0 && seconds in 1..MAX_REST_SECONDS)
            return RestCountdown(RestPhase.RUNNING,sessionId,now+seconds*1000L,sourceSetId=sourceSetId)
        }
    }
}

internal const val MAX_REST_SECONDS = 3600

internal class RestTimerRepository(context:Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("hmyzak_rest_timer",Context.MODE_PRIVATE)
    private fun bootCount() = Settings.Global.getInt(app.contentResolver,Settings.Global.BOOT_COUNT,-1)
    fun read():RestCountdown {
        if(prefs.getInt("boot",-2) != bootCount()) return RestCountdown()
        val phase = runCatching { RestPhase.valueOf(prefs.getString("phase",null) ?: "IDLE") }.getOrDefault(RestPhase.IDLE)
        return RestCountdown(phase,prefs.getLong("session",0),prefs.getLong("end",0),
            prefs.getLong("paused",0),prefs.getLong("source",0))
    }
    fun write(value:RestCountdown) {
        // Persist before vibrating so a restarted process cannot announce the same finish twice.
        prefs.edit(commit=true) {
            remove("error")
            putInt("boot",bootCount());putString("phase",value.phase.name);putLong("session",value.sessionId)
            putLong("end",value.endElapsed);putLong("paused",value.pausedMillis);putLong("source",value.sourceSetId)
        }
    }
    fun error():String? = prefs.getString("error",null)
    fun writeError() {
        write(RestCountdown())
        prefs.edit(commit=true) {putString("error","Časovač se nepodařilo spustit. Zkus ho znovu otevřít v aplikaci.")}
    }
    fun observe(listener:SharedPreferences.OnSharedPreferenceChangeListener) = prefs.registerOnSharedPreferenceChangeListener(listener)
    fun removeObserver(listener:SharedPreferences.OnSharedPreferenceChangeListener) = prefs.unregisterOnSharedPreferenceChangeListener(listener)
}

internal object RestTimerCommands {
    const val START = "cz.rezim.app.REST_START"
    const val PAUSE = "cz.rezim.app.REST_PAUSE"
    const val RESUME = "cz.rezim.app.REST_RESUME"
    const val ADJUST = "cz.rezim.app.REST_ADJUST"
    const val CANCEL = "cz.rezim.app.REST_CANCEL"
    fun send(context:Context, action:String, session:Long=0, seconds:Int=0, source:Long=0):Boolean = try {
        val intent = Intent(context,RestTimerService::class.java).setAction(action)
            .putExtra("session",session).putExtra("seconds",seconds).putExtra("source",source)
        ContextCompat.startForegroundService(context,intent)
        true
    } catch (_:RuntimeException) { false }
    fun cancel(context:Context) {
        RestTimerRepository(context).write(RestCountdown())
        context.stopService(Intent(context,RestTimerService::class.java))
        context.getSystemService(android.app.NotificationManager::class.java)?.cancel(RestTimerService.NOTIFICATION_ID)
        context.getSystemService(android.app.NotificationManager::class.java)?.cancel(RestTimerService.FINISHED_ID)
    }
}

internal fun restSecondsLeft(timer:RestCountdown, now:Long=SystemClock.elapsedRealtime()):Int =
    ((timer.remainingMillis(now)+999)/1000).toInt()

internal fun workoutClock(seconds:Int):String = if(seconds >= 3600)
    "%d:%02d:%02d".format(java.util.Locale.US,seconds/3600,(seconds/60)%60,seconds%60)
    else "%d:%02d".format(java.util.Locale.US,seconds/60,seconds%60)
