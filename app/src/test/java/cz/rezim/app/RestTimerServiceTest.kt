package cz.rezim.app

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28,34,36])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class RestTimerServiceTest {
    private lateinit var context:Application
    private lateinit var store:LocalStore
    private lateinit var repository:RestTimerRepository
    private var controller:ServiceController<RestTimerService>?=null
    private var sessionId=0L
    @Before fun before() {
        context=RuntimeEnvironment.getApplication();context.deleteDatabase("rezim_local_v1.db")
        context.getSharedPreferences("hmyzak_rest_timer",Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("hmyzak_prefs",Context.MODE_PRIVATE).edit().clear().commit()
        store=LocalStore(context).withLegacyTestPlans();sessionId=store.beginWorkout("mon").id
        repository=RestTimerRepository(context)
    }
    @After fun after() {controller?.destroy();store.close();context.deleteDatabase("rezim_local_v1.db")}
    private fun service():RestTimerService {
        if(controller==null) controller=Robolectric.buildService(RestTimerService::class.java).create()
        return controller!!.get()
    }
    private fun command(action:String,seconds:Int=0) {
        service().onStartCommand(Intent(context,RestTimerService::class.java).setAction(action)
            .putExtra("session",sessionId).putExtra("seconds",seconds),0,1)
    }
    @Test fun serviceFinishesOnceAndProducesOnlySilentNotifications() {
        command(RestTimerCommands.START,2)
        assertEquals(RestPhase.RUNNING,repository.read().phase)
        val manager=context.getSystemService(NotificationManager::class.java)
        val running=shadowOf(manager).getNotification(RestTimerService.NOTIFICATION_ID)
        assertNotNull(running);assertNull(running.sound)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
        assertEquals(RestPhase.FINISHED,repository.read().phase)
        assertNotNull(shadowOf(manager).getNotification(RestTimerService.FINISHED_ID))
        assertNull(shadowOf(manager).getNotification(RestTimerService.FINISHED_ID).sound)
        val before=shadowOf(manager).allNotifications.size
        service().onStartCommand(null,0,2)
        assertEquals(before,shadowOf(manager).allNotifications.size)
    }
    @Test fun pausedRestDoesNotFinishAndResumeUsesRemainingTime() {
        command(RestTimerCommands.START,60)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))
        command(RestTimerCommands.PAUSE)
        assertEquals(RestPhase.PAUSED,repository.read().phase)
        assertEquals(50000L,repository.read().remainingMillis(SystemClock.elapsedRealtime()))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(60))
        assertEquals(RestPhase.PAUSED,repository.read().phase)
        command(RestTimerCommands.RESUME)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(49))
        assertEquals(RestPhase.RUNNING,repository.read().phase)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        assertEquals(RestPhase.FINISHED,repository.read().phase)
    }
    @Test fun canceledOrFinishedWorkoutCannotRaiseLateAlert() {
        command(RestTimerCommands.START,2)
        store.finishWorkout(sessionId)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
        assertEquals(RestPhase.IDLE,repository.read().phase)
        assertNull(shadowOf(context.getSystemService(NotificationManager::class.java)).getNotification(RestTimerService.FINISHED_ID))
        command(RestTimerCommands.START,60)
        assertEquals(RestPhase.IDLE,repository.read().phase)
    }
    @Test fun cancellationRemovesDeadlineAndNotification() {
        command(RestTimerCommands.START,2);command(RestTimerCommands.CANCEL)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
        assertEquals(RestPhase.IDLE,repository.read().phase)
        assertTrue(shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications.isEmpty())
    }
    @Test fun repositoryPersistsCountdownButRejectsPreviousBoot() {
        val value=RestCountdown.start(sessionId,90,SystemClock.elapsedRealtime(),42)
        repository.write(value)
        assertEquals(value,RestTimerRepository(context).read())
        val old=Settings.Global.getInt(context.contentResolver,Settings.Global.BOOT_COUNT,-1)
        Settings.Global.putInt(context.contentResolver,Settings.Global.BOOT_COUNT,old+1)
        assertEquals(RestPhase.IDLE,repository.read().phase)
    }
    private fun notice()=shadowOf(context.getSystemService(NotificationManager::class.java))
        .getNotification(RestTimerService.NOTIFICATION_ID)
    private fun assertRunningNotice(postNotification:()->Unit) {
        // System wall time is real in Robolectric, while elapsed time is virtual.
        // Bracket notification creation instead of assuming CI finishes within 100 ms.
        val offsetBefore=System.currentTimeMillis()-SystemClock.elapsedRealtime()
        postNotification()
        val offsetAfter=System.currentTimeMillis()-SystemClock.elapsedRealtime()
        val n=notice();assertNotNull(n)
        assertTrue(n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(n.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertTrue(n.extras.getBoolean(Notification.EXTRA_SHOW_WHEN))
        val deadline=repository.read().endElapsed
        val earliest=deadline+minOf(offsetBefore,offsetAfter)
        val latest=deadline+maxOf(offsetBefore,offsetAfter)
        assertTrue("Notification deadline ${n.`when`} outside [$earliest, $latest]",
            n.`when` in earliest..latest)
        assertFalse(n.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains(Regex("[0-9]:[0-9]")))
        assertEquals(Notification.VISIBILITY_PUBLIC,n.visibility)
        assertNull(n.fullScreenIntent)
        if(android.os.Build.VERSION.SDK_INT>=36) assertTrue(n.extras.getBoolean("android.requestPromotedOngoing"))
    }
    @Test fun chronometerAlwaysTracksPauseResumeAndBothLengthAdjustments() {
        assertRunningNotice { command(RestTimerCommands.START,90) }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))
        assertRunningNotice { command(RestTimerCommands.ADJUST,30) }
        assertEquals(110000L,repository.read().remainingMillis(SystemClock.elapsedRealtime()))
        assertRunningNotice { command(RestTimerCommands.ADJUST,-30) }
        command(RestTimerCommands.PAUSE)
        assertFalse(notice().extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertFalse(notice().extras.getBoolean(Notification.EXTRA_SHOW_WHEN))
        assertFalse(notice().extras.getBoolean("android.requestPromotedOngoing"))
        assertTrue(notice().extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("1:20"))
        command(RestTimerCommands.ADJUST,-30)
        assertTrue(notice().extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("0:50"))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(2))
        assertRunningNotice { command(RestTimerCommands.RESUME) }
        assertEquals(50000L,repository.read().remainingMillis(SystemClock.elapsedRealtime()))
        assertEquals(1,shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications.size)
    }
    @Test fun elapsedClockBuildsDeadlineDespiteWallClockChangeAndNoPerSecondPosting() {
        val timer=RestCountdown.start(sessionId,90,1000)
        val n=RestTimerNotifications.countdown(context,timer,21250,5_000_000L)
        assertEquals(5_069_750L,n.`when`)
        command(RestTimerCommands.START,90)
        val original=notice()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(20))
        assertSame(original,notice())
        assertEquals(70000L,repository.read().remainingMillis(SystemClock.elapsedRealtime()))
    }
    @Test fun processRestartRestoresPersistedDeadlineAndDoesNotDuplicateNotification() {
        command(RestTimerCommands.START,90)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(20))
        val end=repository.read().endElapsed
        controller!!.destroy();controller=null
        assertRunningNotice { service().onStartCommand(null,0,2) }
        assertEquals(end,repository.read().endElapsed)
        assertEquals(1,shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications.size)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(71))
        assertNull(notice())
        val completed=shadowOf(context.getSystemService(NotificationManager::class.java)).getNotification(RestTimerService.FINISHED_ID)
        assertNotNull(completed)
        assertFalse(completed.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertFalse(completed.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertFalse(completed.extras.getBoolean("android.requestPromotedOngoing"))
    }
    @Test fun expiredProcessStateCannotBeResurrectedByDelayedControls() {
        repository.write(RestCountdown.start(sessionId,1,SystemClock.elapsedRealtime()-2000))
        command(RestTimerCommands.ADJUST,30)
        assertEquals(RestPhase.FINISHED,repository.read().phase)
        assertNull(notice())
        assertEquals(1,shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications.size)
        command(RestTimerCommands.RESUME)
        assertEquals(1,shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications.size)
    }
    @Test fun notificationPermissionLossKeepsCountdownAndForegroundReconciliationRestoresNotice() {
        val manager=context.getSystemService(NotificationManager::class.java)
        command(RestTimerCommands.START,60)
        shadowOf(manager).setNotificationsEnabled(false)
        command(RestTimerCommands.ADJUST,-30)
        assertEquals(RestPhase.RUNNING,repository.read().phase)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(31))
        assertEquals(RestPhase.FINISHED,repository.read().phase)
        assertNull(shadowOf(manager).getNotification(RestTimerService.FINISHED_ID))
        shadowOf(manager).setNotificationsEnabled(true)
        command(RestTimerCommands.START,90)
        manager.cancel(RestTimerService.NOTIFICATION_ID)
        assertRunningNotice { command(RestTimerCommands.SYNC) }
    }
    @Test fun optedInCompletionUsesOnlySilentVisualChannelAndReleasesCpuLock() {
        context.getSharedPreferences("hmyzak_prefs",0).edit().putBoolean("rest_wake_screen",true).commit()
        command(RestTimerCommands.START,2)
        val power=context.getSystemService(android.os.PowerManager::class.java)
        val lock=org.robolectric.shadows.ShadowPowerManager.getLatestWakeLock()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
        val manager=context.getSystemService(NotificationManager::class.java)
        val n=shadowOf(manager).getNotification(RestTimerService.FINISHED_ID)
        assertEquals(RestTimerNotifications.WAKE_CHANNEL,n.channelId)
        assertEquals(NotificationManager.IMPORTANCE_HIGH,manager.getNotificationChannel(n.channelId).importance)
        assertNull(manager.getNotificationChannel(n.channelId).sound)
        assertFalse(manager.getNotificationChannel(n.channelId).shouldVibrate())
        assertNull(n.fullScreenIntent);assertFalse(lock.isHeld)
        assertTrue(shadowOf(context).nextStartedActivity==null)
    }
    @Test fun turningVisualOptionOffDuringRestIsRespectedAtCompletion() {
        val prefs=context.getSharedPreferences("hmyzak_prefs",0)
        prefs.edit().putBoolean("rest_wake_screen",true).commit()
        command(RestTimerCommands.START,2)
        prefs.edit().putBoolean("rest_wake_screen",false).commit()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
        val n=shadowOf(context.getSystemService(NotificationManager::class.java)).getNotification(RestTimerService.FINISHED_ID)
        assertEquals(RestTimerNotifications.CHANNEL,n.channelId)
    }
}
