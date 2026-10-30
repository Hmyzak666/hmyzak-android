package cz.rezim.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.*
import java.util.UUID

@Composable internal fun GpsCard(vm:RezimState) {
    val c=LocalContext.current
    val store=remember(c) {LocalStore(c)}
    DisposableEffect(store) {onDispose {store.close()}}
    var discardId by remember {mutableStateOf<String?>(null)}
    var diagnostics by remember {mutableStateOf(false)}
    var tracks by remember {mutableStateOf(store.gpsTracks())}
    var message by remember {mutableIntStateOf(R.string.gps_privacy)}
    var activity by rememberSaveable {mutableStateOf("RUNNING")}
    var show by rememberSaveable {mutableStateOf(false)}
    LaunchedEffect(store) {while(true) {
        try {tracks=store.gpsTracks()} catch(e:Exception) {GpsDiagnostics.record(c,"ui_read",e);message=R.string.gps_error}
        val statusKey=c.getSharedPreferences("gps_status",0).getString("message_key",null)
        message=statusKey?.let {c.resources.getIdentifier(it,"string",c.packageName)}?.takeIf {it!=0} ?: R.string.gps_privacy
        delay(1000)
    }}
    val unfinished=tracks.filter {it.phase!="FINISHED"}
    val selected=unfinished.firstOrNull()
    fun begin() {
        try {
            if(GpsService.activeId!=null) return
            val t=selected ?: GpsTrack(UUID.randomUUID().toString(),activity,currentDate()).also {store.saveGps(it)}
            if(!GpsService.start(c,t.id)) vm.tell(c.tr(R.string.gps_permission))
            show=true;tracks=store.gpsTracks()
        } catch(e:Exception) {GpsDiagnostics.record(c,"ui_action",e);vm.tell(c.tr(R.string.gps_error))}
    }
    val permissions=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {grants->
        if(grants[Manifest.permission.ACCESS_FINE_LOCATION]==true) begin() else vm.tell(c.tr(R.string.gps_permission))
    }
    fun requestStart() {
        if(ContextCompat.checkSelfPermission(c,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) begin()
        else permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    SectionCard {
        TextButton(onClick={show=!show}) {Text(c.tr(R.string.gps_title))}
        if(show || selected!=null) {
            Text(c.tr(R.string.gps_privacy),style=MaterialTheme.typography.bodySmall)
            if(selected==null) {
                var expanded by remember {mutableStateOf(false)}
                Box {
                    OutlinedButton(onClick={expanded=true}) {Text(c.tr(CardioActivity.valueOf(activity).label))}
                    DropdownMenu(expanded,{expanded=false}) {listOf("RUNNING","WALKING","CYCLING").forEach {a->DropdownMenuItem(text={Text(c.tr(CardioActivity.valueOf(a).label))},onClick={activity=a;expanded=false})}}
                }
                MainButton(c.tr(R.string.gps_start),Modifier.testTag("gps-start")) {requestStart()}
            } else {
                val running=GpsService.activeId==selected.id && selected.phase=="RUNNING"
                Text(c.tr(CardioActivity.valueOf(selected.activity).label)+" · "+durationText((selected.elapsedMs/1000).toInt())+" · ${fmtDisplay(selected.meters/1000)} km")
                if(selected.meters>0 && selected.elapsedMs>0) {
                    val speed=selected.meters*3600/selected.elapsedMs
                    Text(c.tr(R.string.gps_average)+": "+if(selected.activity=="CYCLING") "${fmtDisplay(speed)} km/h" else runningPace((selected.elapsedMs/1000).toInt(),selected.meters/1000))
                }
                val last=selected.points.takeLast(2)
                val current=if(running && last.size==2 && last[0].segment==last[1].segment && android.os.SystemClock.elapsedRealtime()-last[1].elapsedMs<15000) {
                    last[0].distance(last[1])*3600/(last[1].elapsedMs-last[0].elapsedMs)
                } else null
                Text(c.tr(R.string.gps_current)+": "+(current?.let {"${fmtDisplay(it)} km/h"} ?: "–"))
                Text(c.tr(if(!running && selected.phase=="RUNNING") R.string.gps_interrupted else if(!running) R.string.gps_paused else message))
                val weight=vm.measures.firstOrNull {it.kg!=null && it.date<=selected.date}?.kg
                EnergyValues(EnergyCalculator.estimate(selected.activity,(selected.elapsedMs/1000).toInt(),weight,selected.meters/1000))
                if(running) MainButton(c.tr(R.string.gps_pause)) {GpsService.pause(c)}
                else {
                    MainButton(c.tr(R.string.gps_resume)) {requestStart()}
                    OutlinedButton(onClick={
                        try {store.finishGps(selected.id);tracks=store.gpsTracks();vm.refresh();vm.tell(c.tr(R.string.cardio_saved))}
                        catch(_:Exception) {vm.tell(c.tr(R.string.gps_error))}
                    },enabled=selected.elapsedMs>=1000,modifier=Modifier.testTag("gps-finish")) {Text(c.tr(R.string.gps_finish))}
                }
                if(!running) TextButton(onClick={discardId=selected.id},modifier=Modifier.testTag("gps-discard")) {Text(c.tr(R.string.gps_discard))}
                RouteMap(selected)
            }
        }
        TextButton(onClick={diagnostics=true},modifier=Modifier.testTag("gps-diagnostics")) {Text(c.tr(R.string.gps_diagnostics))}
    }
    discardId?.let {id->AlertDialog(
        onDismissRequest={discardId=null},
        title={Text(c.tr(R.string.gps_discard))},text={Text(c.tr(R.string.gps_discard_body))},
        confirmButton={TextButton(modifier=Modifier.testTag("gps-discard-confirm"),onClick={
            try {
                store.discardGps(id);tracks=store.gpsTracks();vm.refresh();show=true;discardId=null
            } catch(e:Exception) {GpsDiagnostics.record(c,"discard",e);vm.tell(c.tr(R.string.gps_error))}
        }) {Text(c.tr(R.string.gps_discard))}},
        dismissButton={TextButton(onClick={discardId=null}) {Text(c.tr(R.string.gps_keep))}}
    )}
    if(diagnostics) AlertDialog(onDismissRequest={diagnostics=false},
        title={Text(c.tr(R.string.gps_diagnostics))},
        text={SelectionContainer {Text(GpsDiagnostics.read(c).ifBlank {"–"},modifier=Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState()))}},
        confirmButton={TextButton(onClick={diagnostics=false}) {Text(c.tr(R.string.gps_close))}})
}
/** Offline geographic map: north-up equirectangular local projection, grid, coordinates and scale.
 * No tile servers, network requests or disclosure of route coordinates. */
@Composable internal fun RouteMap(track:GpsTrack) {
    val c=LocalContext.current
    if(track.points.isEmpty()) {Text(c.tr(R.string.gps_signal));return}
    var online by remember(track.id) {mutableStateOf(false)}
    Text(c.tr(R.string.online_map_privacy),style=MaterialTheme.typography.bodySmall)
    TextButton(onClick={online=!online},modifier=Modifier.testTag("gps-map-toggle")) {
        Text(c.tr(if(online) R.string.online_map_hide else R.string.online_map_show))
    }
    if(online) {OnlineRouteMap(track);return}
    Text(c.tr(R.string.gps_map),style=MaterialTheme.typography.bodySmall)
    val first=track.points.first()
    val cosLat=cos(Math.toRadians(first.latitude)).coerceAtLeast(0.01)
    val xy=track.points.map {p->
        val dl=((p.longitude-first.longitude+540)%360)-180
        dl*111195*cosLat to (p.latitude-first.latitude)*111195
    }
    val minX=xy.minOf {it.first};val minY=xy.minOf {it.second}
    val spanX=(xy.maxOf {it.first}-minX).coerceAtLeast(50.0)
    val spanY=(xy.maxOf {it.second}-minY).coerceAtLeast(50.0)
    val color=MaterialTheme.colorScheme.primary
    val grid=MaterialTheme.colorScheme.onSurface.copy(alpha=0.18f)
    Canvas(Modifier.fillMaxWidth().height(240.dp).testTag("gps-map")) {
        for(i in 1..3) {drawLine(grid,Offset(size.width*i/4,0f),Offset(size.width*i/4,size.height));drawLine(grid,Offset(0f,size.height*i/4),Offset(size.width,size.height*i/4))}
        val scale=min((size.width-32)/spanX,(size.height-32)/spanY)
        fun at(i:Int)=Offset((16+(xy[i].first-minX)*scale).toFloat(),(size.height-16-(xy[i].second-minY)*scale).toFloat())
        track.points.indices.drop(1).forEach {i->if(track.points[i].segment==track.points[i-1].segment) drawLine(color,at(i-1),at(i),4f)}
        drawCircle(color,6f,at(0));drawCircle(color,8f,at(track.points.lastIndex))
    }
    Text(c.tr(R.string.gps_coordinates,fmtDisplay(first.latitude),fmtDisplay(first.longitude)),style=MaterialTheme.typography.bodySmall)
    Text(c.tr(R.string.gps_extent,fmtDisplay(spanX),fmtDisplay(spanY)),style=MaterialTheme.typography.bodySmall)
}
@Composable internal fun SavedRoute(runId:Long) {
    val c=LocalContext.current
    val route=remember(runId) {LocalStore(c).use {s->s.gpsTracks().firstOrNull {it.runId==runId}}}
    var show by rememberSaveable {mutableStateOf(false)}
    if(route!=null) {
        TextButton(onClick={show=!show}) {Text(c.tr(R.string.gps_route))}
        if(show) RouteMap(route)
    }
}
