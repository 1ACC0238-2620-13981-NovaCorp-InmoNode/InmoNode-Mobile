package com.novacorp.inmonode_app.features.fieldsales.infrastructure.repositories

import com.novacorp.inmonode_app.core.database.InmoNodeDatabase
import com.novacorp.inmonode_app.core.database.atomic
import com.novacorp.inmonode_app.core.network.operationResult
import com.novacorp.inmonode_app.core.network.requireBody
import com.novacorp.inmonode_app.core.network.AuthenticatedRequestOwner
import com.novacorp.inmonode_app.core.time.utcTimestamp
import com.novacorp.inmonode_app.features.fieldsales.domain.model.SyncSummary
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.FieldSyncRepository
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.local.*
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.remote.*
import com.novacorp.inmonode_app.features.iam.infrastructure.local.FieldSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.time.Instant
import javax.inject.Inject

class FieldSyncRepositoryImpl @Inject constructor(private val database: InmoNodeDatabase,
    private val service: FieldSyncService, private val session: FieldSession) : FieldSyncRepository {
    private val syncMutex = Mutex()

    override suspend fun synchronize(): Result<SyncSummary> = operationResult {
        syncMutex.withLock {
            val owner = session.requireOwner()
            val prospects = database.prospectDao()
            val reservations = database.reservationDao()
            var totalProspects = 0
            var totalReservations = 0
            var totalConflicts = 0
            while (true) {
                session.requireUnchanged(owner)
                val pendingReservations = reservations.pending(owner)
                val requiredProspects = pendingReservations.map { it.prospectId }.distinct().map { id ->
                    requireNotNull(prospects.find(owner, id)) { "La separación no tiene un prospecto local." }
                }.filter { !it.synced }
                // Prioritize dependencies and respect backend maxima (500 prospects / 200 reservations).
                val pendingProspects = (requiredProspects + prospects.pending(owner, 500)).distinctBy { it.id }.take(500)
                if (pendingReservations.isEmpty() && pendingProspects.isEmpty()) break
                val request = FieldSyncRequestDto(pendingProspects.map { it.toRecord() }, pendingReservations.map { it.toRecord() })
                // A rejected or interrupted batch stays pending; no local record is acknowledged prematurely.
                val response =
                    withTimeoutOrNull(15_000) { service.synchronize(request, AuthenticatedRequestOwner(owner)).requireBody() }
                        ?: throw IOException("La sincronización se pausó por red inestable. Puedes reintentar sin duplicar registros.")
                validateSyncResponse(request, response)
                session.requireUnchanged(owner)
                database.atomic {
                    prospects.markSynced(owner, pendingProspects.map { it.id })
                    response.reservations.forEach { outcome ->
                        val row = pendingReservations.single { it.id == outcome.id }
                        val status = if (outcome.effectiveResult() == "SYNCED") "SYNCED" else "CONFLICT"
                        reservations.upsert(row.copy(status = status, blockedUntil = outcome.blockedUntil,
                            serverStatus = outcome.reservationStatus, conflictReason = outcome.conflictReason))
                        // Never return a conflicting lot to AVAILABLE: the server has reported it unavailable.
                        val lotStatus = when (outcome.reservationStatus) {
                            "SOLD" -> "SOLD"
                            "RESERVED", "VERIFIED" -> "RESERVED"
                            "PENDING_VERIFICATION" -> "PENDING_VERIFICATION"
                            else -> "BLOCKED"
                        }
                        database.lotDao().setStatus(owner, row.lotId, lotStatus)
                    }
                    val metadata = database.portfolioDao().metadata(owner) ?: SyncMetadataEntity(owner)
                    // A local lot status update diverges from the cached server representation, invalidating its ETag.
                    database.portfolioDao().upsert(metadata.copy(
                        etag = metadata.etag.takeIf { response.reservations.isEmpty() }, lastSyncedAt = utcTimestamp()))
                }
                totalProspects += response.prospectsSynced
                totalReservations += response.reservations.count { it.effectiveResult() == "SYNCED" }
                totalConflicts += response.reservations.count { it.effectiveResult() == "CONFLICT" }
            }
            SyncSummary(totalProspects, totalReservations, totalConflicts)
        }
    }
}

internal fun validateSyncResponse(request: FieldSyncRequestDto, response: FieldSyncResultDto) {
    require(response.prospectsSynced == request.prospects.size) { "El servidor no confirmó todos los prospectos." }
    val expected = request.reservations.map { it.id }.toSet()
    val received = response.reservations.map { it.id }
    require(received.size == expected.size && received.toSet() == expected) { "El servidor no confirmó todas las separaciones." }
    response.reservations.forEach { outcome ->
        require(outcome.effectiveResult() in setOf("SYNCED", "CONFLICT")) { "Resultado de sincronización desconocido." }
        outcome.blockedUntil?.let(Instant::parse)
    }
}
