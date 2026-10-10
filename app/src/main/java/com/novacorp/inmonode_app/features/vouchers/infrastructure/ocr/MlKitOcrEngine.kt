package com.novacorp.inmonode_app.features.vouchers.infrastructure.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.novacorp.inmonode_app.core.network.operationResult
import com.novacorp.inmonode_app.features.vouchers.domain.OcrEngine
import com.novacorp.inmonode_app.features.vouchers.domain.OcrReading
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal
import javax.inject.Inject

class MlKitOcrEngine @Inject constructor(@ApplicationContext private val context: Context) : OcrEngine {
    override suspend fun recognize(imagePath: String): Result<OcrReading> = operationResult {
        val image = withContext(Dispatchers.IO) { InputImage.fromFilePath(context, Uri.fromFile(File(imagePath))) }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val reading = recognizer.process(image).await()
            val scores = reading.textBlocks.flatMap { it.lines }.flatMap { it.elements }
                .map { it.confidence.toDouble() }.filter { it.isFinite() && it in 0.0..1.0 }
            val confidence = scores.takeIf { it.isNotEmpty() }?.average()?.let(BigDecimal::valueOf)
            OcrReading(VoucherTextParser.parse(reading.text, confidence), reading.text.trim().length < 8 ||
                (confidence != null && confidence < BigDecimal("0.40")))
        } finally {
            recognizer.close()
        }
    }
}
