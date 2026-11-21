package com.novacorp.inmonode_app.core.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Amounts as the field app shows and prints them (mockups, preliminary contract): "S/ 44,300.00",
 * "US$ 1,200.00". Other ISO codes keep the code. Cut, never rounded up, to two decimals.
 */
fun formatMoney(currency: String, amount: BigDecimal): String = "${currencySymbol(currency)} ${formatAmount(amount)}"

/** The amount alone, for a record whose currency is unknown: "44,300.00". */
fun formatAmount(amount: BigDecimal): String =
    DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US)).apply { roundingMode = RoundingMode.DOWN }.format(amount)

private fun currencySymbol(currency: String): String = when (currency.uppercase(Locale.ROOT)) {
    "PEN" -> "S/"
    "USD" -> "US$"
    else -> currency
}
