package com.novacorp.inmonode_app.features.fieldsales

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.novacorp.inmonode_app.core.connectivity.ConnectivityMonitor
import com.novacorp.inmonode_app.core.designsystem.theme.InmoNodeAppTheme
import com.novacorp.inmonode_app.features.fieldsales.application.*
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.*
import com.novacorp.inmonode_app.features.fieldsales.presentation.sync.SyncQueueScreen
import com.novacorp.inmonode_app.features.fieldsales.presentation.sync.SyncQueueViewModel
import com.novacorp.inmonode_app.features.iam.domain.AuthRepository
import com.novacorp.inmonode_app.features.iam.domain.User
import com.novacorp.inmonode_app.features.iam.domain.UserRole
import com.novacorp.inmonode_app.features.vouchers.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.math.BigDecimal

/** "Sincronizar ahora" reports what it sent, and a synced separation says until when its lot is held (US-11, M24). */
class SyncQueueScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val lot = Lot(10, 1, "N-02", LotDimensions(BigDecimal("9"), BigDecimal("15"), BigDecimal("135")), emptyList(),
        BigDecimal("50500"), "PEN", LotStatus.PENDING_VERIFICATION)
    private val project = Project(1, "Residencial Las Lomas de Lurín", "Lurín, Lima", null, null, null,
        FinancingRules(BigDecimal("10"), BigDecimal("12.5"), 120, BigDecimal("2")), listOf(lot))
    private val synced = Reservation("r1", 10, "p1", BigDecimal("500"), "2026-11-19T19:00:00Z", ReservationStatus.SYNCED,
        blockedUntil = "2026-11-20T19:00:00Z")

    private val auth = object : AuthRepository {
        override val currentUser = MutableStateFlow<User?>(User(1, "agent@example.test", UserRole.FIELD_AGENT))
        override suspend fun signIn(email: String, password: String): Result<User> = error("unused")
        override suspend fun signOut() = Unit
    }
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
    private fun vouchers(vararg pending: Voucher) = object : VoucherRepository {
        override fun observeVouchers() = flowOf(pending.toList())
        override suspend fun upload(voucherId: String) = Result.success(pending.first { it.id == voucherId }.copy(status = VoucherStatus.SYNCED))
        override suspend fun discardCancelled() = Result.success(Unit)
        override suspend fun capture(reservationId: String, imagePath: String): Result<Voucher> = error("unused")
        override suspend fun processOcr(voucherId: String): Result<Voucher> = error("unused")
        override suspend fun review(voucherId: String, data: OcrData): Result<Voucher> = error("unused")
        override suspend fun getPaymentEvidences(reservationId: String): Result<PaymentEvidences> = error("unused")
    }
    private val background = object : BackgroundSync {
        override fun schedule() = Unit
        override val state = MutableStateFlow<BackgroundSyncState>(BackgroundSyncState.Idle)
    }

    private fun showQueue(serverSummary: SyncSummary, receipts: VoucherRepository) {
        val fields = object : FieldSyncRepository { override suspend fun synchronize() = Result.success(serverSummary) }
        val viewModel = SyncQueueViewModel(reservations, prospects, receipts, portfolio, ConnectivityMonitor(context),
            SyncFieldRecordsUseCase(fields, reservations, receipts, auth), background, ResolveConflictUseCase(reservations), SavedStateHandle())
        compose.setContent { InmoNodeAppTheme { SyncQueueScreen(viewModel = viewModel, onConflict = {}, onDraft = {}) } }
        compose.waitUntil(10_000) {
            runCatching { compose.onNodeWithText("Sincronizar ahora").assertIsEnabled() }.isSuccess
        }
    }

    @Test fun recordsAlreadySentAutomaticallyAreNotReportedAsZeroSent() {
        showQueue(SyncSummary(0, 0, 0), vouchers())
        compose.onNodeWithText("Lote bloqueado hasta", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Sincronizar ahora").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("No hay registros pendientes: todo ya está sincronizado.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("0 prospectos", substring = true).assertDoesNotExist()
        screenshot("sync-nothing-pending")
    }

    @Test fun summaryNamesEachKindOfRecordSentIncludingVouchers() {
        val voucher = Voucher("v1", "r1", "voucher.jpg", status = VoucherStatus.READY_TO_SYNC, capturedAt = "2026-11-19T19:05:00Z")
        showQueue(SyncSummary(1, 1, 0), vouchers(voucher))
        compose.onNodeWithText("Sincronizar ahora").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Enviados al servidor: 1 prospecto, 1 separación y 1 voucher.").fetchSemanticsNodes().isNotEmpty()
        }
        screenshot("sync-summary")
    }

    /** Saved in the app cache so a reviewer can pull it with run-as. */
    private fun screenshot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.cacheDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
