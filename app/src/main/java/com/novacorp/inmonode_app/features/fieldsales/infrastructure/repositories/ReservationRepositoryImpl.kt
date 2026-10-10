package com.novacorp.inmonode_app.features.fieldsales.infrastructure.repositories

import com.novacorp.inmonode_app.core.database.InmoNodeDatabase
import com.novacorp.inmonode_app.core.database.atomic
import com.novacorp.inmonode_app.core.network.operationResult
import com.novacorp.inmonode_app.features.fieldsales.domain.model.Reservation
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ReservationRepository
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.local.*
import com.novacorp.inmonode_app.features.iam.infrastructure.local.FieldSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import com.novacorp.inmonode_app.core.time.utcTimestamp
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class ReservationRepositoryImpl @Inject constructor(private val database: InmoNodeDatabase,
    private val session: FieldSession) : ReservationRepository {
    private val dao = database.reservationDao()
    override fun observeReservations(): Flow<List<Reservation>> = session.ownerIds.flatMapLatest { owner ->
        if (owner == null) flowOf(emptyList()) else dao.observeReservations(owner).map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun createDraft(lotId: Long, prospectId: String, initialAmount: BigDecimal): Result<Reservation> = operationResult {
        val owner = session.requireOwner()
        database.atomic { create(owner, lotId, prospectId, initialAmount, "DRAFT").toDomain() }
    }

    override suspend fun confirmDraft(reservationId: String): Result<Reservation> = operationResult {
        val owner = session.requireOwner()
        database.atomic {
            val row = requireNotNull(dao.find(owner, reservationId)) { "No se encontró la separación." }
            check(row.status == "DRAFT") { "La separación ya fue confirmada o cancelada." }
            val prospect = requireNotNull(database.prospectDao().find(owner, row.prospectId)) { "No se encontró el prospecto." }
            check(!prospect.maritalStatus.isNullOrBlank()) { "Completa el estado civil del prospecto para el contrato preliminar." }
            val lot = requireNotNull(database.lotDao().find(owner, row.lotId)) { "El lote ya no está en el portafolio." }
            check(lot.status == "AVAILABLE") { "El lote ya no está disponible. Selecciona otro lote." }
            check(database.voucherDao().readyForReservation(owner, row.id).isNotEmpty()) { "Captura y revisa el voucher antes de confirmar la separación." }
            row.copy(status = "PENDING_SYNC").also { dao.upsert(it) }.toDomain()
        }
    }

    override suspend fun cancelDraft(reservationId: String): Result<Unit> = operationResult {
        val owner = session.requireOwner()
        database.atomic {
            val row = requireNotNull(dao.find(owner, reservationId)) { "No se encontró la separación." }
            check(row.status == "DRAFT") { "Solo se puede cancelar un borrador local." }
            dao.upsert(row.copy(status = "CANCELLED"))
        }
    }

    private suspend fun create(owner: Long, lotId: Long, prospectId: String, amount: BigDecimal, status: String): ReservationEntity {
        val lot = requireNotNull(database.lotDao().find(owner, lotId)) { "Descarga el portafolio antes de separar este lote." }
        check(lot.status == "AVAILABLE" && dao.activeForLot(owner, lotId).none { it.holdsLot() }) { "El lote ya no está disponible." }
        requireNotNull(database.prospectDao().find(owner, prospectId)) { "Selecciona un prospecto registrado." }
        require(amount > BigDecimal.ZERO && amount <= lot.price.toBigDecimal()) { "El monto debe ser positivo y no superar el precio del lote." }
        require(amount.stripTrailingZeros().scale() <= 2) { "El monto debe tener como máximo dos decimales." }
        // The reservation endpoint validates a positive separation amount; financing minimums apply to simulation only.
        return ReservationEntity(owner, UUID.randomUUID().toString(), lotId, prospectId,
            amount.toPlainString(), utcTimestamp(), status = status).also { dao.upsert(it) }
    }

    override suspend fun reassign(reservationId: String, newLotId: Long): Result<Reservation> = operationResult {
        val owner = session.requireOwner()
        database.atomic {
            val original = requireNotNull(dao.find(owner, reservationId)) { "No se encontró la separación." }
            check(original.status == "CONFLICT" && original.conflictReason != "REASSIGNED") { "Esta separación no tiene un conflicto pendiente." }
            check(original.lotId != newLotId) { "Selecciona otro lote disponible." }
            // A conflicting id is immutable on the backend. Reusing it would return DUPLICATE/CONFLICT forever.
            val replacement = create(owner, newLotId, original.prospectId, original.initialAmount.toBigDecimal(), "PENDING_SYNC")
            database.voucherDao().reassign(owner, original.id, replacement.id)
            dao.upsert(original.copy(conflictReason = "REASSIGNED"))
            replacement.toDomain()
        }
    }
}
