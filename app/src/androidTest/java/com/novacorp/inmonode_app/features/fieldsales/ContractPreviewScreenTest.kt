package com.novacorp.inmonode_app.features.fieldsales

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.novacorp.inmonode_app.core.designsystem.theme.InmoNodeAppTheme
import com.novacorp.inmonode_app.features.fieldsales.application.*
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.*
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.repositories.ContractDocumentRepositoryImpl
import com.novacorp.inmonode_app.features.fieldsales.presentation.reservation.ReservationWizardViewModel
import com.novacorp.inmonode_app.features.fieldsales.presentation.reservation.components.ContractPreviewStep
import com.novacorp.inmonode_app.features.vouchers.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.math.BigDecimal

/** US-13 on the contract step: the missing-data dialog (scenario 2), then the PDF pages generated offline (scenario 1). */
class ContractPreviewScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val documents = ContractDocumentRepositoryImpl(context)
    private val lot = Lot(10, 1, "N-02", LotDimensions(BigDecimal("9"), BigDecimal("15"), BigDecimal("135")), emptyList(),
        BigDecimal("50500"), "PEN", LotStatus.AVAILABLE)
    private val project = Project(1, "Residencial Las Lomas de Lurín", "Lurín, Lima", null, null, null,
        FinancingRules(BigDecimal("10"), BigDecimal("12.5"), 120, BigDecimal("2")), listOf(lot))
    private val draft = Reservation(ID, 10, "p1", BigDecimal("500"), "2026-11-20T02:48:00Z", ReservationStatus.DRAFT)
    private val people = MutableStateFlow(listOf(Prospect("p1", "59000059", "Prueba Contrato", "", null, "2026-11-19T19:00:00Z")))

    private val portfolio = object : PortfolioRepository {
        override fun observePortfolio() = flowOf(Portfolio(listOf(project), "2026-11-19", null))
        override suspend fun download() = Result.success(Portfolio(listOf(project), "2026-11-19", null))
    }
    private val prospects = object : ProspectRepository {
        override fun observeProspects() = people
        override suspend fun completeMaritalStatus(prospectId: String, maritalStatus: String): Result<Prospect> {
            people.value = people.value.map { it.copy(maritalStatus = maritalStatus) }
            return Result.success(people.value.first())
        }
        override suspend fun register(document: String, fullName: String, phone: String, maritalStatus: String?) = error("unused")
    }
    private val reservations = object : ReservationRepository {
        override fun observeReservations() = flowOf(listOf(draft))
        override suspend fun createDraft(lotId: Long, prospectId: String, initialAmount: BigDecimal, terms: NegotiationTerms) = error("unused")
        override suspend fun confirmDraft(reservationId: String) = Result.success(draft.copy(status = ReservationStatus.PENDING_SYNC))
        override suspend fun cancelDraft(reservationId: String) = Result.success(Unit)
        override suspend fun reassign(reservationId: String, newLotId: Long) = error("unused")
    }
    private val vouchers = object : VoucherRepository {
        override fun observeVouchers() = flowOf(emptyList<Voucher>())
        override suspend fun capture(reservationId: String, imagePath: String) = error("unused")
        override suspend fun processOcr(voucherId: String) = error("unused")
        override suspend fun review(voucherId: String, data: OcrData) = error("unused")
        override suspend fun upload(voucherId: String) = error("unused")
        override suspend fun getPaymentEvidences(reservationId: String) = error("unused")
        override suspend fun discardCancelled() = Result.success(Unit)
    }
    private val identityDocuments = object : IdentityDocumentRepository {
        override fun observeDocuments() = flowOf(emptyList<IdentityDocument>())
        override suspend fun capture(reservationId: String, imagePath: String) = error("unused")
        override suspend fun upload(documentId: String) = error("unused")
        override suspend fun discardCancelled() = Result.success(Unit)
    }

    private fun viewModel() = ReservationWizardViewModel(portfolio, prospects, reservations, BeginReservationUseCase(reservations),
        ReserveLotUseCase(reservations), CancelReservationDraftUseCase(reservations, vouchers, identityDocuments, documents), CompleteProspectUseCase(prospects),
        PreviewContractUseCase(documents), SavedStateHandle(mapOf("reservationId" to ID)))

    @After fun deleteThePdf() = runBlocking { documents.delete(ID) }

    @Test fun missingMaritalStatusStopsTheContractUntilTheAgentCompletesIt() {
        compose.setContent { InmoNodeAppTheme { ContractPreviewStep(viewModel = viewModel(), onBack = {}, onConfirmed = {}) } }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("No se puede generar el contrato").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Falta completar: Estado civil del prospecto.").assertExists()
        compose.onNodeWithContentDescription("Compartir contrato").assertDoesNotExist()
        screenshot("contract-missing")

        compose.onNodeWithText("Completar dato").performClick()
        compose.onNodeWithText("No se puede generar el contrato").assertDoesNotExist()
        compose.onNodeWithText("Datos OCR").assertExists()
        compose.onNode(hasText("Estado civil") and hasClickAction()).performClick()
        compose.onNodeWithText("Casado(a)").performClick()

        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("del contrato preliminar", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Compartir contrato").assertIsEnabled()
        compose.onNodeWithText("Confirmar separación").assertIsNotEnabled()
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onNodeWithText("Confirmar separación").assertIsEnabled()
        screenshot("contract-ready")
        assert(File(context.cacheDir, "contracts/$ID/contrato-preliminar-N-02.pdf").exists())
    }

    @Test fun contractOpenedFromSeparationsShowsNoWizardStepsOrConfirmation() {
        people.value = people.value.map { it.copy(maritalStatus = "Soltero(a)") }
        compose.setContent { InmoNodeAppTheme { ContractPreviewStep(viewModel = viewModel(), readOnly = true, onBack = {}, onConfirmed = {}) } }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("del contrato preliminar", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Datos OCR").assertDoesNotExist()
        compose.onNodeWithText("Confirmar separación").assertDoesNotExist()
        compose.onNodeWithContentDescription("Compartir contrato").assertIsEnabled()
        screenshot("contract-read-only")
    }

    /** Saved in the app cache so a reviewer can pull it with run-as. */
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.cacheDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object { const val ID = "screen-test" }
}
