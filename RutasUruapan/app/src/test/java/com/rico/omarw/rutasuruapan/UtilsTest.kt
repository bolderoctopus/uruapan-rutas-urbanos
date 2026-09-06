package com.rico.omarw.rutasuruapan

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UtilsTest {

    @Test
    fun `formatRouteTitle should format correctly`() {
        val name = "Route A"
        val shortName = "10"
        val result = Utils.formatRouteTitle(name, shortName)
        
        assertEquals("Route A #10", result.toString())
    }

    @Test
    fun `getSquareFrom should return 4 points`() {
        val center = LatLng(19.4326, -102.0621)
        val distance = 0.001
        val points = getSquareFrom(distance, center)
        
        assertEquals(4, points.size)
        // Verify one of the points
        assertEquals(center.latitude - distance, points[0].latitude, 0.0001)
        assertEquals(center.longitude + distance, points[0].longitude, 0.0001)
    }
}