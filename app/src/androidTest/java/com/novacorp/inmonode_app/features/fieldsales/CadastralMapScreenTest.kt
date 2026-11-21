package com.novacorp.inmonode_app.features.fieldsales

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.novacorp.inmonode_app.core.connectivity.ConnectivityMonitor
import com.novacorp.inmonode_app.core.designsystem.theme.InmoNodeAppTheme
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.PortfolioRepository
import com.novacorp.inmonode_app.features.fieldsales.presentation.map.CadastralMapScreen
import com.novacorp.inmonode_app.features.fieldsales.presentation.map.CadastralMapViewModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.math.BigDecimal

/** The map key shows each lot state with its colour, and the centre control reads as an active button (mock-up M06). */
class CadastralMapScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun lot(id: Long, code: String, column: Int, status: LotStatus, pending: String? = null): Lot {
        val west = -76.87 + column * 0.0001
        val square = listOf(GeoPoint(west, -12.25), GeoPoint(west + 0.0001, -12.25),
            GeoPoint(west + 0.0001, -12.2501), GeoPoint(west, -12.2501), GeoPoint(west, -12.25))
        return Lot(id, 1, code, LotDimensions(BigDecimal("9"), BigDecimal("15"), BigDecimal("135")), listOf(square),
            BigDecimal("50500"), "PEN", status, pendingReservationId = pending)
    }

    private val project = Project(1, "Residencial Las Lomas de Lurín", "Lurín, Lima", null, null, null,
        FinancingRules(BigDecimal("10"), BigDecimal("12.5"), 120, BigDecimal("2")),
        listOf(lot(1, "N-01", 0, LotStatus.AVAILABLE), lot(2, "N-02", 1, LotStatus.AVAILABLE, pending = "r1"),
            lot(3, "N-03", 2, LotStatus.SOLD)))
    private val portfolio = object : PortfolioRepository {
        override fun observePortfolio() = flowOf(Portfolio(listOf(project), "2026-11-20", null))
        override suspend fun download() = Result.success(Portfolio(listOf(project), "2026-11-20", null))
    }

    @Test fun legendShowsEachStateAndTheCentreControlIsAnActiveButton() {
        val viewModel = CadastralMapViewModel(portfolio, ConnectivityMonitor(context))
        compose.setContent { InmoNodeAppTheme {
            CadastralMapScreen(viewModel = viewModel, onDownload = {}, onSimulate = {}, onReserve = {})
        } }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Disponible").fetchSemanticsNodes().isNotEmpty() }
        listOf("Disponible", "Separado", "Vendido").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        compose.onNodeWithContentDescription("Centrar mapa").assertIsDisplayed().assertIsEnabled().assertHasClickAction()
            .performClick()
        screenshot("map-legend")
    }

    /** Saved in the app cache so a reviewer can pull it with run-as. */
    private fun screenshot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.cacheDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
