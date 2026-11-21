package com.novacorp.inmonode_app.features.fieldsales

import com.novacorp.inmonode_app.features.vouchers.domain.IdentityDocument
import com.novacorp.inmonode_app.features.vouchers.domain.IdentityDocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Counts identity document cleanups and serves [documents]; capturing is not used by the field-sales tests. */
internal class IdentityDocumentRepositoryFake(private val discardResult: Result<Unit> = Result.success(Unit),
    private val documents: Flow<List<IdentityDocument>> = flowOf(emptyList())) : IdentityDocumentRepository {
    var discards = 0
    override suspend fun discardCancelled(): Result<Unit> { discards++; return discardResult }
    override fun observeDocuments(): Flow<List<IdentityDocument>> = documents
    override suspend fun capture(reservationId: String, imagePath: String): Result<IdentityDocument> = error("unused")
    override suspend fun upload(documentId: String): Result<IdentityDocument> = error("unused")
}
