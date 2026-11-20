package com.novacorp.inmonode_app.features.fieldsales.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A datum the preliminary contract cannot be generated without (US-13 scenario 2). */
enum class ContractField { BUYER_NAME, BUYER_DOCUMENT, MARITAL_STATUS, LOT, LOT_PRICE, INITIAL_AMOUNT }

/**
 * The separation stored on the device, as the preliminary contract reads it (US-13). The term and the rate come
 * from the project's financing rules, downloaded with the portfolio, because the field separation does not fix them.
 */
data class PreliminaryContract(val reservation: Reservation?, val prospect: Prospect?, val lot: Lot?, val project: Project?) {

    /** The critical data still empty, in the order the agent completes them; empty when the contract can be generated. */
    val missing: List<ContractField>
        get() = buildList {
            if (prospect?.fullName.isNullOrBlank()) add(ContractField.BUYER_NAME)
            if (prospect?.document.isNullOrBlank()) add(ContractField.BUYER_DOCUMENT)
            if (prospect?.maritalStatus.isNullOrBlank()) add(ContractField.MARITAL_STATUS)
            if (lot == null || project == null) add(ContractField.LOT)
            else if (lot.price <= BigDecimal.ZERO) add(ContractField.LOT_PRICE)
            if (reservation == null || reservation.initialAmount <= BigDecimal.ZERO) add(ContractField.INITIAL_AMOUNT)
        }

    /** The value of each template variable, with dates in [zone]; fails while [missing] is not empty. */
    fun values(zone: ZoneId): Map<String, String> {
        check(missing.isEmpty()) { "Faltan datos para generar el contrato: ${missing.joinToString()}." }
        val reservation = requireNotNull(reservation)
        val prospect = requireNotNull(prospect)
        val lot = requireNotNull(lot)
        val project = requireNotNull(project)
        val rules = project.financingRules
        val terms = reservation.terms
        fun money(value: BigDecimal) = "${lot.currency} ${value.setScale(2, RoundingMode.DOWN).toPlainString()}"
        fun percent(value: BigDecimal) = value.stripTrailingZeros().toPlainString()
        return mapOf(
            "buyer.name" to prospect.fullName.trim(),
            "buyer.document" to prospect.document.trim(),
            "buyer.maritalStatus" to prospect.maritalStatus.orEmpty().trim().lowercase(SPANISH),
            "project.name" to project.name,
            "project.location" to project.location,
            "lot.code" to lot.code,
            "lot.area" to lot.dimensions.area.setScale(2, RoundingMode.HALF_UP).toPlainString(),
            "price.list" to money(lot.price),
            "terms.discount" to (terms.discount?.let(::money) ?: "no aplica"),
            "price.final" to money(terms.finalPrice(lot.price)),
            "terms.bonus" to (terms.bonus ?: "ninguno"),
            "terms.notes" to (terms.notes ?: "No se pactaron condiciones adicionales."),
            "reservation.amount" to money(reservation.initialAmount),
            "reservation.date" to date(reservation.reservedAt, zone),
            "rules.minDownPayment" to percent(rules.minDownPaymentPercentage),
            "rules.annualRate" to percent(rules.annualInterestRate),
            "rules.maxTermMonths" to rules.maxTermMonths.toString(),
        )
    }

    private companion object {
        val SPANISH: Locale = Locale.forLanguageTag("es-PE")

        fun date(instant: String, zone: ZoneId): String = runCatching {
            DateTimeFormatter.ofPattern("dd/MM/uuuu", SPANISH).withZone(zone).format(Instant.parse(instant))
        }.getOrDefault(instant)
    }
}
