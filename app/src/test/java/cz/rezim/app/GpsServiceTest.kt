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
}
