package com.novacorp.inmonode_app.features.fieldsales

import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.time.ZoneId

class PreliminaryContractTest {
    private val lima = ZoneId.of("America/Lima")
    private val rules = FinancingRules(BigDecimal("10.00"), BigDecimal("12.50"), 120, BigDecimal("2"))
    private val lot = Lot(10, 1, "N-02", LotDimensions(BigDecimal("9"), BigDecimal("15"), BigDecimal("135")), emptyList(),
        BigDecimal("50500"), "PEN", LotStatus.AVAILABLE)
    private val project = Project(1, "Residencial Las Lomas de Lurín", "Lurín, Lima", null, null, null, rules, listOf(lot))
    private val prospect = Prospect("p1", "59000059", "Prueba Sync T59", "", "Casado(a)", "2026-11-19T19:00:00Z")
    private val reservation = Reservation("r1", 10, "p1", BigDecimal("500"), "2026-11-20T02:48:00Z",
        terms = NegotiationTerms(BigDecimal("500"), "Cerco perimétrico", null))
    private val contract = PreliminaryContract(reservation, prospect, lot, project)

    @Test fun bundledTemplateIsFilledWithTheSeparationOnTheDevice() {
        val template = ContractTemplate.parse(File("src/main/assets/contracts/preliminary-separation.txt").readText())
        val filled = template.fill(contract.values(lima))
        val text = filled.sections.joinToString("\n") { it.heading + "\n" + it.body }
        assertEquals("1", template.version)
        assertEquals(8, filled.sections.size)
        assertFalse(text.contains("{{"))
        listOf("Prueba Sync T59", "59000059", "casado(a)", "N-02", "135.00 m²", "PEN 50500.00", "PEN 50000.00",
            "Cerco perimétrico", "PEN 500.00", "12.5 %", "120 meses").forEach { assertTrue(it, text.contains(it)) }
    }

    @Test fun separationDateIsTheAgentsLocalDay() {
        // 02:48 UTC on the 20th is still the 19th in Lima.
        assertEquals("19/11/2026", contract.values(lima)["reservation.date"])
    }

    @Test fun termsNotAgreedUseNeutralWording() {
        val values = contract.copy(reservation = reservation.copy(terms = NegotiationTerms())).values(lima)
        assertEquals("no aplica", values["terms.discount"])
        assertEquals("PEN 50500.00", values["price.final"])
        assertEquals("ninguno", values["terms.bonus"])
        assertEquals("No se pactaron condiciones adicionales.", values["terms.notes"])
    }

    @Test fun missingMaritalStatusStopsTheContract() {
        val incomplete = contract.copy(prospect = prospect.copy(maritalStatus = " "))
        assertEquals(listOf(ContractField.MARITAL_STATUS), incomplete.missing)
        assertThrows(IllegalStateException::class.java) { incomplete.values(lima) }
    }

    @Test fun everyCriticalDatumIsReportedWhenNothingIsStored() {
        assertEquals(listOf(ContractField.BUYER_NAME, ContractField.BUYER_DOCUMENT, ContractField.MARITAL_STATUS,
            ContractField.LOT, ContractField.INITIAL_AMOUNT), PreliminaryContract(null, null, null, null).missing)
        assertTrue(contract.missing.isEmpty())
    }

    @Test fun templateParagraphsKeepTheirBreaks() {
        val template = ContractTemplate.parse("version: 2\n# Título\n\n## A\nuna\nlínea\n\notro párrafo\n## B\n{{x}}")
        assertEquals(listOf("A", "B"), template.sections.map { it.heading })
        assertEquals("una línea\n\notro párrafo", template.sections[0].body)
        assertEquals(setOf("x"), template.variables)
    }

    @Test fun templateWithUnknownDataOrWithoutVersionIsRejected() {
        val unknown = ContractTemplate.parse("version: 2\n# T\n## A\nEdad: {{buyer.age}}")
        val error = assertThrows(IllegalArgumentException::class.java) { unknown.fill(contract.values(lima)) }
        assertTrue(error.message!!.contains("buyer.age"))
        assertThrows(IllegalArgumentException::class.java) { ContractTemplate.parse("# T\n## A\ntexto") }
    }
}
