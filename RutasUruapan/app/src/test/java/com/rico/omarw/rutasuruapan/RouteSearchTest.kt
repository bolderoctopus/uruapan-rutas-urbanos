package com.rico.omarw.rutasuruapan

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.maps.model.LatLng
import com.rico.omarw.rutasuruapan.database.AppDatabase
import com.rico.omarw.rutasuruapan.database.Point
import com.rico.omarw.rutasuruapan.database.Route
import com.rico.omarw.rutasuruapan.database.RouteDAO
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Runs [RouteSearch] against an in-memory Room database.
 *
 * Routes run north along a longitude, with points 0.001° of latitude apart and 100 m segments.
 * "Loop" is the out-and-back route from RouteDAOTest: out along lng 0, back along lng 0.0001.
 */
@RunWith(RobolectricTestRunner::class)
class RouteSearchTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: RouteDAO

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.routesDAO()

        val loopId = dao.insertRoute(Route("Loop", "#FF0000", "L"))
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
    fun `returns routes near origin and destination sorted by walk distance`() = runTest {
        // Runs 0.0002° east of the loop, so riding it means walking further.
        val lineId = dao.insertRoute(Route("Line", "#00FF00", "N"))
        dao.insertPoints((1..5).map { point(lineId, it, (it - 1) * 0.001, 0.0002) })

        val results = RouteSearch.findRoutes(dao, LatLng(0.0, 0.0), LatLng(0.004, 0.0), walkDistLimit = 0.00025)

        assertEquals(listOf("Loop", "Line"), results.map { it.name })
        assertEquals(listOf(1, 1), results.map { it.startPoint?.number })
        assertEquals(listOf(5, 5), results.map { it.endPoint?.number })
        assertEquals(0.0, results[0].walkDist!!, DELTA)
        assertEquals(0.0004, results[1].walkDist!!, DELTA)
        // Riding #1..#5 adds up 5 segments of 100 m.
        assertEquals(500.0, results[0].totalDist!!, DELTA)
        assertEquals(500.0004, results[1].totalDist!!, DELTA)
    }

    @Test
    fun `skips a route with no best end point and keeps the others`() = runTest {
        // Origin and destination are both near loop point #5 and nothing else on the loop, so there is
        // no stop to ride to after boarding at #5. The short route has a stop near each of them.
        val shortId = dao.insertRoute(Route("Short", "#0000FF", "S"))
        dao.insertPoints(listOf(point(shortId, 1, 0.004, -0.00002), point(shortId, 2, 0.004, 0.00002)))

        val results = RouteSearch.findRoutes(
            dao,
            LatLng(0.004, -0.00002),
            LatLng(0.004, 0.00002),
            walkDistLimit = 0.00005
        )

        assertEquals(listOf("Short"), results.map { it.name })
        assertEquals(1, results[0].startPoint?.number)
        assertEquals(2, results[0].endPoint?.number)
    }

    @Test
    fun `returns no routes when the only candidate can't be resolved`() = runTest {
        val results = RouteSearch.findRoutes(
            dao,
            LatLng(0.004, -0.00002),
            LatLng(0.004, 0.00002),
            walkDistLimit = 0.00005
        )

        assertEquals(emptyList<String>(), results.map { it.name })
    }

    @Test
    fun `returns no routes when the database is unavailable`() = runTest {
        val results = RouteSearch.findRoutes(null, LatLng(0.0, 0.0), LatLng(0.004, 0.0), walkDistLimit = 0.00025)

        assertEquals(emptyList<String>(), results.map { it.name })
    }

    @Test
    fun `skips routes whose route row or any start or end point is missing`() = runTest {
        // Route 1 resolves fully. Routes 2 to 6 each miss one lookup, which Room returns as null.
        val mockDao = mockk<RouteDAO>()
        coEvery { mockDao.getRoutesIntercepting(any(), any(), any()) } returns (1L..6L).toList()
        coEvery { mockDao.getRoute(any()) } answers { route(firstArg()) }
        coEvery { mockDao.getRoute(2) } returns null
        coEvery { mockDao.getNearestPointTo(any(), any(), any(), any()) } answers { point(firstArg(), 1, 0.0, 0.0) }
        coEvery { mockDao.getNearestPointTo(3, ORIGIN.latitude, any(), any()) } returns null
        coEvery { mockDao.getNearestPointTo(4, DESTINATION.latitude, any(), any()) } returns null
        coEvery { mockDao.findBestStartPoint(any(), any(), any(), any(), any()) } answers {
            point(firstArg(), 1, 0.0, 0.0)
        }
        coEvery { mockDao.findBestStartPoint(5, any(), any(), any(), any()) } returns null
        coEvery { mockDao.findBestEndPoint(any(), any(), any(), any(), any()) } answers {
            point(firstArg(), 2, 0.004, 0.0)
        }
        coEvery { mockDao.findBestEndPoint(6, any(), any(), any(), any()) } returns null
        coEvery { mockDao.getRouteDist(any(), any(), any()) } returns 100

        val results = RouteSearch.findRoutes(mockDao, ORIGIN, DESTINATION, walkDistLimit = 0.00025)

        assertEquals(listOf(1L), results.map { it.id })
        assertEquals(100.0, results[0].totalDist!!, DELTA)
    }

    private fun route(id: Long) = Route("Route $id", "#FF0000", "$id").apply { routeId = id }

    private fun point(routeId: Long, number: Int, lat: Double, lng: Double) =
        Point(routeId, lat, lng, number, distanceToNextPoint = 100)

    private companion object {
        const val BACK_LANE = 0.0001
        const val DELTA = 1e-9
        val ORIGIN = LatLng(0.0, 0.0)
        val DESTINATION = LatLng(0.004, 0.0)
    }
}
