package cz.rezim.app
import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.location.LocationManager
import android.os.Looper
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28,34,36])
class GpsServiceTest {
    private lateinit var app:Application
    private lateinit var store:LocalStore
    private var controller:org.robolectric.android.controller.ServiceController<GpsService>?=null
    @Before fun setup() {
        app=RuntimeEnvironment.getApplication();app.deleteDatabase("rezim_local_v1.db");store=LocalStore(app)
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowOf(app.getSystemService(LocationManager::class.java)).setProviderEnabled(LocationManager.GPS_PROVIDER,true)
        store.saveGps(GpsTrack("service","RUNNING","2026-10-28"))
    }
    @After fun done() {controller?.destroy();store.close()}
    private fun service():GpsService {
        if(controller==null) controller=Robolectric.buildService(GpsService::class.java).create()
        return controller!!.get()
    }
    private fun start()=service().onStartCommand(Intent(app,GpsService::class.java).setAction("START").putExtra("id","service"),0,1)
    @Test fun onlyExplicitStartCollectsAndPauseStopsElapsedTime() {
        service().onStartCommand(null,0,1);assertNull(GpsService.activeId)
        start();assertEquals("service",GpsService.activeId)
        assertNotNull(shadowOf(app.getSystemService(NotificationManager::class.java)).getNotification(GpsService.NOTIFICATION))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))
        val elapsed=store.gpsTracks().single().elapsedMs;assertTrue(elapsed>=10000)
        service().onStartCommand(Intent(app,GpsService::class.java).setAction("PAUSE"),0,2)
        assertNull(GpsService.activeId)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(20))
        assertEquals(elapsed,store.gpsTracks().single().elapsedMs)
        start();shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))
        assertEquals(elapsed+5000,store.gpsTracks().single().elapsedMs)
    }
    @Test fun permissionRevocationPausesAndPreservesCheckpoint() {
        start();shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))
        assertNull(GpsService.activeId);assertEquals("PAUSED",store.gpsTracks().single().phase)
        assertTrue(store.gpsTracks().single().elapsedMs>=5000)
    }
    @Test fun serviceDestructionPreservesAndNeverRestartsItself() {
        start();shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))
        controller!!.destroy();controller=null
        assertEquals("PAUSED",store.gpsTracks().single().phase);assertNull(GpsService.activeId)
        assertEquals(android.app.Service.START_NOT_STICKY,service().onStartCommand(null,0,1))
        assertNull(GpsService.activeId)
    }

    @Test fun repeatedResumeDoesNotDuplicateTimersOrSegments() {
        start();val segment=store.gpsTracks().single().segment
        repeat(5) {start()}
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))
        assertEquals(segment,store.gpsTracks().single().segment)
        assertEquals(10000L,store.gpsTracks().single().elapsedMs)
    }
    @Test fun disabledGpsLeavesRecoverableDiscardableRecord() {
        shadowOf(app.getSystemService(LocationManager::class.java)).setProviderEnabled(LocationManager.GPS_PROVIDER,false)
        assertFalse(GpsService.start(app,"service"))
        start() // Also exercise a provider change after the caller preflight.
        assertNull(GpsService.activeId)
        assertEquals(0L,store.gpsTracks().single().elapsedMs)
        assertEquals("PAUSED",store.gpsTracks().single().phase)
        assertTrue(store.discardGps("service"))
    }
    @Test fun interruptedRunningRecordNeverCountsTimeSinceBoot() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofHours(2))
        store.saveGps(store.gpsTracks().single().copy(phase="RUNNING",elapsedMs=9000,segment=2))
        start();shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))
        val t=store.gpsTracks().single()
        assertEquals(14000L,t.elapsedMs);assertEquals(3,t.segment)
    }
    @Test fun invalidAndMissingCommandsStopWithoutThrowing() {
        service().onStartCommand(Intent(app,GpsService::class.java).setAction("START"),0,1)
        assertNull(GpsService.activeId)
        service().onStartCommand(Intent(app,GpsService::class.java).setAction("START").putExtra("id","unknown"),0,2)
        assertNull(GpsService.activeId);assertEquals(1,store.gpsTracks().size)
        assertTrue(GpsDiagnostics.read(app).contains("start"))
    }
    @Test fun databaseDecodeFailureIsContainedAndDiagnosed() {
        store.writableDatabase.execSQL("UPDATE gps_tracks SET payload=? WHERE id=?",arrayOf("broken","service"))
        start();assertNull(GpsService.activeId)
        assertTrue(GpsDiagnostics.read(app).contains("JSONException"))
        assertNull(shadowOf(app.getSystemService(NotificationManager::class.java)).getNotification(GpsService.NOTIFICATION))
    }
    @Test fun satelliteFixesWorkWithoutNetworkAndRejectInaccuratePoints() {
        val connectivity=app.getSystemService(android.net.ConnectivityManager::class.java)
        shadowOf(connectivity).setActiveNetworkInfo(null)
        start()
        fun fix(lat:Double,accuracy:Float) {
            service().onLocationChanged(android.location.Location(LocationManager.GPS_PROVIDER).apply {
                latitude=lat;longitude=16.0;this.accuracy=accuracy
                elapsedRealtimeNanos=android.os.SystemClock.elapsedRealtimeNanos()
            })
        }
        fix(49.0,100f);assertTrue(store.gpsTracks().single().points.isEmpty())
        fix(49.0,5f)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))
        fix(49.0001,5f)
        assertEquals(2,store.gpsTracks().single().points.size)
        assertTrue(store.gpsTracks().single().meters>10)
        service().onProviderDisabled(LocationManager.GPS_PROVIDER)
        assertEquals("PAUSED",store.gpsTracks().single().phase)
        val distance=store.gpsTracks().single().meters
        start();fix(50.0,5f) // A new segment must not bridge the interruption.
        assertEquals(distance,store.gpsTracks().single().meters,0.0)
    }
    @Test fun diagnosticsAreBoundedAndNeverStoreExceptionMessages() {
        repeat(50) {GpsDiagnostics.record(app,"test",IllegalStateException("SECRET coordinates and personal data"))}
        val text=GpsDiagnostics.read(app)
        assertEquals(40,text.lines().size);assertFalse(text.contains("SECRET"))
        assertTrue(text.contains("IllegalStateException"))
    }
}
