package com.novacorp.inmonode_app.features.fieldsales.application

import com.novacorp.inmonode_app.features.fieldsales.domain.model.FinancingRules
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import javax.inject.Inject

data class FinancingInstallment(val number: Int, val dueDate: LocalDate, val payment: BigDecimal,
    val principal: BigDecimal, val interest: BigDecimal, val balance: BigDecimal)
data class FinancingSimulation(val financedAmount: BigDecimal, val monthlyPayment: BigDecimal,
    val totalInterest: BigDecimal, val installments: List<FinancingInstallment>)

/** Same effective annual rate and rounding policy as the backend's French amortization service. */
class SimulateFinancingUseCase @Inject constructor() {
    operator fun invoke(price: BigDecimal, downPayment: BigDecimal, months: Int,
        rules: FinancingRules, startDate: LocalDate = LocalDate.now()): Result<FinancingSimulation> = runCatching {
        require(price > BigDecimal.ZERO && downPayment > BigDecimal.ZERO && downPayment < price) { "La inicial debe ser positiva y menor que el precio del lote." }
        require(downPayment >= price.multiply(rules.minDownPaymentPercentage).movePointLeft(2).setScale(2, RoundingMode.CEILING)) {
            "La inicial no cumple el mínimo del proyecto (${rules.minDownPaymentPercentage}%)."
        }
        require(months in 1..rules.maxTermMonths) { "El plazo debe estar entre 1 y ${rules.maxTermMonths} meses." }
        require(price.stripTrailingZeros().scale() <= 2 && downPayment.stripTrailingZeros().scale() <= 2) { "Los montos deben tener como máximo dos decimales." }
        val precision = MathContext.DECIMAL128
        val annualGrowth = BigDecimal.ONE.add(rules.annualInterestRate.movePointLeft(2), precision)
        var root = BigDecimal.ONE
        for (iteration in 0 until 80) {
            val next = root.multiply(BigDecimal(11), precision)
                .add(annualGrowth.divide(root.pow(11, precision), precision), precision).divide(BigDecimal(12), precision)
            val converged = next.subtract(root).abs() < BigDecimal("1E-30")
            root = next
            if (converged) break
        }
        val rate = root.subtract(BigDecimal.ONE, precision)
        val financed = price.subtract(downPayment).setScale(2, RoundingMode.HALF_EVEN)
        val payment = if (rate.signum() == 0) financed.divide(BigDecimal(months), 2, RoundingMode.HALF_EVEN) else {
            val growth = BigDecimal.ONE.add(rate).pow(months, precision)
            financed.multiply(rate, precision).multiply(growth, precision)
                .divide(growth.subtract(BigDecimal.ONE), precision).setScale(2, RoundingMode.HALF_EVEN)
        }
        var balance = financed
        val installments = (1..months).map { number ->
            val interest = balance.multiply(rate, precision).setScale(2, RoundingMode.HALF_EVEN)
            val principal = if (number == months) balance else payment.subtract(interest)
            balance = balance.subtract(principal).setScale(2, RoundingMode.HALF_EVEN)
            FinancingInstallment(number, startDate.plusMonths(number.toLong()), principal.add(interest), principal, interest, balance)
        }
        FinancingSimulation(financed, payment, installments.fold(BigDecimal.ZERO) { total, item -> total + item.interest }, installments)
    }
}
