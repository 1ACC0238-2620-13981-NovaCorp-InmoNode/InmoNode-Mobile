package com.novacorp.inmonode_app.features.vouchers

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.novacorp.inmonode_app.core.designsystem.theme.InmoNodeAppTheme
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.*
import com.novacorp.inmonode_app.features.vouchers.application.GetPaymentEvidencesUseCase
import com.novacorp.inmonode_app.features.vouchers.domain.*
import com.novacorp.inmonode_app.features.vouchers.presentation.separations.SeparationDetailScreen
import com.novacorp.inmonode_app.features.vouchers.presentation.separations.SeparationListScreen
import com.novacorp.inmonode_app.features.vouchers.presentation.separations.SeparationsViewModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

/** The verification deadline of a separation says what it is: until when the lot stays held. */
class SeparationHoldDateTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val until = Instant.now().plus(20, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS).toString()

    private val lot = Lot(10, 1, "N-02", LotDimensions(BigDecimal("9"), BigDecimal("15"), BigDecimal("135")), emptyList(),
        BigDecimal("50500"), "PEN", LotStatus.PENDING_VERIFICATION)
    private val project = Project(1, "Residencial Las Lomas de Lurín", "Lurín, Lima", null, null, null,
        FinancingRules(BigDecimal("10"), BigDecimal("12.5"), 120, BigDecimal("2")), listOf(lot))
    private val synced = Reservation("r1", 10, "p1", BigDecimal("500"), "2026-11-19T19:00:00Z", ReservationStatus.SYNCED,
        blockedUntil = until, serverStatus = "PENDING_VERIFICATION")

    private val portfolio = object : PortfolioRepository {
        override fun observePortfolio() = flowOf(Portfolio(listOf(project), "2026-11-19", null))
        override suspend fun download() = Result.success(Portfolio(listOf(project), "2026-11-19", null))
    }
    private val prospects = object : ProspectRepository {
        override fun observeProspects() = flowOf(listOf(Prospect("p1", "59000059", "Prueba Sync", "", "Soltero(a)",
            "2026-11-19T18:55:00Z", synced = true)))
        override suspend fun completeMaritalStatus(prospectId: String, maritalStatus: String): Result<Prospect> = error("unused")
        override suspend fun register(document: String, fullName: String, phone: String, maritalStatus: String?): Result<Prospect> = error("unused")
    }
    private val reservations = object : ReservationRepository {
        override fun observeReservations() = flowOf(listOf(synced))
        override suspend fun createDraft(lotId: Long, prospectId: String, initialAmount: BigDecimal, terms: NegotiationTerms): Result<Reservation> = error("unused")
        override suspend fun confirmDraft(reservationId: String): Result<Reservation> = error("unused")
        override suspend fun cancelDraft(reservationId: String): Result<Unit> = error("unused")
        override suspend fun reassign(reservationId: String, newLotId: Long): Result<Reservation> = error("unused")
    }
    private val vouchers = object : VoucherRepository {
        override suspend fun getPaymentEvidences(reservationId: String) = Result.success(PaymentEvidences("t1", "FIELD",
            "PENDING_VERIFICATION", BigDecimal("500"), "PEN", until, emptyList()))
        override fun observeVouchers() = flowOf(emptyList<Voucher>())
        override suspend fun discardCancelled() = Result.success(Unit)
        override suspend fun upload(voucherId: String): Result<Voucher> = error("unused")
        override suspend fun capture(reservationId: String, imagePath: String): Result<Voucher> = error("unused")
        override suspend fun processOcr(voucherId: String): Result<Voucher> = error("unused")
        override suspend fun review(voucherId: String, data: OcrData): Result<Voucher> = error("unused")
    }
    private val identityDocuments = object : IdentityDocumentRepository {
        override fun observeDocuments() = flowOf(emptyList<IdentityDocument>())
        override suspend fun capture(reservationId: String, imagePath: String): Result<IdentityDocument> = error("unused")
        override suspend fun upload(documentId: String): Result<IdentityDocument> = error("unused")
        override suspend fun discardCancelled(): Result<Unit> = error("unused")
    }
    private fun viewModel(saved: SavedStateHandle = SavedStateHandle()) =
        SeparationsViewModel(reservations, prospects, portfolio, identityDocuments, GetPaymentEvidencesUseCase(vouchers), saved)

    @Test fun separationListLabelsTheHoldDate() {
        val viewModel = viewModel()
        compose.setContent { InmoNodeAppTheme { SeparationListScreen(viewModel = viewModel, onProspects = {}, onDetail = {}) } }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Lote bloqueado hasta", substring = true).fetchSemanticsNodes().isNotEmpty() }
        screenshot("separations-hold-list")
    }

    @Test fun separationDetailLabelsTheHoldDateUnderTheCountdown() {
        val viewModel = viewModel(SavedStateHandle(mapOf("reservationId" to "r1")))
        compose.setContent { InmoNodeAppTheme { SeparationDetailScreen(viewModel = viewModel, onBack = {}, onReplace = {}, onContract = {}, onIdentityDocument = {}) } }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Lote bloqueado hasta", substring = true).fetchSemanticsNodes().isNotEmpty() }
        screenshot("separations-hold-detail")
    }

    /** Saved in the app cache so a reviewer can pull it with run-as. */
    private fun screenshot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.cacheDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
