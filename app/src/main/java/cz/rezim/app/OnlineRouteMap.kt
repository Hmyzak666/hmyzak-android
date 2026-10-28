package cz.rezim.app

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

/** Normalised Web Mercator; wrapped x preserves routes crossing the date line. */
internal object RouteProjection {
    fun point(latitude:Double,longitude:Double):Pair<Double,Double> {
        val lat=Math.toRadians(latitude.coerceIn(-85.05112878,85.05112878))
        return (longitude+180)/360 to ((1-ln(tan(lat)+1/cos(lat))/Math.PI)/2).coerceIn(0.0,1.0)
    }
    fun near(x:Double,reference:Double)=reference+((x-reference+1.5)%1.0)-0.5
    fun tileX(x:Int,zoom:Int)=Math.floorMod(x,1 shl zoom)
    fun fitZoom(spanX:Double,spanY:Double,width:Int,height:Int):Int {
        val scale=min((width-48).coerceAtLeast(1)/spanX.coerceAtLeast(0.000001),
            (height-48).coerceAtLeast(1)/spanY.coerceAtLeast(0.000001))
        return floor(log2(scale/256)).toInt().coerceIn(1,17)
    }
}
internal data class MapTile(val zoom:Int,val x:Int,val y:Int)
/** Only visible tiles, serial requests, explicit UA, seven-day disk cache; never backed up. */
internal class RouteTiles(context:Context) {
    private val directory=File(context.cacheDir,"osm-route-tiles-v1").apply {mkdirs()}
    suspend fun load(tile:MapTile):ImageBitmap?=withContext(Dispatchers.IO) {
        val file=File(directory,"${tile.zoom}-${tile.x}-${tile.y}.png")
        val cached=if(file.exists()) BitmapFactory.decodeFile(file.path) else null
        if(cached!=null && System.currentTimeMillis()-file.lastModified()<7*24*60*60*1000L) return@withContext cached.asImageBitmap()
        ensureActive()
        var connection:HttpURLConnection?=null
        try {
            connection=URL("https://tile.openstreetmap.org/${tile.zoom}/${tile.x}/${tile.y}.png").openConnection() as HttpURLConnection
            connection.connectTimeout=5000;connection.readTimeout=5000
            connection.instanceFollowRedirects=false
            connection.setRequestProperty("User-Agent","LiftAnt/${BuildConfig.VERSION_NAME} (https://github.com/Hmyzak666/hmyzak-android)")
            if(file.exists()) connection.ifModifiedSince=file.lastModified()
            if(connection.responseCode==304 && cached!=null) {file.setLastModified(System.currentTimeMillis());return@withContext cached.asImageBitmap()}
            if(connection.responseCode!=200) return@withContext cached?.asImageBitmap()
            val bytes=connection.inputStream.use {input->
                val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                while(true) {ensureActive();val n=input.read(buffer);if(n<0) break;require(out.size()+n<=1024*1024);out.write(buffer,0,n)}
                out.toByteArray()
            }
            ensureActive()
            val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size) ?: return@withContext cached?.asImageBitmap()
            if(bitmap.width!=256 || bitmap.height!=256) return@withContext cached?.asImageBitmap()
            // Cache is bounded; never delete the user's recorded routes or backups.
            val files=directory.listFiles().orEmpty()
            if(files.sumOf {it.length()}>64L*1024*1024) files.sortedBy {it.lastModified()}.take(128).forEach {it.delete()}
            file.writeBytes(bytes)
            bitmap.asImageBitmap()
        } catch(e:CancellationException) {throw e}
        catch(_:Exception) {cached?.asImageBitmap()}
        finally {connection?.disconnect()}
    }
}

