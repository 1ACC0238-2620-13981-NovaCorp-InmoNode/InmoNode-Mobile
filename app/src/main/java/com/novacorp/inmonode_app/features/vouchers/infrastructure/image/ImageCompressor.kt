package com.novacorp.inmonode_app.features.vouchers.infrastructure.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

class ImageCompressor @Inject constructor() {
    /** Encode once and retain this exact file for idempotent upload retries. */
    suspend fun compress(source: File, destination: File): File = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "La imagen no es legible. Vuelve a capturar el voucher." }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        val original = BitmapFactory.decodeFile(source.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IOException("No se pudo abrir la imagen del voucher.")
        val orientation = ExifInterface(source.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val transform = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }
        }
        val oriented = Bitmap.createBitmap(original, 0, 0, original.width, original.height, transform, true)
        val flattened = createBitmap(oriented.width, oriented.height, Bitmap.Config.RGB_565)
        Canvas(flattened).apply { drawColor(Color.WHITE); drawBitmap(oriented, 0f, 0f, null) }
        val temporary = File(destination.parentFile, "${destination.name}.tmp")
        try {
            destination.parentFile?.mkdirs()
            var quality = 90
            do {
                temporary.outputStream().use { output -> check(flattened.compress(Bitmap.CompressFormat.JPEG, quality, output)) }
                if (temporary.length() in 1 until MAX_BYTES) break
                quality -= 10
            } while (quality >= 40)
            check(temporary.length() in 1 until MAX_BYTES) { "No se pudo comprimir el voucher por debajo de 2 MB. Vuelve a capturarlo." }
            check(temporary.renameTo(destination)) { "No se pudo guardar el voucher comprimido." }
            destination
        } finally {
            temporary.delete()
            flattened.recycle()
            if (oriented !== original) oriented.recycle()
            original.recycle()
        }
    }

    companion object { const val MAX_BYTES = 2L * 1024 * 1024 }
}
