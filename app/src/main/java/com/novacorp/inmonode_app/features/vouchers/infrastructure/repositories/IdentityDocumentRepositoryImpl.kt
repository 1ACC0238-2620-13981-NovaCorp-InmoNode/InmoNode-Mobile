package com.novacorp.inmonode_app.features.vouchers.infrastructure.repositories

import android.content.Context
import com.novacorp.inmonode_app.core.database.InmoNodeDatabase
import com.novacorp.inmonode_app.core.database.atomic
import com.novacorp.inmonode_app.core.network.AuthenticatedRequestOwner
import com.novacorp.inmonode_app.core.network.operationResult
import com.novacorp.inmonode_app.core.network.requireBody
import com.novacorp.inmonode_app.core.time.utcTimestamp
import com.novacorp.inmonode_app.features.iam.infrastructure.local.FieldSession
import com.novacorp.inmonode_app.features.vouchers.domain.IdentityDocument
import com.novacorp.inmonode_app.features.vouchers.domain.IdentityDocumentRepository
import com.novacorp.inmonode_app.features.vouchers.infrastructure.image.ImageCompressor
import com.novacorp.inmonode_app.features.vouchers.infrastructure.local.IdentityDocumentEntity
import com.novacorp.inmonode_app.features.vouchers.infrastructure.local.toDomain
import com.novacorp.inmonode_app.features.vouchers.infrastructure.remote.IdentityDocumentFileDto
import com.novacorp.inmonode_app.features.vouchers.infrastructure.remote.IdentityDocumentService
import com.novacorp.inmonode_app.features.vouchers.infrastructure.remote.VoucherFileUploader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class IdentityDocumentRepositoryImpl @Inject constructor(
    private val database: InmoNodeDatabase,
    private val session: FieldSession,
    private val service: IdentityDocumentService,
    private val uploader: VoucherFileUploader,
    private val compressor: ImageCompressor,
    @ApplicationContext private val context: Context,
) : IdentityDocumentRepository {
    private val dao = database.identityDocumentDao()
    private val mutationMutex = Mutex()

    override fun observeDocuments(): Flow<List<IdentityDocument>> = session.ownerIds.flatMapLatest { owner ->
        if (owner == null) flowOf(emptyList()) else dao.observeDocuments(owner).map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun capture(reservationId: String, imagePath: String): Result<IdentityDocument> = operationResult {
        mutationMutex.withLock {
            val owner = session.requireOwner()
            val reservation = requireNotNull(database.reservationDao().find(owner, reservationId)) { "No se encontró la separación." }
            check(reservation.status in setOf("DRAFT", "PENDING_SYNC", "SYNCED", "FAILED")) {
                "Resuelve el conflicto de la separación antes de adjuntar el documento de identidad."
            }
            val previous = dao.forReservation(owner, reservationId)
            check(previous == null || previous.toDomain().replaceable) {
                "El documento de identidad ya se envió y forma parte del expediente."
            }
            val source = File(imagePath)
            require(source.isFile && source.length() in 1..MAX_CAPTURE_BYTES) { "El archivo de imagen es inválido o demasiado grande." }
            val id = UUID.randomUUID().toString()
            // Compressed right away (US-08): the stored file is the one that will be uploaded, retries included.
            val destination = compressor.compress(source, File(context.filesDir, "identity-documents/$owner/$id.jpg"),
                subject = SUBJECT)
            val row = IdentityDocumentEntity(owner, id, reservationId, destination.path, capturedAt = utcTimestamp())
            try {
                session.requireUnchanged(owner)
                database.atomic {
                    previous?.let { dao.delete(owner, listOf(it.id)) }
                    dao.upsert(row)
                }
            } catch (exception: Exception) {
                withContext(NonCancellable + Dispatchers.IO) { destination.delete() }
                throw exception
            }
            // The retaken photo replaces the previous one, which never reached the server.
            previous?.let { withContext(NonCancellable + Dispatchers.IO) { File(it.imagePath).delete() } }
            row.toDomain()
        }
    }

    override suspend fun upload(documentId: String): Result<IdentityDocument> = operationResult {
        mutationMutex.withLock {
            val owner = session.requireOwner()
            var row = requireNotNull(dao.find(owner, documentId)) { "No se encontró el documento de identidad." }
            if (row.status == "SYNCED") return@withLock row.toDomain()
            check(database.reservationDao().find(owner, row.reservationId)?.status == "SYNCED") {
                "Sincroniza la separación antes de enviar su documento de identidad."
            }
            try {
                val file = File(row.imagePath)
                check(file.isFile && file.length() in 1 until ImageCompressor.MAX_BYTES) {
                    "No se encontró la foto del documento de identidad. Vuelve a capturarla."
                }
                val request = IdentityDocumentFileDto(row.id, CONTENT_TYPE, file.length())
                session.requireUnchanged(owner)
                if (!row.uploaded) {
                    val target = service.requestUpload(row.reservationId, request, AuthenticatedRequestOwner(owner)).requireBody()
                    session.requireUnchanged(owner)
                    uploader.upload(file, target, subject = SUBJECT)
                    row = row.copy(uploaded = true)
                    dao.upsert(row)
                }
                // A lost registration response only resends these values: the photo is not uploaded again.
                session.requireUnchanged(owner)
                val receipt = service.register(row.reservationId, request, AuthenticatedRequestOwner(owner)).requireBody()
                check(receipt.documentId == row.id && receipt.transactionId == row.reservationId &&
                    receipt.result in setOf("RECEIVED", "DUPLICATE")) { "El servidor no confirmó el documento de identidad esperado." }
                session.requireUnchanged(owner)
                row = row.copy(status = "SYNCED", errorMessage = null)
                dao.upsert(row)
                row.toDomain()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                dao.upsert(row.copy(errorMessage = exception.message ?: "No se pudo enviar el documento de identidad."))
                throw exception
            }
        }
    }

    override suspend fun discardCancelled(): Result<Unit> = operationResult {
        mutationMutex.withLock {
            val owner = session.requireOwner()
            val rows = dao.ofCancelledReservations(owner)
            if (rows.isEmpty()) return@withLock
            dao.delete(owner, rows.map { it.id })
            // Files go only after their rows, so a failed delete never leaves a row pointing at a missing image.
            withContext(NonCancellable + Dispatchers.IO) { rows.forEach { File(it.imagePath).delete() } }
        }
    }

    companion object {
        private const val MAX_CAPTURE_BYTES = 40L * 1024 * 1024
        /** ImageCompressor always writes a JPEG. */
        private const val CONTENT_TYPE = "image/jpeg"
        private const val SUBJECT = "documento de identidad"
    }
}
