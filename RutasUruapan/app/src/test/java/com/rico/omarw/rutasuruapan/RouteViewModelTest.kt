package com.rico.omarw.rutasuruapan

import app.cash.turbine.test
import com.google.android.gms.maps.model.Polyline
import com.rico.omarw.rutasuruapan.database.Route
import com.rico.omarw.rutasuruapan.database.RouteDAO
import com.rico.omarw.rutasuruapan.models.RouteModel
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteViewModelTest {

    private val routeDAO: RouteDAO = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init should fetch routes`() = runTest {
        val mockRoutes = listOf(
            Route("Route 1", "Color1", "R1"),
            Route("Route 2", "Color2", "R2")
        )
        coEvery { routeDAO.getRoutes() } returns mockRoutes

        val viewModel = RouteViewModel(routeDAO)

        viewModel.filterableRoutes.test {
            // Initial empty list from MutableStateFlow declaration
            assertEquals(0, awaitItem().size)
            // After fetchRoutes completes
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("Route 1", result[0].name)
            assertEquals("Route 2", result[1].name)
        }
    }

    @Test
    fun `filterRoutes should filter by name`() = runTest {
        val mockRoutes = listOf(
            Route("Zapata", "Color1", "Z1"),
            Route("Centro", "Color2", "C1")
        )
        coEvery { routeDAO.getRoutes() } returns mockRoutes

        val viewModel = RouteViewModel(routeDAO)

        viewModel.filterableRoutes.test {
            assertEquals(0, awaitItem().size) // Initial
            assertEquals(2, awaitItem().size) // After fetch

            viewModel.filterRoutes("zap")
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Zapata", result[0].name)
        }
    }

    @Test
    fun `filterRoutes should match the short name`() = runTest {
        val viewModel = loadedViewModel()

        filter(viewModel, "C1")

        assertEquals(listOf("Centro"), viewModel.filterableRoutes.value.map { it.name })
    }

    @Test
    fun `filterRoutes should ignore case in names and short names`() = runTest {
        val viewModel = loadedViewModel()

        filter(viewModel, "ZAP")
        assertEquals(listOf("Zapata"), viewModel.filterableRoutes.value.map { it.name })

        filter(viewModel, "c1")
        assertEquals(listOf("Centro"), viewModel.filterableRoutes.value.map { it.name })
    }

    @Test
    fun `filterRoutes should return nothing when no route matches`() = runTest {
        val viewModel = loadedViewModel()

        filter(viewModel, "xyz")

        assertTrue(viewModel.filterableRoutes.value.isEmpty())
    }

    @Test
    fun `filterRoutes with an empty query should restore all routes`() = runTest {
        val viewModel = loadedViewModel()

        filter(viewModel, "zap")
        filter(viewModel, "")

        assertEquals(listOf("Zapata", "Centro"), viewModel.filterableRoutes.value.map { it.name })
    }

    @Test
    fun `drawn routes should track added and removed routes`() = runTest {
        val viewModel = loadedViewModel()
        val (zapata, centro) = viewModel.filterableRoutes.value

        viewModel.addDrawnRoute(zapata)
        viewModel.addDrawnRoute(zapata)
        viewModel.addDrawnRoute(centro)
        assertEquals(setOf(zapata, centro), viewModel.drawnRoutes.value)

        viewModel.removeDrawnRoute(zapata)
        assertEquals(setOf(centro), viewModel.drawnRoutes.value)
    }

    @Test
    fun `clearDrawnRoutes should remove routes from the map and empty the set`() = runTest {
        val viewModel = loadedViewModel()
        val polyline = mockk<Polyline>(relaxUnitFun = true)
        val route: RouteModel = viewModel.filterableRoutes.value.first().apply {
            this.polyline = polyline
            isDrawn = true
        }
        viewModel.addDrawnRoute(route)

        viewModel.clearDrawnRoutes()

        verify { polyline.remove() }
        assertFalse(route.isDrawn)
        assertTrue(viewModel.drawnRoutes.value.isEmpty())
    }

    private suspend fun loadedViewModel(): RouteViewModel {
        coEvery { routeDAO.getRoutes() } returns listOf(
            Route("Zapata", "Color1", "Z1"),
            Route("Centro", "Color2", "C1")
        )
        return RouteViewModel(routeDAO).also { viewModel ->
            viewModel.filterableRoutes.first { it.isNotEmpty() }
        }
    }

    private fun TestScope.filter(viewModel: RouteViewModel, query: String) {
        viewModel.filterRoutes(query)
        advanceUntilIdle()
    }
}
