package cz.rezim.app

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.ServiceCompat

// User-started local countdown; keeps only the CPU awake, never the screen.
// No microphone, sensors, location, exact-alarm access or network are needed.
class RestTimerService : Service() {
    private val context:Context get()=this
    override fun attachBaseContext(newBase:Context) {super.attachBaseContext(AppLanguage.wrap(newBase))}
    private lateinit var repository:RestTimerRepository
    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock:PowerManager.WakeLock? = null
    private val finishTask = Runnable { finishIfDue() }

    override fun onCreate() {
        super.onCreate()
        repository = RestTimerRepository(this)
        RestTimerNotifications.createChannels(this)
    }
    override fun onBind(intent:Intent?):IBinder? = null
    override fun onStartCommand(intent:Intent?, flags:Int, startId:Int):Int {
        val now=SystemClock.elapsedRealtime()
        var timer=repository.read()
        // Stale controls from another workout must not affect the current rest.
        val requestedSession=intent?.getLongExtra("session",0)?:0L
        val action=if(requestedSession!=0L && requestedSession!=timer.sessionId && intent?.action!=RestTimerCommands.START)
            RestTimerCommands.SYNC else intent?.action
        // An already expired rest cannot be resurrected by a delayed pause/+30 command.
        timer=when(if(timer.phase==RestPhase.RUNNING && timer.remainingMillis(now)==0L &&
            action !in setOf(RestTimerCommands.START,RestTimerCommands.CANCEL)) RestTimerCommands.SYNC else action) {
            RestTimerCommands.START -> runCatching {
                RestCountdown.start(intent?.getLongExtra("session",0)?:0,intent?.getIntExtra("seconds",0)?:0,now,intent?.getLongExtra("source",0)?:0)
            }.getOrDefault(RestCountdown())
            RestTimerCommands.PAUSE -> timer.pause(now)
            RestTimerCommands.RESUME -> timer.resume(now)
            RestTimerCommands.ADJUST -> timer.adjust(intent?.getIntExtra("seconds",0)?:0,now)
            RestTimerCommands.CANCEL -> RestCountdown()
            else -> timer
        }
        // Orphaned/rebooted/finished workouts must never produce a stale alert.
        val active = activeSessionId()
        if(timer.sessionId != active || timer.phase == RestPhase.IDLE || timer.phase == RestPhase.FINISHED) {
            repository.write(RestCountdown())
            if(action==RestTimerCommands.CANCEL) getSystemService(NotificationManager::class.java).cancel(FINISHED_ID)
            stopTimer();return START_NOT_STICKY
        }
        try {
            getSystemService(NotificationManager::class.java).cancel(FINISHED_ID)
            ServiceCompat.startForeground(this,NOTIFICATION_ID,RestTimerNotifications.countdown(this,timer),
                if(Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
            repository.write(timer)
            handler.removeCallbacks(finishTask)
            releaseWakeLock()
            if(timer.phase == RestPhase.PAUSED) {
                ServiceCompat.stopForeground(this,ServiceCompat.STOP_FOREGROUND_DETACH)
                stopSelf();return START_NOT_STICKY
            }
            val remaining=timer.remainingMillis(now)
            if(remaining <= 0) finishIfDue()
            else {
                wakeLock=getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"hmyzak:rest").apply {
                    setReferenceCounted(false);acquire(remaining+5000L)
                }
                handler.postDelayed(finishTask,remaining)
            }
        } catch (_:RuntimeException) {
            repository.writeError();stopTimer()
        }
        return if(repository.read().phase == RestPhase.RUNNING) START_STICKY else START_NOT_STICKY
    }
    private fun finishIfDue() {
        val timer=repository.read()
        if(timer.phase != RestPhase.RUNNING) return
        val remaining=timer.remainingMillis(SystemClock.elapsedRealtime())
        if(remaining > 0) { handler.postDelayed(finishTask,remaining);return }
        val active = activeSessionId()
        if(active != timer.sessionId) {repository.write(RestCountdown());stopTimer();return}
        repository.write(timer.finished())
        if(getSharedPreferences("hmyzak_prefs",MODE_PRIVATE).getBoolean("rest_haptic",true)) performRestHaptic(this)
        val manager=getSystemService(NotificationManager::class.java)
        try {
            if(manager.areNotificationsEnabled()) manager.notify(FINISHED_ID,RestTimerNotifications.finished(this,
                getSharedPreferences("hmyzak_prefs",MODE_PRIVATE).getBoolean("rest_wake_screen",false)))
        } catch (_:SecurityException) {
            // Notification permission can be revoked while a rest is running. Vibration still works.
        } finally {stopTimer()}
    }
    private fun releaseWakeLock() { wakeLock?.let { if(it.isHeld) it.release() };wakeLock=null }
    private fun activeSessionId():Long? {
        val store=LocalStore(this)
        return try {store.firstOpenSession()?.id} finally {store.close()}
    }
    private fun stopTimer() {
        handler.removeCallbacks(finishTask);releaseWakeLock()
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        ServiceCompat.stopForeground(this,ServiceCompat.STOP_FOREGROUND_REMOVE);stopSelf()
    }
    override fun onDestroy() { handler.removeCallbacks(finishTask);releaseWakeLock();super.onDestroy() }
    companion object {
        const val NOTIFICATION_ID=2901
        const val FINISHED_ID=2902
    }
}
