package cz.rezim.app
import org.junit.Assert.*
import org.junit.Test
class RouteProjectionTest {
    @Test fun equatorAndPrimeMeridianAreCentered() {
        val (x,y)=RouteProjection.point(0.0,0.0)
        assertEquals(0.5,x,0.000001);assertEquals(0.5,y,0.000001)
    }
    @Test fun polesAreClampedToFiniteMapBounds() {
        for(lat in listOf(-90.0,90.0)) {
            val y=RouteProjection.point(lat,14.0).second
            assertTrue(y.isFinite() && y in 0.0..1.0)
        }
    }
    @Test fun dateLineDoesNotDrawAcrossWholeWorld() {
        val a=RouteProjection.point(0.0,179.9).first
        val b=RouteProjection.near(RouteProjection.point(0.0,-179.9).first,a)
        assertEquals(0.2/360,b-a,0.000001)
        assertEquals(7,RouteProjection.tileX(-1,3))
        assertEquals(0,RouteProjection.tileX(8,3))
    }
    @Test fun fitFitsBothPhoneAndTabletAndSinglePoint() {
        for(width in listOf(320,1200)) {
            val zoom=RouteProjection.fitZoom(0.01,0.005,width,280)
            assertTrue(0.01*256*(1 shl zoom)<=width-48)
            assertTrue(0.005*256*(1 shl zoom)<=280-48)
        }
        assertEquals(17,RouteProjection.fitZoom(0.0,0.0,320,280))
    }
}
