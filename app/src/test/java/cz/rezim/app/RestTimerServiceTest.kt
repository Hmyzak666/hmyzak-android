package cz.rezim.app

import android.app.Application
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
@Config(sdk=[28,34])
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
        store=LocalStore(context);sessionId=store.beginWorkout("mon").id
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
}
