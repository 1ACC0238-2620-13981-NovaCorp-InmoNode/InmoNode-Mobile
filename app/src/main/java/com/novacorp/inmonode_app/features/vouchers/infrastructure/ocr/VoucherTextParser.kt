package com.novacorp.inmonode_app.features.vouchers.infrastructure.ocr

import com.novacorp.inmonode_app.features.vouchers.domain.OcrData
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/** Only labelled amounts/codes are extracted; missing or ambiguous data stays for manual review. */
internal object VoucherTextParser {
    fun parse(text: String, confidence: BigDecimal?): OcrData {
        val amounts = Regex("(?:monto|importe|total|amount)\\s*:?\\s*(S/[.]?|PEN|USD|US\\$|\\$)?\\s*([0-9][0-9.,]*)", RegexOption.IGNORE_CASE)
            .findAll(text).toList()
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
        val code = Regex("(?:^|\\n)\\s*(?:(?:n[uú]mero|n[°ºo.]?)\\s*(?:de\\s*)?)?(?:operaci[oó]n|operation|c[oó]digo de operaci[oó]n)\\s*[:#.-]?\\s*([A-Za-z0-9-]{3,50})", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)
        return OcrData(amount, currency, date, code, confidence)
    }

    private fun parseAmount(raw: String): BigDecimal? {
        val normalized = when {
            ',' in raw && '.' in raw -> if (raw.lastIndexOf(',') > raw.lastIndexOf('.')) raw.replace(".", "").replace(',', '.') else raw.replace(",", "")
            ',' in raw -> if (raw.substringAfterLast(',').length == 2) raw.replace(',', '.') else raw.replace(",", "")
            else -> raw
        }
        return normalized.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO && it.stripTrailingZeros().scale() <= 2 }
    }
}
