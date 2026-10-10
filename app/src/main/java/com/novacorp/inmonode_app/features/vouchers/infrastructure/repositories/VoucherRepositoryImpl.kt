package com.novacorp.inmonode_app.features.vouchers.infrastructure.repositories

import android.content.Context
import android.graphics.BitmapFactory
import com.novacorp.inmonode_app.core.database.InmoNodeDatabase
import com.novacorp.inmonode_app.core.network.ApiOperationException
import com.novacorp.inmonode_app.core.network.operationResult
import com.novacorp.inmonode_app.core.network.requireBody
import com.novacorp.inmonode_app.core.network.AuthenticatedRequestOwner
import com.novacorp.inmonode_app.core.time.utcTimestamp
import com.novacorp.inmonode_app.features.iam.infrastructure.local.FieldSession
import com.novacorp.inmonode_app.features.vouchers.domain.*
import com.novacorp.inmonode_app.features.vouchers.infrastructure.image.ImageCompressor
import com.novacorp.inmonode_app.features.vouchers.infrastructure.local.*
import com.novacorp.inmonode_app.features.vouchers.infrastructure.remote.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class VoucherRepositoryImpl @Inject constructor(
    private val database: InmoNodeDatabase,
    private val session: FieldSession,
    private val service: VoucherService,
    private val uploader: VoucherFileUploader,
    private val compressor: ImageCompressor,
    private val ocr: OcrEngine,
    @ApplicationContext private val context: Context,
) : VoucherRepository {
    private val dao = database.voucherDao()
    private val mutationMutex = Mutex()

    override fun observeVouchers(): Flow<List<Voucher>> = session.ownerIds.flatMapLatest { owner ->
        if (owner == null) flowOf(emptyList()) else dao.observeVouchers(owner).map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun capture(reservationId: String, imagePath: String): Result<Voucher> = operationResult {
        mutationMutex.withLock {
            val owner = session.requireOwner()
            val reservation = requireNotNull(database.reservationDao().find(owner, reservationId)) { "No se encontró la separación." }
            check(reservation.status in setOf("DRAFT", "PENDING_SYNC", "SYNCED", "FAILED")) { "Resuelve el conflicto de la separación antes de capturar el voucher." }
            val id = UUID.randomUUID().toString()
            val destination = File(context.filesDir, "vouchers/$owner/$id-original")
            withContext(Dispatchers.IO) {
                val source = File(imagePath)
                require(source.isFile && source.length() in 1..MAX_CAPTURE_BYTES) { "El archivo de imagen es inválido o demasiado grande." }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(source.path, bounds)
                require(bounds.outWidth > 0 && bounds.outHeight > 0) { "La imagen es ilegible. Vuelve a capturar el voucher." }
                destination.parentFile?.mkdirs()
                source.copyTo(destination)
            }
            val row = VoucherEntity(owner, id, reservationId, destination.path, voucherGson.toJson(OcrData()),
                capturedAt = utcTimestamp())
            try {
                session.requireUnchanged(owner)
                dao.upsert(row)
            } catch (exception: Exception) {
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) { destination.delete() }
                throw exception
            }
            row.toDomain()
        }
    }

    override suspend fun processOcr(voucherId: String): Result<Voucher> = operationResult {
        mutationMutex.withLock {
            val owner = session.requireOwner()
            val row = requireNotNull(dao.find(owner, voucherId)) { "No se encontró el voucher." }
            check(row.status == "PENDING_OCR" || row.status == "RECAPTURE_REQUIRED") { "Este voucher ya fue procesado." }
            // Bound the decoded bitmap before ML Kit processes a high-resolution camera image.
            val ocrFile = compressor.compress(File(row.imagePath), File(context.cacheDir, "voucher-ocr/$owner/${row.id}.jpg"))
            val reading = try {
                ocr.recognize(ocrFile.path).getOrThrow()
            } finally {
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) { ocrFile.delete() }
            }
            val status = when {
                reading.requiresRecapture -> "RECAPTURE_REQUIRED"
                reading.data.isValid() -> "EXTRACTED"
                else -> "MANUAL_REVIEW_NEEDED"
            }
            session.requireUnchanged(owner)
            val updated = row.copy(dataJson = voucherGson.toJson(reading.data), status = status, errorMessage = null)
            dao.upsert(updated)
            updated.toDomain()
        }
    }

    override suspend fun review(voucherId: String, data: OcrData): Result<Voucher> = operationResult {
        mutationMutex.withLock {
            val owner = session.requireOwner()
            val row = requireNotNull(dao.find(owner, voucherId)) { "No se encontró el voucher." }
            check(row.status in setOf("EXTRACTED", "MANUAL_REVIEW_NEEDED", "READY_TO_SYNC") && row.uploadPath == null) {
                "No se pueden modificar los datos de un voucher que ya comenzó su carga. Captura un voucher sustituto."
            }
            // The agent edits fields, never the confidence reported by the recognition engine.
            val normalized = data.copy(currency = data.currency?.trim()?.uppercase(),
                operationCode = data.operationCode?.trim(), confidence = row.ocrData().confidence)
            require(normalized.isValid()) { "Completa monto, moneda, fecha válida y número de operación antes de confirmar." }
            require(normalized.amount!!.stripTrailingZeros().scale() <= 2) { "El monto debe tener como máximo dos decimales." }
            val original = row.ocrData()
            val corrected = original.amount?.compareTo(normalized.amount) != 0 || original.currency != normalized.currency ||
                original.operationDate != normalized.operationDate || original.operationCode != normalized.operationCode
            val updated = row.copy(dataJson = voucherGson.toJson(normalized), status = "READY_TO_SYNC",
                manuallyCorrected = row.manuallyCorrected || corrected, errorMessage = null)
            dao.upsert(updated)
            updated.toDomain()
        }
    }

    override suspend fun upload(voucherId: String): Result<Voucher> = operationResult {
        mutationMutex.withLock {
            val owner = session.requireOwner()
            var row = requireNotNull(dao.find(owner, voucherId)) { "No se encontró el voucher." }
            if (row.status == "SYNCED") return@withLock row.toDomain()
            check(row.status == "READY_TO_SYNC") { "Revisa los datos OCR antes de sincronizar el voucher." }
            check(database.reservationDao().find(owner, row.reservationId)?.status == "SYNCED") {
                "Sincroniza la separación antes de subir su voucher."
            }
            val data = row.ocrData()
            require(data.isValid()) { "Los datos del voucher están incompletos." }
            try {
                val uploadFile = row.uploadPath?.let(::File) ?: run {
                    val file = compressor.compress(File(row.imagePath), File(context.filesDir, "vouchers/$owner/${row.id}-upload.jpg"))
                    row = row.copy(uploadPath = file.path)
                    dao.upsert(row)
                    file
                }
                check(uploadFile.isFile && uploadFile.length() in 1 until ImageCompressor.MAX_BYTES) { "No se encontró la imagen comprimida. Captura un voucher sustituto." }
                session.requireUnchanged(owner)
                if (!row.uploaded) {
                    val target = service.requestUpload(UploadUrlRequestDto(row.id, row.reservationId, "image/jpeg", uploadFile.length()), AuthenticatedRequestOwner(owner)).requireBody()
                    session.requireUnchanged(owner)
                    uploader.upload(uploadFile, target)
                    row = row.copy(uploaded = true)
                    dao.upsert(row)
                }
                // A lost registration response only resends this metadata: the frozen file is not uploaded again.
                session.requireUnchanged(owner)
                val receipt = service.register(RegisterVoucherRequestDto(row.id, row.reservationId, "image/jpeg", uploadFile.length(),
                    requireNotNull(data.amount), requireNotNull(data.currency), requireNotNull(data.operationDate),
                    requireNotNull(data.operationCode), data.confidence, row.manuallyCorrected), AuthenticatedRequestOwner(owner)).requireBody()
                check(receipt.voucherId == row.id && receipt.reservationId == row.reservationId && receipt.status == "SYNCED" &&
                    receipt.result in setOf("RECEIVED", "DUPLICATE")) { "El servidor no confirmó el voucher esperado." }
                session.requireUnchanged(owner)
                row = row.copy(status = "SYNCED", errorMessage = null)
                dao.upsert(row)
                row.toDomain()
            } catch (exception: kotlinx.coroutines.CancellationException) {
                throw exception
            } catch (exception: Exception) {
                dao.upsert(row.copy(errorMessage = exception.message ?: "No se pudo sincronizar el voucher."))
                throw exception
            }
        }
    }

    override suspend fun getPaymentEvidences(reservationId: String): Result<PaymentEvidences> = operationResult {
        val owner = session.requireOwner()
        requireNotNull(database.reservationDao().find(owner, reservationId)) { "No se encontró la separación." }
        val row = try {
            val response = service.paymentEvidences(reservationId, AuthenticatedRequestOwner(owner)).requireBody()
            check(response.transactionId == reservationId) { "El servidor devolvió otra separación." }
            val fetchedAt = utcTimestamp()
            response.toDomain(fetchedAt) // Validate dates before storing a usable cache.
            session.requireUnchanged(owner)
            PaymentEvidencesEntity(owner, reservationId, voucherGson.toJson(response), fetchedAt).also { dao.upsert(it) }
        } catch (exception: IOException) {
            if (exception is ApiOperationException) throw exception
            val cached = dao.paymentEvidences(owner, reservationId) ?: throw exception
            session.requireUnchanged(owner)
            return@operationResult voucherGson.fromJson(cached.payloadJson, PaymentEvidencesDto::class.java).toDomain(cached.fetchedAt, true)
        }
        voucherGson.fromJson(row.payloadJson, PaymentEvidencesDto::class.java).toDomain(row.fetchedAt)
    }

    companion object { private const val MAX_CAPTURE_BYTES = 40L * 1024 * 1024 }
}
