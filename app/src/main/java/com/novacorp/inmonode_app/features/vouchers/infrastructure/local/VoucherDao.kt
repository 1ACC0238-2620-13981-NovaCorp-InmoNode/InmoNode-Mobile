package com.novacorp.inmonode_app.features.vouchers.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface VoucherDao {
    @Query("SELECT * FROM vouchers WHERE ownerId = :ownerId ORDER BY capturedAt DESC, id")
    fun observeVouchers(ownerId: Long): Flow<List<VoucherEntity>>
    @Query("SELECT * FROM vouchers WHERE ownerId = :ownerId AND id = :id")
    suspend fun find(ownerId: Long, id: String): VoucherEntity?
    @Query("SELECT * FROM vouchers WHERE ownerId = :ownerId AND status = 'READY_TO_SYNC' ORDER BY capturedAt, id")
    suspend fun pending(ownerId: Long): List<VoucherEntity>
    @Query("SELECT * FROM vouchers WHERE ownerId = :ownerId AND reservationId = :reservationId AND status = 'READY_TO_SYNC'")
    suspend fun readyForReservation(ownerId: Long, reservationId: String): List<VoucherEntity>
    @Upsert suspend fun upsert(voucher: VoucherEntity)
    @Query("UPDATE vouchers SET reservationId = :newId WHERE ownerId = :ownerId AND reservationId = :oldId AND status != 'SYNCED'")
    suspend fun reassign(ownerId: Long, oldId: String, newId: String)
    @Query("SELECT * FROM payment_evidences WHERE ownerId = :ownerId AND reservationId = :reservationId")
    suspend fun paymentEvidences(ownerId: Long, reservationId: String): PaymentEvidencesEntity?
    @Upsert suspend fun upsert(evidences: PaymentEvidencesEntity)
}
