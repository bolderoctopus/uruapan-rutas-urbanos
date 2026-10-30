package com.rico.omarw.rutasuruapan.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Runs the real SQL in [RouteDAO] against an in-memory Room database.
 *
 * Test route ("loop"): an out-and-back line. Points are 0.001° of latitude apart and
 * every segment is 100 m long. The way back runs 0.0001° east of the way out.
 *
 *   lat     out (lng 0)    back (lng 0.0001)
 *   0.004   #5
 *   0.003   #4             #6
 *   0.002   #3             #7
 *   0.001   #2             #8
 *   0.000   #1             #9  (then back to #1)
 */
@RunWith(RobolectricTestRunner::class)
class RouteDAOTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: RouteDAO
    private var loopId = 0L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.routesDAO()

        loopId = dao.insertRoute(Route("Loop", "#FF0000", "L"))
        dao.insertPoints(
            listOf(
                point(loopId, 1, 0.000, 0.0),
                point(loopId, 2, 0.001, 0.0),
                point(loopId, 3, 0.002, 0.0),
                point(loopId, 4, 0.003, 0.0),
                point(loopId, 5, 0.004, 0.0),
                point(loopId, 6, 0.003, BACK_LANE),
                point(loopId, 7, 0.002, BACK_LANE),
                point(loopId, 8, 0.001, BACK_LANE),
                point(loopId, 9, 0.000, BACK_LANE)
            )
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `getRoutes and getRoute return inserted routes`() = runTest {
        val farId = dao.insertRoute(Route("Far", "#00FF00", "F"))

        assertEquals(listOf("Loop", "Far"), dao.getRoutes().map { it.name })
        assertEquals("Far", dao.getRoute(farId).name)
    }

    @Test
    fun `getPointsFrom returns only that route's points ordered by number`() {
        val otherId = dao.insertRoute(Route("Other", "#0000FF", "O"))
        dao.insertPoints(listOf(point(otherId, 2, 1.0, 1.0), point(otherId, 1, 1.0, 1.0)))

        assertEquals((1..9).toList(), dao.getPointsFrom(loopId).map { it.number })
        assertEquals(listOf(1, 2), dao.getPointsFrom(otherId).map { it.number })
    }

    @Test
    fun `getRoutesIntercepting returns each nearby route once and ignores far ones`() = runTest {
        val farId = dao.insertRoute(Route("Far", "#00FF00", "F"))
        dao.insertPoints(listOf(point(farId, 1, 1.0, 1.0)))

        // Box around lat 0.002 contains points #3 and #7 of the loop: still one result.
        assertEquals(listOf(loopId), dao.getRoutesIntercepting(0.0005, 0.002, 0.0))
        assertEquals(listOf(farId), dao.getRoutesIntercepting(0.0005, 1.0, 1.0))
        assertEquals(emptyList<Long>(), dao.getRoutesIntercepting(0.0005, 0.5, 0.5))
    }

    @Test
    fun `getNearestPointTo picks the closest point inside the search box`() = runTest {
        // #4 is 0.0002° away; #6 is slightly further because of the back lane offset.
        val nearest = dao.getNearestPointTo(loopId, 0.0032, 0.0, 0.0005)

        assertEquals(4, nearest.number)
    }

    @Test
    fun `getRouteDist sums segments between start and end`() = runTest {
        assertEquals(400, dao.getRouteDist(loopId, 1, 4))
    }

    @Test
    fun `getRouteDist wraps around the end of the route when start is after end`() = runTest {
        // Ride #8 -> #9 -> #1 -> #2: segments of points 8, 9, 1 and 2.
        assertEquals(400, dao.getRouteDist(loopId, 8, 2))
    }

    @Test
    fun `findBestEndPoint prefers the stop reached sooner when two are equally close`() = runTest {
        // Destination sits exactly between #4 (way out) and #6 (way back).
        // Boarding at #1, #4 is a shorter ride than #6.
        val best = dao.findBestEndPoint(loopId, 0.003, BACK_LANE / 2, startPoint = 1, distance = 0.0005)

        assertEquals(4, best.number)
    }

    @Test
    fun `findBestStartPoint prefers the stop with the shorter ride to the end point`() = runTest {
        // Origin sits exactly between #2 (way out) and #8 (way back).
        // Getting off at #5: from #2 it's #2..#5, from #8 it wraps around #8, #9, #1..#5.
        val best = dao.findBestStartPoint(loopId, 0.001, BACK_LANE / 2, endPoint = 5, distance = 0.0005)

        assertEquals(2, best.number)
    }

    @Test
    fun `findBest clears previous results between searches`() = runTest {
        dao.findBestEndPoint(loopId, 0.003, BACK_LANE / 2, startPoint = 1, distance = 0.0005)
        val second = dao.findBestEndPoint(loopId, 0.000, 0.0, startPoint = 3, distance = 0.0005)

        // Only #1 and #9 are near the second destination; #4 from the first search must be gone.
        assertEquals(9, second.number)
    }

    @Test
    fun `deleteAll removes points and routes`() = runTest {
        dao.deleteAllPoints()
        dao.deleteAllRoutes()

        assertEquals(emptyList<Point>(), dao.getPointsFrom(loopId))
        assertEquals(emptyList<Route>(), dao.getRoutes())
    }

    private fun point(routeId: Long, number: Int, lat: Double, lng: Double) =
        Point(routeId, lat, lng, number, distanceToNextPoint = 100)

    private companion object {
        const val BACK_LANE = 0.0001
    }
}
