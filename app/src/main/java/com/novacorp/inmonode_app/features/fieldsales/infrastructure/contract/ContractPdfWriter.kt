package com.novacorp.inmonode_app.features.fieldsales.infrastructure.contract

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.text.LineBreaker
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.withTranslation
import com.novacorp.inmonode_app.features.fieldsales.domain.model.ContractTemplate
import java.io.OutputStream

/** Lays out a filled contract template on A4 pages with Android's own PDF engine, so it works without a connection. */
internal object ContractPdfWriter {
    private const val PAGE_WIDTH = 595 // A4, in points
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 56f
    private const val FOOTER_SPACE = 36f
    private val TEXT_WIDTH = (PAGE_WIDTH - 2 * MARGIN).toInt()
    private const val BOTTOM = PAGE_HEIGHT - MARGIN - FOOTER_SPACE

    private class Block(val layout: StaticLayout, val spaceBefore: Float = 0f, val spaceAfter: Float = 0f,
        val keepWithNext: Boolean = false)

    /** The lines [first]..[last] of [layout], drawn on [page] with their top at [y]. */
    private class Placement(val page: Int, val layout: StaticLayout, val first: Int, val last: Int, val y: Float)

    /** Writes [contract] to [out] and returns the number of pages; [footer] gets the page and the page count. */
    fun write(contract: ContractTemplate, footer: (page: Int, pages: Int) -> String, out: OutputStream): Int {
        val placements = paginate(blocks(contract))
        val pages = placements.maxOf { it.page } + 1
        val document = PdfDocument()
        try {
            for (index in 0 until pages) {
                val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create())
                val canvas = page.canvas
                placements.filter { it.page == index }.forEach { placement ->
                    val top = placement.layout.getLineTop(placement.first).toFloat()
                    val bottom = placement.layout.getLineBottom(placement.last).toFloat()
                    canvas.withTranslation(MARGIN, placement.y - top) {
                        clipRect(0f, top, TEXT_WIDTH.toFloat(), bottom)
                        placement.layout.draw(this)
                    }
                }
                val text = layout(footer(index + 1, pages), FOOTER, Layout.Alignment.ALIGN_CENTER)
                canvas.withTranslation(MARGIN, PAGE_HEIGHT - MARGIN - text.height) { text.draw(this) }
                document.finishPage(page)
            }
            document.writeTo(out)
        } finally {
            document.close()
        }
        return pages
    }

    private fun blocks(contract: ContractTemplate): List<Block> = buildList {
        add(Block(layout(contract.title, TITLE, Layout.Alignment.ALIGN_CENTER), spaceAfter = 18f))
        contract.sections.forEach { section ->
            add(Block(layout(section.heading, HEADING), spaceBefore = 10f, spaceAfter = 4f, keepWithNext = true))
            section.body.split("\n\n").forEach { add(Block(layout(it, BODY), spaceAfter = 6f)) }
        }
    }

    /** Places each block line by line; a heading moves to the next page together with the first line of its text. */
    private fun paginate(blocks: List<Block>): List<Placement> {
        val placements = mutableListOf<Placement>()
        var page = 0
        var y = MARGIN
        blocks.forEachIndexed { index, block ->
            val layout = block.layout
            if (y > MARGIN) y += block.spaceBefore
            val needed = if (block.keepWithNext) layout.height + (blocks.getOrNull(index + 1)?.let { lineHeight(it.layout, 0) } ?: 0f)
                else lineHeight(layout, 0)
            if (y > MARGIN && y + needed > BOTTOM) { page++; y = MARGIN }
            var first = 0
            for (line in 1 until layout.lineCount) {
                if (y + layout.getLineBottom(line) - layout.getLineTop(first) > BOTTOM) {
                    placements += Placement(page, layout, first, line - 1, y)
                    page++; y = MARGIN; first = line
                }
            }
            placements += Placement(page, layout, first, layout.lineCount - 1, y)
            y += layout.getLineBottom(layout.lineCount - 1) - layout.getLineTop(first) + block.spaceAfter
        }
        return placements
    }

    private fun lineHeight(layout: StaticLayout, line: Int) = (layout.getLineBottom(line) - layout.getLineTop(line)).toFloat()

    private fun layout(text: String, paint: TextPaint, alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, TEXT_WIDTH).setAlignment(alignment)
            .setLineSpacing(0f, 1.2f).setIncludePad(false).apply {
                if (paint === BODY && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD)
            }.build()

    private fun paint(size: Float, bold: Boolean = false, color: Int = Color.BLACK) = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        this.color = color
    }

    private val TITLE = paint(15f, bold = true)
    private val HEADING = paint(11.5f, bold = true)
    private val BODY = paint(11.5f)
    private val FOOTER = paint(8f, color = Color.DKGRAY)
}
