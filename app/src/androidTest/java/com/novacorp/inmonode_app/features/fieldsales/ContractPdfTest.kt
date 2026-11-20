package com.novacorp.inmonode_app.features.fieldsales

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.repositories.ContractDocumentRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.time.ZoneId

/** US-13 scenario 1: the preliminary contract is rendered on the device, without a connection, as a PDF Android can open. */
class ContractPdfTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val documents = ContractDocumentRepositoryImpl(context)
    private val rules = FinancingRules(BigDecimal("10"), BigDecimal("12.5"), 120, BigDecimal("2"))
    private val lot = Lot(10, 1, "N-02", LotDimensions(BigDecimal("9"), BigDecimal("15"), BigDecimal("135")), emptyList(),
        BigDecimal("50500"), "PEN", LotStatus.AVAILABLE)
    private val project = Project(1, "Residencial Las Lomas de Lurín", "Lurín, Lima", null, null, null, rules, listOf(lot))
    private val contract = PreliminaryContract(
        Reservation("test-contract", 10, "p1", BigDecimal("500"), "2026-11-20T02:48:00Z",
            terms = NegotiationTerms(BigDecimal("500"), "Cerco perimétrico", "Pago del saldo en dos partes.")),
        Prospect("p1", "59000059", "Prueba Sync T59", "", "Casado(a)", "2026-11-19T19:00:00Z"), lot, project)

    @Test fun bundledTemplateBecomesAPdfAndroidCanOpen() = runBlocking {
        val filled = documents.template().fill(contract.values(ZoneId.of("America/Lima")))
        val footers = mutableListOf<Pair<Int, Int>>()
        val pdf = documents.render("test-contract", "contrato-preliminar-N-02", filled) { page, pages -> footers += page to pages; "Página $page de $pages" }
        try {
            assertEquals("contrato-preliminar-N-02.pdf", pdf.file.name)
            assertEquals((1..pdf.pages).map { it to pdf.pages }, footers)
            assertEquals(pdf.pages, pageCount(pdf.file))
            assertTrue(firstPageHasInk(pdf.file))
            assertFalse(File(pdf.file.path + ".tmp").exists())
            Log.i("ContractPdf", "pages=${pdf.pages} bytes=${pdf.file.length()}")
        } finally {
            documents.delete("test-contract")
        }
        assertFalse(pdf.file.exists())
    }

    @Test fun longClausesFlowOntoMorePagesAndRenderingAgainReplacesTheFile() = runBlocking {
        val long = ContractTemplate("t", "TÍTULO", (1..40).map {
            ContractTemplate.Section("CLÁUSULA $it", "Texto de la cláusula $it. ".repeat(30) + "\n\nSegundo párrafo de la cláusula $it.")
        })
        try {
            val first = documents.render("test-contract", "contrato-preliminar-N-02", long) { page, pages -> "$page/$pages" }
            assertTrue("pages=${first.pages}", first.pages > 5)
            assertEquals(first.pages, pageCount(first.file))
            val short = documents.render("test-contract", "contrato-preliminar-N-02", long.copy(sections = long.sections.take(1))) { page, pages -> "$page/$pages" }
            assertEquals(first.file, short.file)
            assertEquals(1, pageCount(short.file))
        } finally {
            documents.delete("test-contract")
        }
    }

    @Test fun aNameThatIsNotASeparationIdCannotChooseTheFile() = runBlocking {
        val template = ContractTemplate("t", "T", listOf(ContractTemplate.Section("A", "texto")))
        assertTrue(runCatching { documents.render("../shared_prefs/x", "x", template) { _, _ -> "" } }.exceptionOrNull() is IllegalArgumentException)
    }

    @Test fun theSharedNameStaysInsideTheFolderAndASeparationKeepsOnePdf() = runBlocking {
        val template = ContractTemplate("t", "T", listOf(ContractTemplate.Section("A", "texto")))
        try {
            val odd = documents.render("test-contract", "contrato preliminar/../N 02", template) { _, _ -> "" }
            assertEquals("contrato-preliminar-N-02.pdf", odd.file.name)
            val renamed = documents.render("test-contract", "contrato-preliminar-N-03", template) { _, _ -> "" }
            assertEquals(listOf("contrato-preliminar-N-03.pdf"), renamed.file.parentFile!!.list()!!.toList())
        } finally {
            documents.delete("test-contract")
        }
        assertFalse(File(context.cacheDir, "contracts/test-contract").exists())
    }

    private fun pageCount(file: File): Int = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
        val renderer = PdfRenderer(fd)
        try { renderer.pageCount } finally { renderer.close() }
    }

    private fun firstPageHasInk(file: File): Boolean = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
        val renderer = PdfRenderer(fd)
        try {
            renderer.openPage(0).use { page ->
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val pixels = IntArray(bitmap.width * bitmap.height).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }
                pixels.count { it != Color.WHITE } > 1000
            }
        } finally { renderer.close() }
    }
}
