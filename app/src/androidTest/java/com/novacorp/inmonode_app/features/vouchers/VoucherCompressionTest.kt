package com.novacorp.inmonode_app.features.vouchers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.novacorp.inmonode_app.features.vouchers.domain.OcrData
import com.novacorp.inmonode_app.features.vouchers.infrastructure.image.ImageCompressor
import com.novacorp.inmonode_app.features.vouchers.infrastructure.ocr.MlKitOcrEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import kotlin.random.Random

/** US-08 with the device's own JPEG encoder and the bundled ML Kit model, as in the field. */
class VoucherCompressionTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dir = File(context.cacheDir, "compression-test").apply { deleteRecursively(); mkdirs() }
    private val compressor = ImageCompressor()
    private val ocr = MlKitOcrEngine(context)

    @After fun cleanUp() { dir.deleteRecursively() }

    @Test fun aCameraSizedVoucherGoesUnderTwoMegabytesAndReadsTheSame() = runBlocking {
        val photo = syntheticPhoto(File(dir, "photo.jpg"))
        assertTrue("the sample must start above the limit", photo.length() >= ImageCompressor.MAX_BYTES)

        val upload = compressor.compress(photo, File(dir, "upload.jpg"))
        Log.i(TAG, "synthetic|${size(photo)}|${photo.length()}|${size(upload)}|${upload.length()}")

        assertTrue(upload.length() < ImageCompressor.MAX_BYTES)
        assertTrue(longSide(upload) <= 2048)
        val before = ocr.recognize(photo.path).getOrThrow().data.copy(confidence = null)
        val after = ocr.recognize(upload.path).getOrThrow().data.copy(confidence = null)
        assertEquals(OcrData(BigDecimal("1250.50"), "PEN", "2026-10-09", "00123456"), after)
        assertEquals(before, after)
    }

    @Test fun aCorruptImageStopsTheCompressionAndKeepsTheOriginal() = runBlocking {
        val broken = File(dir, "broken.jpg").apply { writeBytes(ByteArray(4096) { it.toByte() }) }
        val original = broken.readBytes()
        val destination = File(dir, "upload.jpg")

        val error = runCatching { compressor.compress(broken, destination) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertArrayEquals(original, broken.readBytes())
        assertFalse(destination.exists())
        assertFalse(File(dir, "upload.jpg.tmp").exists())
    }

    /**
     * Measures real voucher photos copied into the app (never committed: they hold bank data).
     * Run with `-e voucherSamples <dir>`; skipped otherwise. Results go to logcat, tag VoucherCompression.
     */
    @Test fun measuresVoucherPhotos() = runBlocking {
        val samples = InstrumentationRegistry.getArguments().getString("voucherSamples")?.let(::File)
        assumeTrue(samples?.isDirectory == true)
        val photos = samples!!.listFiles { file -> file.extension.lowercase() in setOf("jpg", "jpeg") }.orEmpty().sorted()
        assumeTrue(photos.isNotEmpty())
        photos.forEach { photo ->
            val upload = compressor.compress(photo, File(dir, "${photo.nameWithoutExtension}-upload.jpg"))
            val before = ocr.recognize(photo.path).getOrThrow()
            val after = ocr.recognize(upload.path).getOrThrow()
            Log.i(TAG, "${photo.name}|${size(photo)}|${photo.length()}|${size(upload)}|${upload.length()}|" +
                "${before.data}|${after.data}|${after.requiresRecapture}")
            assertTrue("${photo.name} is ${upload.length()} bytes", upload.length() < ImageCompressor.MAX_BYTES)
        }
    }

    private fun bounds(file: File) = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        .also { BitmapFactory.decodeFile(file.path, it) }
    private fun size(file: File) = bounds(file).let { "${it.outWidth}x${it.outHeight}" }
    private fun longSide(file: File) = bounds(file).let { maxOf(it.outWidth, it.outHeight) }

    /** A 12 MP portrait photo of a printed voucher: sensor grain keeps the JPEG above 2 MB, like a camera's. */
    private fun syntheticPhoto(file: File): File {
        val width = 3060
        val height = 4080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val random = Random(8)
        val row = IntArray(width)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val shade = 228 + random.nextInt(-14, 15)
                row[x] = Color.rgb(shade, shade - 3, shade - 9)
            }
            bitmap.setPixels(row, 0, width, 0, y, width, 1)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(25, 25, 30); textSize = 120f; typeface = Typeface.DEFAULT_BOLD }
        val canvas = Canvas(bitmap)
        listOf("BANCO DE PRUEBA", "Constancia de pago", "Monto: S/ 1,250.50", "Fecha: 09/10/2026", "Numero de operacion: 00123456")
            .forEachIndexed { index, line -> canvas.drawText(line, 220f, 900f + index * 330f, paint) }
        file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)) }
        bitmap.recycle()
        return file
    }

    private companion object { const val TAG = "VoucherCompression" }
}
