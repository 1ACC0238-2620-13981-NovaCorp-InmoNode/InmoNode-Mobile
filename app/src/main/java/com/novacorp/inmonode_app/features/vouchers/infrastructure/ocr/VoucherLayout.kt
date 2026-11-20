package com.novacorp.inmonode_app.features.vouchers.infrastructure.ocr

/** A line read by the OCR engine, with its position on the photo in pixels. */
internal data class OcrLine(val text: String, val left: Int, val top: Int, val bottom: Int)

/**
 * Rebuilds the voucher as the agent sees it (US-09). The engine reads a two-column receipt column by
 * column, so "Número de operación" and its value end up lines apart. Lines whose vertical center falls
 * within the height of a row's first line belong to that row, and a row is read left to right.
 */
internal object VoucherLayout {
    fun rows(lines: List<OcrLine>): String {
        val rows = mutableListOf<MutableList<OcrLine>>()
        lines.filter { it.text.isNotBlank() }.sortedBy { it.top + it.bottom }.forEach { line ->
            val center = (line.top + line.bottom) / 2
            val row = rows.lastOrNull()?.takeIf { row -> center in row.first().top..row.first().bottom }
            if (row != null) row += line else rows += mutableListOf(line)
        }
        return rows.joinToString("\n") { row -> row.sortedBy { it.left }.joinToString("  ") { it.text.trim() } }
    }
}
