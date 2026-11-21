package com.novacorp.inmonode_app.features.vouchers.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface IdentityDocumentDao {
    @Query("SELECT * FROM identity_documents WHERE ownerId = :ownerId ORDER BY capturedAt, id")
    fun observeDocuments(ownerId: Long): Flow<List<IdentityDocumentEntity>>
    @Query("SELECT * FROM identity_documents WHERE ownerId = :ownerId AND id = :id")
    suspend fun find(ownerId: Long, id: String): IdentityDocumentEntity?
    @Query("SELECT * FROM identity_documents WHERE ownerId = :ownerId AND reservationId = :reservationId")
    suspend fun forReservation(ownerId: Long, reservationId: String): IdentityDocumentEntity?
    @Upsert suspend fun upsert(document: IdentityDocumentEntity)
    @Query("""SELECT identity_documents.* FROM identity_documents INNER JOIN reservations
        ON reservations.ownerId = identity_documents.ownerId AND reservations.id = identity_documents.reservationId
        WHERE identity_documents.ownerId = :ownerId AND reservations.status = 'CANCELLED'
        AND identity_documents.status != 'SYNCED'""")
    suspend fun ofCancelledReservations(ownerId: Long): List<IdentityDocumentEntity>
    @Query("DELETE FROM identity_documents WHERE ownerId = :ownerId AND id IN (:ids)")
    suspend fun delete(ownerId: Long, ids: List<String>)
    /** A conflict moves its photo to the replacement separation, as it does with the voucher. */
    @Query("UPDATE identity_documents SET reservationId = :newId WHERE ownerId = :ownerId AND reservationId = :oldId AND status != 'SYNCED'")
    suspend fun reassign(ownerId: Long, oldId: String, newId: String)
}
