package cz.rezim.app
import android.graphics.Bitmap
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class RouteTileCacheTest {
    @Test fun freshTileUsesDiskWithoutNetwork()=runBlocking {
        val context=RuntimeEnvironment.getApplication()
        val directory=File(context.cacheDir,"osm-route-tiles-v1").apply {mkdirs()}
        val file=File(directory,"3-4-4.png")
        val bitmap=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888)
        file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        val timestamp=file.lastModified()
        val result=RouteTiles(context).load(MapTile(3,4,4))
        assertNotNull(result);assertEquals(256,result!!.width)
        assertEquals(timestamp,file.lastModified())
    }
}
