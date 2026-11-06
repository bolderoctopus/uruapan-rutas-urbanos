package com.rico.omarw.rutasuruapan.models

import android.util.SparseArray
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.rico.omarw.rutasuruapan.database.Point
import com.rico.omarw.rutasuruapan.database.Route
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Route points are numbered around a loop, and each point's latitude equals its number,
 * so segments can be compared by latitude. Riding past the last point continues at the first.
 */
@RunWith(RobolectricTestRunner::class)
class RouteModelTest {

    private val points = (1..6).map { point(it) }.shuffled()

    @Test
    fun `main segment runs from start to end in point order`() {
        val model = routeModel(start = 2, end = 4)

        assertEquals(listOf(2.0, 3.0, 4.0), model.getMainSegment(points).map { it.latitude })
    }

    @Test
    fun `main segment wraps around the end of the route when start is after end`() {
        val model = routeModel(start = 5, end = 2)

        assertEquals(listOf(5.0, 6.0, 1.0, 2.0), model.getMainSegment(points).map { it.latitude })
    }

    @Test
    fun `secondary segment is the rest of the loop, from end back to start`() {
        val model = routeModel(start = 2, end = 4)

        assertEquals(listOf(4.0, 5.0, 6.0, 1.0, 2.0), model.getSecondarySegment(points).map { it.latitude })
    }

    @Test
    fun `main segment points follow the same order as the segment`() {
        assertEquals(listOf(2, 3, 4), routeModel(start = 2, end = 4).getMainSegmentPoints(points).map { it.number })
        assertEquals(listOf(5, 6, 1, 2), routeModel(start = 5, end = 2).getMainSegmentPoints(points).map { it.number })
    }

    @Test
    fun `segments are empty without a start or end point`() {
        val model = RouteModel(route())

        assertTrue(model.getMainSegment(points).none())
        assertTrue(model.getSecondarySegment(points).none())
        assertTrue(model.getMainSegmentPoints(points).isEmpty())
    }

    @Test
    fun `setVisibility updates map objects and isDrawn`() {
        val polyline = mockk<Polyline>(relaxUnitFun = true)
        val marker = mockk<Marker>(relaxUnitFun = true)
        val model = RouteModel(route()).apply {
            this.polyline = polyline
            mainSegmentMarkers = listOf(marker)
        }

        model.setVisibility(true)
        assertTrue(model.isDrawn)

        model.setVisibility(false)
        assertFalse(model.isDrawn)
        verify { polyline.isVisible = false }
        verify { marker.isVisible = false }
    }

    @Test
    fun `remove clears every map object and isDrawn`() {
        val mainSegment = mockk<Polyline>(relaxUnitFun = true)
        val secondarySegment = mockk<Polyline>(relaxUnitFun = true)
        val segmentMarker = mockk<Marker>(relaxUnitFun = true)
        val arrow = mockk<Marker>(relaxUnitFun = true)
        val model = RouteModel(route()).apply {
            this.mainSegment = mainSegment
            this.secondarySegment = secondarySegment
            mainSegmentMarkers = listOf(segmentMarker)
            directionalMarkers = SparseArray<Iterable<Marker>>().apply { put(13, listOf(arrow)) }
            isDrawn = true
        }

        model.remove()

        assertFalse(model.isDrawn)
        verify { mainSegment.remove() }
        verify { secondarySegment.remove() }
        verify { segmentMarker.remove() }
        verify { arrow.remove() }
    }

    @Test
    fun `models are equal when id, name and color match`() {
        assertEquals(RouteModel(route()), RouteModel(route()))
        assertNotEquals(RouteModel(route()), RouteModel(route().apply { routeId = 2 }))
        assertNotEquals(RouteModel(route()), RouteModel(route().apply { color = "#000000" }))
    }

    private fun routeModel(start: Int, end: Int) = RouteModel(route()).apply {
        startPoint = point(start)
        endPoint = point(end)
    }

    private fun route() = Route("Centro", "#FF0000", "1").apply { routeId = 1 }

    private fun point(number: Int) = Point(1, number.toDouble(), 0.0, number, distanceToNextPoint = 100)
}
