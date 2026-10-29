package cz.rezim.app

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.*
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/** Only explicit visible-screen Start/Resume starts collection; no boot receiver/background permission. */
internal class GpsService:Service(),LocationListener {
    private val store:LocalStore by lazy {LocalStore(this)}
    private val manager:LocationManager by lazy {getSystemService(LocationManager::class.java)}
    private val handler=Handler(Looper.getMainLooper())
    private var track:GpsTrack?=null
    private var tickAt:Long?=null
    private var lastFix=0L
    private var wake:PowerManager.WakeLock?=null
    private val tick=object:Runnable {override fun run() {
        try {
            if(ContextCompat.checkSelfPermission(this@GpsService,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {pause(R.string.gps_permission);return}
            checkpoint()
            if(track?.phase=="RUNNING") {
                if(SystemClock.elapsedRealtime()-lastFix>30000) status(R.string.gps_signal)
                if((track?.elapsedMs ?: 0)>=359998000) {pause(R.string.gps_limit);return}
                handler.postDelayed(this,5000)
            }
        } catch(e:Exception) {GpsDiagnostics.record(this@GpsService,"tick",e);pause(R.string.gps_error)}
    }}
    override fun onBind(intent:Intent?) :IBinder?=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        try {
            when(intent?.action) {
                "PAUSE" -> {pause(R.string.gps_paused);return START_NOT_STICKY}
                "START" -> Unit
                else -> {stopSelfResult(startId);return START_NOT_STICKY}
            }
            // Satisfy the foreground contract before database decoding, provider checks or I/O.
            // Promotion can itself fail (permission revoked / app no longer eligible).
            promote()
            val id=intent.getStringExtra("id") ?: error("Missing track")
            if(activeId==id && track?.phase=="RUNNING") return START_NOT_STICKY
            if(activeId!=null) return START_NOT_STICKY // Never replace an ongoing activity.
            val stored=store.gpsTracks().firstOrNull {it.id==id && it.phase!="FINISHED"}
                ?: error("Missing unfinished track")
            // A persisted RUNNING track belongs to a previous service/process. Do not count
            // the time since boot (or time spent paused) before this instance has started.
            track=stored.copy(phase="PAUSED")
            check(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)
            if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                pause(R.string.gps_permission);return START_NOT_STICKY
            }
            val now=SystemClock.elapsedRealtime()
            track=stored.copy(phase="RUNNING",segment=Math.addExact(stored.segment,1))
            tickAt=now;lastFix=now
            store.saveGps(track!!);activeId=id
            wake=getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"LiftAnt:GPS").apply {acquire(360000000L)}
            // Satellite GPS works without data/Wi-Fi. Do not gate this on connectivity or tiles.
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,3000L,0f,this,Looper.getMainLooper())
            status(R.string.gps_signal);handler.removeCallbacks(tick);handler.postDelayed(tick,5000)
            GpsDiagnostics.record(this,"started")
        } catch(e:Exception) {
            GpsDiagnostics.record(this,"start",e)
            pause(if(e is SecurityException) R.string.gps_permission else R.string.gps_error)
        }
        return START_NOT_STICKY
    }
    private fun promote() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,getString(R.string.gps_title),NotificationManager.IMPORTANCE_LOW))
        val open=PendingIntent.getActivity(this,71,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val pause=PendingIntent.getService(this,72,Intent(this,GpsService::class.java).setAction("PAUSE"),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n=NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(getString(R.string.gps_title)).setContentText(getString(R.string.gps_recording))
            .setOngoing(true).setContentIntent(open).setOnlyAlertOnce(true)
            .addAction(0,getString(R.string.gps_pause),pause).build()
        ServiceCompat.startForeground(this,NOTIFICATION,n,if(Build.VERSION.SDK_INT>=29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
    }
    private fun checkpoint() {
        val t=track ?: return
        if(t.phase!="RUNNING") return
        val previous=tickAt ?: return
        val now=SystemClock.elapsedRealtime()
        track=t.copy(elapsedMs=(t.elapsedMs+(now-previous).coerceAtLeast(0)).coerceAtMost(359999000));tickAt=now
        store.saveGps(track!!)
    }
    override fun onLocationChanged(location:Location) {
        try {
            val t=track ?: return
            if(t.phase!="RUNNING" || !location.hasAccuracy() || location.isFromMockProvider) return
            checkpoint()
            val point=TrackPoint(location.latitude,location.longitude,location.accuracy.toDouble(),location.elapsedRealtimeNanos/1000000,t.segment)
            val updated=GpsFilter.accept(track!!,point,SystemClock.elapsedRealtime())
            if(updated.points.size!=track!!.points.size) {lastFix=SystemClock.elapsedRealtime();status(R.string.gps_recording)}
            track=updated;store.saveGps(updated)
        } catch(e:Exception) {GpsDiagnostics.record(this,"location",e);pause(R.string.gps_error)}
    }
    override fun onProviderDisabled(provider:String) {if(provider==LocationManager.GPS_PROVIDER) pause(R.string.gps_signal)}
    override fun onProviderEnabled(provider:String) {}
    @Deprecated("Legacy callback") override fun onStatusChanged(provider:String?,status:Int,extras:Bundle?) {}
    private fun status(message:Int) {
        runCatching {getSharedPreferences("gps_status",0).edit().putString("message_key",resources.getResourceEntryName(message)).apply()}
            .onFailure {GpsDiagnostics.record(this,"status",it)}
    }
    private fun persistPause() {
        runCatching {checkpoint()}.onFailure {GpsDiagnostics.record(this,"checkpoint",it)}
        track=track?.copy(phase="PAUSED");tickAt=null
        track?.let {t->runCatching {store.saveGps(t)}.onFailure {GpsDiagnostics.record(this,"persist_pause",it)}}
    }
    private fun releaseResources() {
        handler.removeCallbacksAndMessages(null)
        runCatching {manager.removeUpdates(this)}.onFailure {GpsDiagnostics.record(this,"remove_updates",it)}
        runCatching {if(wake?.isHeld==true) wake?.release()}.onFailure {GpsDiagnostics.record(this,"wake_release",it)}
        wake=null;activeId=null
        runCatching {stopForeground(STOP_FOREGROUND_REMOVE)}.onFailure {GpsDiagnostics.record(this,"stop_foreground",it)}
    }
    private fun pause(message:Int) {
        persistPause();status(message);releaseResources()
        runCatching {stopSelf()}.onFailure {GpsDiagnostics.record(this,"stop",it)}
    }
    override fun onDestroy() {
        if(track?.phase=="RUNNING") {persistPause();status(R.string.gps_interrupted)}
        releaseResources()
        runCatching {store.close()}.onFailure {GpsDiagnostics.record(this,"close",it)}
        super.onDestroy()
    }
    companion object {
        const val CHANNEL="liftant_gps";const val NOTIFICATION=710
        @Volatile var activeId:String?=null
            private set
        fun start(c:Context,id:String):Boolean {
            // Visible UI calls only. Check before scheduling the foreground-service deadline.
            if(ContextCompat.checkSelfPermission(c,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) return false
            return runCatching {
                if(!c.getSystemService(LocationManager::class.java).isProviderEnabled(LocationManager.GPS_PROVIDER)) return false
                ContextCompat.startForegroundService(c,Intent(c,GpsService::class.java).setAction("START").putExtra("id",id));true
            }.onFailure {GpsDiagnostics.record(c,"launch",it)}.getOrDefault(false)
        }
        fun pause(c:Context) {
            runCatching {if(activeId!=null) c.startService(Intent(c,GpsService::class.java).setAction("PAUSE"))}
                .onFailure {GpsDiagnostics.record(c,"pause_request",it)}
        }
    }
}
