package com.novacorp.inmonode_app.features.vouchers.infrastructure.repositories

import android.content.Context
import com.novacorp.inmonode_app.core.database.InmoNodeDatabase
import com.novacorp.inmonode_app.core.database.atomic
import com.novacorp.inmonode_app.core.network.operationResult
import com.novacorp.inmonode_app.core.time.utcTimestamp
import com.novacorp.inmonode_app.features.iam.infrastructure.local.FieldSession
import com.novacorp.inmonode_app.features.vouchers.domain.IdentityDocument
import com.novacorp.inmonode_app.features.vouchers.domain.IdentityDocumentRepository
import com.novacorp.inmonode_app.features.vouchers.infrastructure.image.ImageCompressor
import com.novacorp.inmonode_app.features.vouchers.infrastructure.local.IdentityDocumentEntity
import com.novacorp.inmonode_app.features.vouchers.infrastructure.local.toDomain
import dagger.hilt.android.qualifiers.ApplicationContext
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
                subject = "documento de identidad")
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

    companion object { private const val MAX_CAPTURE_BYTES = 40L * 1024 * 1024 }
}
