package com.novacorp.inmonode_app.features.vouchers.domain

import kotlinx.coroutines.flow.Flow

/** The photo of the buyer's identity document of each separation (US-59); at most one per separation. */
interface IdentityDocumentRepository {
    fun observeDocuments(): Flow<List<IdentityDocument>>
    /** Compresses the photo and keeps it with the separation; a new capture replaces one that was not sent yet. */
    suspend fun capture(reservationId: String, imagePath: String): Result<IdentityDocument>
    /** Deletes the photos, and their files, of separations cancelled before they were synchronized. */
    suspend fun discardCancelled(): Result<Unit>
}