@Composable internal fun OnlineRouteMap(track:GpsTrack) {
    val c=LocalContext.current
    val uri=LocalUriHandler.current
    val owner=LocalLifecycleOwner.current
    var resumed by remember {mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))}
    DisposableEffect(owner) {
        val observer=LifecycleEventObserver {_,_->resumed=owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)}
        owner.lifecycle.addObserver(observer)
        onDispose {owner.lifecycle.removeObserver(observer)}
    }
    val points=remember(track.points) {
        val reference=RouteProjection.point(track.points.first().latitude,track.points.first().longitude).first
        track.points.map {p->RouteProjection.point(p.latitude,p.longitude).let {(x,y)->RouteProjection.near(x,reference) to y}}
    }
    var dimensions by remember {mutableStateOf(IntSize.Zero)}
    var center by remember(track.id) {mutableStateOf(points.first())}
    var zoom by remember(track.id) {mutableIntStateOf(15)}
    var fitted by remember(track.id) {mutableStateOf(false)}
    fun fit() {
        if(dimensions.width==0) return
        val loX=points.minOf {it.first};val hiX=points.maxOf {it.first}
        val loY=points.minOf {it.second};val hiY=points.maxOf {it.second}
        center=(loX+hiX)/2 to (loY+hiY)/2
        zoom=RouteProjection.fitZoom(hiX-loX,hiY-loY,dimensions.width,dimensions.height)
        fitted=true
    }
    LaunchedEffect(dimensions) {if(!fitted) fit()}
    val world=256.0*(1 shl zoom)
    val left=center.first*world-dimensions.width/2.0
    val top=center.second*world-dimensions.height/2.0
    val tiles=remember(zoom,left,top,dimensions) {
        if(dimensions.width==0) emptyList() else buildList {
            for(y in floor(top/256).toInt()..floor((top+dimensions.height-1)/256).toInt())
                for(x in floor(left/256).toInt()..floor((left+dimensions.width-1)/256).toInt())
                    if(y in 0 until (1 shl zoom)) add(x to MapTile(zoom,RouteProjection.tileX(x,zoom),y))
        }
    }
    val loader=remember(c) {RouteTiles(c)}
    var images by remember {mutableStateOf<Map<MapTile,ImageBitmap>>(emptyMap())}
    var unavailable by remember {mutableStateOf(false)}
    var loading by remember {mutableStateOf(false)}
    var retry by remember {mutableIntStateOf(0)}
    LaunchedEffect(tiles,resumed,retry) {
        if(!resumed) return@LaunchedEffect
        delay(250) // settle pan/zoom; no speculative loading
        loading=true;unavailable=false
        val visible=tiles.map {it.second}.toSet()
        images=images.filterKeys {it in visible}
        for(tile in visible) {
            ensureActive()
            if(images[tile]!=null) continue
            val bitmap=loader.load(tile)
            if(bitmap!=null) images=images+(tile to bitmap) else unavailable=true
        }
        loading=false
    }
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick={zoom=(zoom-1).coerceAtLeast(1)},enabled=zoom>1) {Text("−")}
        OutlinedButton(onClick={zoom=(zoom+1).coerceAtMost(19)},enabled=zoom<19) {Text("+")}
        TextButton(onClick={fit()}) {Text(c.tr(R.string.online_map_fit))}
    }
    Canvas(Modifier.fillMaxWidth().height(280.dp).clipToBounds().testTag("gps-online-map")
        .onSizeChanged {dimensions=it}
        .pointerInput(zoom) {detectDragGestures {change,drag->
            change.consume()
            center=(center.first-drag.x/world) to (center.second-drag.y/world).coerceIn(0.0,1.0)
        }}) {
        drawRect(Color(0xffe2e6df))
        tiles.forEach {(rawX,tile)->images[tile]?.let {bitmap->
            drawImage(bitmap,dstOffset=IntOffset((rawX*256-left).roundToInt(),(tile.y*256-top).roundToInt()),dstSize=IntSize(256,256))
        }}
        fun at(i:Int)=Offset((points[i].first*world-left).toFloat(),(points[i].second*world-top).toFloat())
        points.indices.drop(1).forEach {i->if(track.points[i].segment==track.points[i-1].segment) {
            drawLine(Color.White,at(i-1),at(i),8f);drawLine(Color(0xff1769d2),at(i-1),at(i),4f)
        }}
        drawCircle(Color.White,10f,at(0));drawCircle(Color(0xff087f23),7f,at(0))
        drawCircle(Color.White,10f,at(points.lastIndex));drawCircle(Color(0xffb71c1c),7f,at(points.lastIndex))
    }
    // Attribution stays outside the pannable canvas and is visible in every viewport.
    TextButton(onClick={uri.openUri("https://www.openstreetmap.org/copyright")}) {Text("© OpenStreetMap contributors")}
    if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    if(unavailable) {
        Text(c.tr(R.string.online_map_unavailable),style=MaterialTheme.typography.bodySmall)
        TextButton(onClick={retry++}) {Text(c.tr(R.string.online_map_retry))}
    }
}
