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
    private lateinit var store:LocalStore
    private lateinit var manager:LocationManager
    private val handler=Handler(Looper.getMainLooper())
    private var track:GpsTrack?=null
    private var tickAt=0L
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
        } catch(_:Exception) {pause(R.string.gps_error)}
    }}
    override fun onCreate() {
        super.onCreate();store=LocalStore(this);manager=getSystemService(LocationManager::class.java)
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,getString(R.string.gps_title),NotificationManager.IMPORTANCE_LOW))
    }
    override fun onBind(intent:Intent?) :IBinder?=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent==null) {stopSelf();return START_NOT_STICKY}
        if(intent.action=="PAUSE") {pause(R.string.gps_paused);return START_NOT_STICKY}
        if(intent.action!="START") {stopSelf();return START_NOT_STICKY}
        val id=intent.getStringExtra("id") ?: run {stopSelf();return START_NOT_STICKY}
        if(activeId==id && track?.phase=="RUNNING") return START_NOT_STICKY
        if(activeId!=null) {status(R.string.gps_error);return START_NOT_STICKY}
        val stored=store.gpsTracks().firstOrNull {it.id==id && it.phase!="FINISHED"} ?: run {stopSelf();return START_NOT_STICKY}
        track=stored
        try {
            check(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)
            check(manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            val open=PendingIntent.getActivity(this,71,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val pause=PendingIntent.getService(this,72,Intent(this,GpsService::class.java).setAction("PAUSE"),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val n=NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle(getString(R.string.gps_title)).setContentText(getString(R.string.gps_recording))
                .setOngoing(true).setContentIntent(open).setOnlyAlertOnce(true)
                .addAction(0,getString(R.string.gps_pause),pause).build()
            ServiceCompat.startForeground(this,NOTIFICATION,n,if(Build.VERSION.SDK_INT>=29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
            track=stored.copy(phase="RUNNING",segment=stored.segment+1)
            store.saveGps(track!!);activeId=id;tickAt=SystemClock.elapsedRealtime();lastFix=tickAt
            wake=getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"LiftAnt:GPS").apply {acquire(360000000L)}
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,3000L,0f,this,Looper.getMainLooper())
            status(R.string.gps_signal);handler.postDelayed(tick,5000)
        } catch(_:Exception) {pause(R.string.gps_permission)}
        return START_NOT_STICKY
    }
    private fun checkpoint() {
        val t=track ?: return
        if(t.phase!="RUNNING") return
        val now=SystemClock.elapsedRealtime()
        track=t.copy(elapsedMs=(t.elapsedMs+(now-tickAt).coerceAtLeast(0)).coerceAtMost(359999000));tickAt=now
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
        } catch(_:Exception) {pause(R.string.gps_error)}
    }
    override fun onProviderDisabled(provider:String) {if(provider==LocationManager.GPS_PROVIDER) pause(R.string.gps_signal)}
    override fun onProviderEnabled(provider:String) {}
    @Deprecated("Legacy callback") override fun onStatusChanged(provider:String?,status:Int,extras:Bundle?) {}
    private fun status(message:Int) {getSharedPreferences("gps_status",0).edit().putString("message_key",resources.getResourceEntryName(message)).apply()}
    private fun pause(message:Int) {
        runCatching {checkpoint()}
        track?.let {t->runCatching {store.saveGps(t.copy(phase="PAUSED"))}}
        track=track?.copy(phase="PAUSED");activeId=null;status(message)
        runCatching {manager.removeUpdates(this)};handler.removeCallbacks(tick)
        if(wake?.isHeld==true) wake?.release();wake=null
        stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()
    }
    override fun onDestroy() {
        if(track?.phase=="RUNNING") pause(R.string.gps_interrupted)
        handler.removeCallbacksAndMessages(null);runCatching {manager.removeUpdates(this)}
        if(wake?.isHeld==true) wake?.release()
        activeId=null;store.close();super.onDestroy()
    }
    companion object {
        const val CHANNEL="liftant_gps";const val NOTIFICATION=710
        @Volatile var activeId:String?=null
            private set
        fun start(c:Context,id:String):Boolean=runCatching {ContextCompat.startForegroundService(c,Intent(c,GpsService::class.java).setAction("START").putExtra("id",id));true}.getOrDefault(false)
        fun pause(c:Context) {if(activeId!=null) c.startService(Intent(c,GpsService::class.java).setAction("PAUSE"))}
    }
}
