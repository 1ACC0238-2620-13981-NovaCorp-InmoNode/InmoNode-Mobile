package com.novacorp.inmonode_app.features.vouchers.infrastructure.local

import androidx.room3.Entity
import androidx.room3.Index
import com.novacorp.inmonode_app.features.vouchers.domain.IdentityDocument
import com.novacorp.inmonode_app.features.vouchers.domain.IdentityDocumentStatus

/** US-59 (schema v3): the compressed photo of the buyer's identity document; one per separation. */
@Entity(tableName = "identity_documents", primaryKeys = ["ownerId", "id"],
    indices = [Index(value = ["ownerId", "reservationId"], unique = true)])
data class IdentityDocumentEntity(val ownerId: Long, val id: String, val reservationId: String,
    val imagePath: String, val status: String = "READY_TO_SYNC", val capturedAt: String,
    val uploaded: Boolean = false, val errorMessage: String? = null)

fun IdentityDocumentEntity.toDomain() = IdentityDocument(id, reservationId, imagePath,
    IdentityDocumentStatus.valueOf(status), capturedAt, uploaded, errorMessage)
