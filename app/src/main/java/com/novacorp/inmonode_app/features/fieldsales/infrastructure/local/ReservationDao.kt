package com.novacorp.inmonode_app.features.fieldsales.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReservationDao {
    @Query("SELECT * FROM reservations WHERE ownerId = :ownerId ORDER BY reservedAt DESC, id")
    fun observeReservations(ownerId: Long): Flow<List<ReservationEntity>>
    @Query("SELECT * FROM reservations WHERE ownerId = :ownerId AND status IN ('PENDING_SYNC', 'FAILED') ORDER BY reservedAt, id LIMIT 200")
    suspend fun pending(ownerId: Long): List<ReservationEntity>
    @Query("SELECT * FROM reservations WHERE ownerId = :ownerId AND id = :id")
    suspend fun find(ownerId: Long, id: String): ReservationEntity?
    @Query("SELECT * FROM reservations WHERE ownerId = :ownerId AND lotId = :lotId AND status IN ('DRAFT', 'PENDING_SYNC', 'FAILED', 'FAILED_VALIDATION', 'SYNCED')")
    suspend fun activeForLot(ownerId: Long, lotId: Long): List<ReservationEntity>
    @Upsert suspend fun upsert(reservation: ReservationEntity)
}
