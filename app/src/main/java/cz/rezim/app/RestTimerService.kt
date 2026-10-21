package cz.rezim.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

// User-started local countdown; keeps only the CPU awake, never the screen.
// No microphone, sensors, location, exact-alarm access or network are needed.
class RestTimerService : Service() {
    private lateinit var repository:RestTimerRepository
    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock:PowerManager.WakeLock? = null
    private val finishTask = Runnable { finishIfDue() }

    override fun onCreate() {
        super.onCreate()
        repository = RestTimerRepository(this)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL,"Časovač pauz",NotificationManager.IMPORTANCE_LOW).apply {
                description="Odpočet přestávky při tréninku; bez zvuku."
                setSound(null,null);enableVibration(false)
            })
    }
    override fun onBind(intent:Intent?):IBinder? = null
    override fun onStartCommand(intent:Intent?, flags:Int, startId:Int):Int {
        val now=SystemClock.elapsedRealtime()
        var timer=repository.read()
        timer=when(intent?.action) {
            RestTimerCommands.START -> runCatching {
                RestCountdown.start(intent.getLongExtra("session",0),intent.getIntExtra("seconds",0),now,intent.getLongExtra("source",0))
            }.getOrDefault(RestCountdown())
            RestTimerCommands.PAUSE -> timer.pause(now)
            RestTimerCommands.RESUME -> timer.resume(now)
            RestTimerCommands.ADJUST -> timer.adjust(intent.getIntExtra("seconds",0),now)
            RestTimerCommands.CANCEL -> RestCountdown()
            else -> timer
        }
        // Orphaned/rebooted/finished workouts must never produce a stale alert.
        val active = activeSessionId()
        if(timer.sessionId != active || timer.phase == RestPhase.IDLE || timer.phase == RestPhase.FINISHED) {
            repository.write(RestCountdown());stopTimer();return START_NOT_STICKY
        }
        try {
            getSystemService(NotificationManager::class.java).cancel(FINISHED_ID)
            ServiceCompat.startForeground(this,NOTIFICATION_ID,notification(timer),
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
        if(manager.areNotificationsEnabled()) manager.notify(FINISHED_ID,NotificationCompat.Builder(this,CHANNEL)
            .setSmallIcon(R.drawable.ic_timer_notification).setContentTitle("Pauza skončila")
            .setContentText("Pokračuj, až budeš připravený.").setContentIntent(openWorkout())
            .setAutoCancel(true).setSilent(true).build())
        } catch (_:SecurityException) {
            // Notification permission can be revoked while a rest is running.
        } finally {stopTimer()}
    }
    private fun openWorkout():PendingIntent = PendingIntent.getActivity(this,0,
        Intent(this,MainActivity::class.java).putExtra("open_workout",true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun action(action:String, request:Int):PendingIntent = PendingIntent.getForegroundService(this,request,
        Intent(this,RestTimerService::class.java).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun notification(timer:RestCountdown):Notification = NotificationCompat.Builder(this,CHANNEL)
        .setSmallIcon(R.drawable.ic_timer_notification).setContentTitle("Hmyzák · pauza")
        .setContentText((if(timer.phase==RestPhase.PAUSED) "Pozastaveno · " else "Zbývá ")+workoutClock(restSecondsLeft(timer)))
        .setWhen(System.currentTimeMillis()+timer.remainingMillis(SystemClock.elapsedRealtime()))
        .setUsesChronometer(timer.phase == RestPhase.RUNNING).setChronometerCountDown(true)
        .setContentIntent(openWorkout()).setOngoing(timer.phase==RestPhase.RUNNING).setSilent(true).setOnlyAlertOnce(true)
        .addAction(0,if(timer.phase==RestPhase.PAUSED) "Pokračovat" else "Pozastavit",
            action(if(timer.phase==RestPhase.PAUSED) RestTimerCommands.RESUME else RestTimerCommands.PAUSE,1))
        .addAction(0,"Zrušit",action(RestTimerCommands.CANCEL,2)).build()
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
        private const val CHANNEL="hmyzak_rest_timer"
        const val NOTIFICATION_ID=2901
        const val FINISHED_ID=2902
    }
}
