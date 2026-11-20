package com.novacorp.inmonode_app.features.fieldsales

import com.novacorp.inmonode_app.features.fieldsales.application.ContractPreview
import com.novacorp.inmonode_app.features.fieldsales.application.PreviewContractUseCase
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ContractDocumentRepository
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ContractPdf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime

class PreviewContractUseCaseTest {
    private class Documents : ContractDocumentRepository {
        var rendered: ContractTemplate? = null
        var name: String? = null
        val footers = mutableListOf<String>()
        override suspend fun template() = ContractTemplate.parse(File("src/main/assets/contracts/preliminary-separation.txt").readText())
        override suspend fun render(reservationId: String, name: String, contract: ContractTemplate, footer: (Int, Int) -> String): ContractPdf {
            rendered = contract
            this.name = name
            footers += footer(1, 2)
            footers += footer(2, 2)
            return ContractPdf(File("$reservationId.pdf"), 2)
        }
        override suspend fun delete(reservationId: String) = Unit
    }

    private val rules = FinancingRules(BigDecimal("10"), BigDecimal("12.5"), 120, BigDecimal("2"))
    private val lot = Lot(10, 1, "N-02", LotDimensions(BigDecimal("9"), BigDecimal("15"), BigDecimal("135")), emptyList(),
        BigDecimal("50500"), "PEN", LotStatus.AVAILABLE)
    private val project = Project(1, "Residencial Las Lomas de Lurín", "Lurín", null, null, null, rules, listOf(lot))
    private val prospect = Prospect("p1", "59000059", "Prueba Sync T59", "", "Soltero(a)", "2026-11-19T19:00:00Z")
    private val reservation = Reservation("r1", 10, "p1", BigDecimal("500"), "2026-11-20T02:48:00Z")
    private val now = ZonedDateTime.of(2026, 11, 19, 21, 50, 0, 0, ZoneId.of("America/Lima"))

    @Test fun completeSeparationBecomesAPdfWhoseFooterNamesTheTemplateAndThePage() = runTest {
        val documents = Documents()
        val preview = PreviewContractUseCase(documents) { now }(PreliminaryContract(reservation, prospect, lot, project)).getOrThrow()
        assertEquals(ContractPreview.Ready(ContractPdf(File("r1.pdf"), 2), "1"), preview)
        assertTrue(documents.rendered!!.sections.none { it.body.contains("{{") })
        assertEquals("contrato-preliminar-N-02", documents.name)
        assertEquals("Borrador sin valor legal · Plantilla v1 · Generado sin conexión el 19/11/2026 21:50 · Página 2 de 2",
            documents.footers[1])
    }

    @Test fun missingDataIsReportedWithoutGeneratingTheDocument() = runTest {
        val documents = Documents()
        val incomplete = PreliminaryContract(reservation, prospect.copy(maritalStatus = null), lot, project)
        val preview = PreviewContractUseCase(documents) { now }(incomplete).getOrThrow()
        assertEquals(ContractPreview.Missing(listOf(ContractField.MARITAL_STATUS)), preview)
        assertNull(documents.rendered)
    }
}
