package cz.rezim.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.core.app.NotificationCompat

/** Standard system templates only: SystemUI advances the countdown without per-second wakeups. */
internal object RestTimerNotifications {
    const val CHANNEL="hmyzak_rest_timer"
    const val WAKE_CHANNEL="liftant_rest_end_visual"
    fun createChannels(context:Context) {
        val manager=context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL,context.tr(R.string.ui_casovac_pauz_7cfc6),NotificationManager.IMPORTANCE_LOW).apply {
            description=context.tr(R.string.ui_odpocet_prestavky_pri_treninku_bez_zvuku_8c69f)
            setSound(null,null);enableVibration(false)
        })
        // A high-importance, silent visual notification is the supported best effort on a locked
        // phone. No background activity launch, full-screen intent, screen wake lock or unlock.
        manager.createNotificationChannel(NotificationChannel(WAKE_CHANNEL,context.tr(R.string.rest_end_visual_channel),NotificationManager.IMPORTANCE_HIGH).apply {
            description=context.tr(R.string.rest_wake_screen_info)
            setSound(null,null);enableVibration(false)
        })
    }
    private fun openWorkout(context:Context):PendingIntent = PendingIntent.getActivity(context,0,
        Intent(context,MainActivity::class.java).putExtra("open_workout",true)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun action(context:Context,command:String,request:Int,session:Long):PendingIntent =
        PendingIntent.getForegroundService(context,request,Intent(context,RestTimerService::class.java)
            .setAction(command).putExtra("session",session),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun countdown(base:Context,timer:RestCountdown,elapsed:Long=SystemClock.elapsedRealtime(),wall:Long=System.currentTimeMillis()):Notification {
        val context=AppLanguage.wrap(base)
        val running=timer.phase==RestPhase.RUNNING && timer.remainingMillis(elapsed)>0
        return NotificationCompat.Builder(context,CHANNEL)
            .setSmallIcon(R.drawable.ic_timer_notification).setContentTitle(context.tr(R.string.ui_liftant_pauza_a2b70))
            // Never put an initial/static duration beside the system's live countdown.
            .setContentText(if(running) context.tr(R.string.rest_running_notification)
                else if(timer.phase==RestPhase.PAUSED) context.tr(R.string.ui_pozastaveno_s_02b9a,workoutClock(restSecondsLeft(timer,elapsed)))
                else context.tr(R.string.ui_pauza_skoncila_a22ff))
            .setWhen(if(running) wall+timer.remainingMillis(elapsed) else 0L)
            .setShowWhen(running).setUsesChronometer(running).setChronometerCountDown(running)
            .setContentIntent(openWorkout(context)).setOngoing(running).setSilent(true).setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addExtras(Bundle().apply {
                // Public Android 16 Live Updates request. The OS/OEM/user decides promotion.
                if(Build.VERSION.SDK_INT>=36 && running) putBoolean("android.requestPromotedOngoing",true)
            })
            .addAction(0,context.tr(if(timer.phase==RestPhase.PAUSED) R.string.ui_pokracovat_659f1 else R.string.ui_pozastavit_0761b),
                action(context,if(timer.phase==RestPhase.PAUSED) RestTimerCommands.RESUME else RestTimerCommands.PAUSE,1,timer.sessionId))
            .addAction(0,context.tr(R.string.ui_zrusit_9bd5e),action(context,RestTimerCommands.CANCEL,2,timer.sessionId)).build()
    }
    fun finished(base:Context,visual:Boolean):Notification {
        val context=AppLanguage.wrap(base)
        return NotificationCompat.Builder(context,if(visual) WAKE_CHANNEL else CHANNEL)
            .setSmallIcon(R.drawable.ic_timer_notification).setContentTitle(context.tr(R.string.ui_pauza_skoncila_a22ff))
            .setContentText(context.tr(R.string.ui_pokracuj_az_budes_pripraveny_d3db2))
            .setContentIntent(openWorkout(context)).setAutoCancel(true).setOngoing(false)
            .setShowWhen(false).setUsesChronometer(false).setChronometerCountDown(false)
            .setCategory(NotificationCompat.CATEGORY_REMINDER).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(if(visual) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .setSilent(!visual).setTimeoutAfter(5*60*1000L).build()
    }
}
