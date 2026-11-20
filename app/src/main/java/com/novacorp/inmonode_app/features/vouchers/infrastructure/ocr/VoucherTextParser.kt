package com.novacorp.inmonode_app.features.vouchers.infrastructure.ocr

import com.novacorp.inmonode_app.features.vouchers.domain.OcrData
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/** Amounts need a label or their currency and codes a label; missing or ambiguous data stays for manual review. */
internal object VoucherTextParser {
    /**
     * A label at the start of a row ("Operación", "N° de operación", "Nro. de operación", "Código de operación")
     * and, on the same row, a code with at least one digit, so a heading such as "Operación exitosa" is no code.
     */
    private val CODE = Regex("(?:^|\\n)[ \\t]*(?:(?:n[uú]mero|nro\\.?|n[°ºo.]?)[ \\t]*(?:de[ \\t]*)?)?" +
        "(?:operaci[oó]n|operation|c[oó]digo de operaci[oó]n)[ \\t]*[:#.-]?[ \\t]*" +
        "((?=[A-Za-z0-9-]*[0-9])[A-Za-z0-9-]{3,50})(?![A-Za-z0-9-])", RegexOption.IGNORE_CASE)

    private const val CURRENCY = "(S/[.]?|PEN|USD|US\\$|\\$)"
    /** Thousands may carry a space the OCR adds after the separator ("2, 500.00" was read as 2). */
    private const val NUMBER = "([0-9]{1,3}(?:[.,] ?[0-9]{3})+(?:[.,][0-9]{1,2})?|[0-9]+(?:[.,][0-9]{1,2})?)(?![0-9])"
    private val LABELLED_AMOUNT = Regex("(?:monto(?:[ \\t]+(?:transferido|enviado|pagado|depositado|abonado))?|importe|total|amount)" +
        "[ \\t]*:?[ \\t]*$CURRENCY?[ \\t]*$NUMBER", RegexOption.IGNORE_CASE)
    private val CURRENCY_AMOUNT = Regex("$CURRENCY[ \\t]*$NUMBER", RegexOption.IGNORE_CASE)

    fun parse(text: String, confidence: BigDecimal?): OcrData {
        // A labelled amount wins; without any label, a single amount written with its currency ("S/ 50") is taken.
        val amounts = LABELLED_AMOUNT.findAll(text).toList().ifEmpty { CURRENCY_AMOUNT.findAll(text).toList() }
        val parsedAmounts = amounts.mapNotNull { parseAmount(it.groupValues[2]) }.distinctBy { it.stripTrailingZeros() }
        val amount = parsedAmounts.singleOrNull()
        val labelledCurrencies = amounts.mapNotNull { match ->
            when (match.groupValues[1].uppercase()) {
                "USD", "US$" -> "USD"
                "PEN", "S/", "S/." -> "PEN"
                else -> null
            }
        }.distinct()
        val mentionedCurrencies = buildList {
            if (Regex("\\bUSD\\b|US\\$|d[oó]lares", RegexOption.IGNORE_CASE).containsMatchIn(text)) add("USD")
            if (Regex("\\bPEN\\b|S/|soles", RegexOption.IGNORE_CASE).containsMatchIn(text)) add("PEN")
        }
        val currency = if (labelledCurrencies.isNotEmpty()) labelledCurrencies.singleOrNull() else mentionedCurrencies.singleOrNull()
        val dateText = Regex("\\b(?:[0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}[/.-][0-9]{2}[/.-][0-9]{4})\\b").find(text)?.value
        val date = dateText?.let { value ->
            listOf("uuuu-MM-dd", "dd/MM/uuuu", "dd-MM-uuuu", "dd.MM.uuuu").firstNotNullOfOrNull { pattern ->
                runCatching { LocalDate.parse(value, DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT)).toString() }.getOrNull()
            }
        }
        val code = CODE.find(text)?.groupValues?.get(1)
        return OcrData(amount, currency, date, code, confidence)
    }

    private fun parseAmount(text: String): BigDecimal? {
        val raw = text.replace(" ", "")
        val normalized = when {
            ',' in raw && '.' in raw -> if (raw.lastIndexOf(',') > raw.lastIndexOf('.')) raw.replace(".", "").replace(',', '.') else raw.replace(",", "")
            ',' in raw -> if (raw.substringAfterLast(',').length == 2) raw.replace(',', '.') else raw.replace(",", "")
            else -> raw
        }
        return normalized.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO && it.stripTrailingZeros().scale() <= 2 }
    }
}
