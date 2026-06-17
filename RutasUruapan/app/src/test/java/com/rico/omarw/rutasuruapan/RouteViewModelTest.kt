package com.rico.omarw.rutasuruapan

import app.cash.turbine.test
import com.rico.omarw.rutasuruapan.database.Route
import com.rico.omarw.rutasuruapan.database.RouteDAO
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
            Route("Route 1", "R1", "Color1"),
            Route("Route 2", "R2", "Color2")
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
            Route("Zapata", "Z1", "Color1"),
            Route("Centro", "C1", "Color2")
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
}