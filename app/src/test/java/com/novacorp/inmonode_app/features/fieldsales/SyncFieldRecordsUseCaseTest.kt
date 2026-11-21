package com.novacorp.inmonode_app.features.fieldsales

import com.novacorp.inmonode_app.features.fieldsales.application.SyncFieldRecordsUseCase
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.FieldSyncRepository
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ReservationRepository
import com.novacorp.inmonode_app.features.iam.domain.AuthRepository
import com.novacorp.inmonode_app.features.iam.domain.User
import com.novacorp.inmonode_app.features.iam.domain.UserRole
import com.novacorp.inmonode_app.features.vouchers.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

/**
 * The summary of "Sincronizar ahora" counts what this sync sent, vouchers and identity documents included (US-11, US-59).
 * A sync that cannot finish stops before sending anything else, and the records stay queued for the next one (T88).
 */
class SyncFieldRecordsUseCaseTest {
    private val auth = object : AuthRepository {
        override val currentUser = MutableStateFlow<User?>(User(1, "agent@example.test", UserRole.FIELD_AGENT))
        override suspend fun signIn(email: String, password: String): Result<User> = error("unused")
        override suspend fun signOut() = Unit
    }
    private var synchronizations = 0
    /** [during] runs while the separations are being sent, as if something happened in the meantime. */
    private fun fields(summary: SyncSummary, during: () -> Unit = {}) = object : FieldSyncRepository {
        override suspend fun synchronize(): Result<SyncSummary> { synchronizations++; during(); return Result.success(summary) }
    }
    private fun failingFields(error: Throwable) = object : FieldSyncRepository {
        override suspend fun synchronize(): Result<SyncSummary> { synchronizations++; return Result.failure(error) }
    }
    private val synced = Reservation("r1", 10, "p1", BigDecimal("500"), "2026-11-19T19:00:00Z", ReservationStatus.SYNCED)
    private val conflicted = Reservation("r2", 11, "p2", BigDecimal("500"), "2026-11-19T19:10:00Z", ReservationStatus.CONFLICT)
    /** What was sent, in order, across vouchers and identity documents. */
    private val sent = mutableListOf<String>()
    private val reservations = object : ReservationRepository {
        override fun observeReservations() = flowOf(listOf(synced, conflicted))
        override suspend fun createDraft(lotId: Long, prospectId: String, initialAmount: BigDecimal, terms: NegotiationTerms): Result<Reservation> = error("unused")
        override suspend fun confirmDraft(reservationId: String): Result<Reservation> = error("unused")
        override suspend fun cancelDraft(reservationId: String): Result<Unit> = error("unused")
        override suspend fun reassign(reservationId: String, newLotId: Long): Result<Reservation> = error("unused")
    }
    private fun vouchers(vararg pending: Voucher, unreachable: String? = null) = object : VoucherRepository {
        val uploaded = mutableListOf<String>()
        override fun observeVouchers() = flowOf(pending.toList())
        override suspend fun upload(voucherId: String): Result<Voucher> {
            if (voucherId == unreachable) return Result.failure(IOException("Sin conexión con el almacenamiento"))
            uploaded += voucherId
            sent += "voucher $voucherId"
            return Result.success(pending.first { it.id == voucherId }.copy(status = VoucherStatus.SYNCED))
        }
        override suspend fun discardCancelled() = Result.success(Unit)
        override suspend fun capture(reservationId: String, imagePath: String): Result<Voucher> = error("unused")
        override suspend fun processOcr(voucherId: String): Result<Voucher> = error("unused")
        override suspend fun review(voucherId: String, data: OcrData): Result<Voucher> = error("unused")
        override suspend fun getPaymentEvidences(reservationId: String): Result<PaymentEvidences> = error("unused")
    }
    private fun documents(vararg pending: IdentityDocument) = object : IdentityDocumentRepository {
        var discards = 0
        override fun observeDocuments() = flowOf(pending.toList())
        override suspend fun upload(documentId: String): Result<IdentityDocument> {
            sent += "document $documentId"
            return Result.success(pending.first { it.id == documentId }.copy(status = IdentityDocumentStatus.SYNCED))
        }
        override suspend fun discardCancelled(): Result<Unit> { discards++; return Result.success(Unit) }
        override suspend fun capture(reservationId: String, imagePath: String): Result<IdentityDocument> = error("unused")
    }

    @Test fun recordsAlreadySentByTheBackgroundSyncLeaveNothingToReport() = runTest {
        val summary = SyncFieldRecordsUseCase(fields(SyncSummary(0, 0, 0)), reservations, vouchers(), documents(), auth)().getOrThrow()
        assertTrue(summary.sentNothing)
    }

    @Test fun aVoucherUploadedForASyncedSeparationIsCounted() = runTest {
        val voucher = Voucher("v1", "r1", "voucher.jpg", status = VoucherStatus.READY_TO_SYNC, capturedAt = "2026-11-19T19:05:00Z")
        val receipts = vouchers(voucher)
        val summary = SyncFieldRecordsUseCase(fields(SyncSummary(0, 0, 0)), reservations, receipts, documents(), auth)().getOrThrow()
        assertEquals(listOf("v1"), receipts.uploaded)
        assertEquals(SyncSummary(0, 0, 0, vouchersUploaded = 1), summary)
        assertFalse(summary.sentNothing)
    }

    @Test fun theIdentityDocumentGoesAfterTheVoucherOfItsSyncedSeparation() = runTest {
        val voucher = Voucher("v1", "r1", "voucher.jpg", status = VoucherStatus.READY_TO_SYNC, capturedAt = "2026-11-19T19:05:00Z")
        val photo = IdentityDocument("d1", "r1", "d1.jpg", IdentityDocumentStatus.READY_TO_SYNC, "2026-11-19T19:04:00Z")
        val photos = documents(photo)

        val summary = SyncFieldRecordsUseCase(fields(SyncSummary(0, 1, 0)), reservations, vouchers(voucher), photos, auth)().getOrThrow()

        assertEquals(listOf("voucher v1", "document d1"), sent)
        assertEquals(SyncSummary(0, 1, 0, vouchersUploaded = 1, identityDocumentsUploaded = 1), summary)
        assertEquals("cancelled drafts lose their photo before each sync", 1, photos.discards)
    }

    @Test fun aPhotoWaitsWhileItsSeparationIsNotSyncedAndASentOneIsNotSentAgain() = runTest {
        val waiting = IdentityDocument("d2", "r2", "d2.jpg", IdentityDocumentStatus.READY_TO_SYNC, "2026-11-19T19:11:00Z")
        val alreadySent = IdentityDocument("d1", "r1", "d1.jpg", IdentityDocumentStatus.SYNCED, "2026-11-19T19:04:00Z")

        val summary = SyncFieldRecordsUseCase(fields(SyncSummary(0, 0, 0)), reservations, vouchers(),
            documents(waiting, alreadySent), auth)().getOrThrow()

        assertTrue(sent.isEmpty())
        assertTrue(summary.sentNothing)
    }

    @Test fun theVoucherAndPhotoOfAConflictAreNeverSent() = runTest {
        val voucher = Voucher("v2", "r2", "voucher.jpg", status = VoucherStatus.READY_TO_SYNC, capturedAt = "2026-11-19T19:12:00Z")
        val photo = IdentityDocument("d2", "r2", "d2.jpg", IdentityDocumentStatus.READY_TO_SYNC, "2026-11-19T19:11:00Z")

        val summary = SyncFieldRecordsUseCase(fields(SyncSummary(0, 0, 1)), reservations, vouchers(voucher), documents(photo), auth)().getOrThrow()

        assertTrue("a conflict keeps its voucher until the agent picks another lot", sent.isEmpty())
        assertEquals(SyncSummary(0, 0, 1), summary)
    }

    @Test fun onlyASignedInFieldAgentCanSync() = runTest {
        for (user in listOf(null, User(2, "buyer@example.test", UserRole.BUYER))) {
            auth.currentUser.value = user
            val result = SyncFieldRecordsUseCase(fields(SyncSummary(1, 1, 0)), reservations, vouchers(), documents(), auth)()
            assertEquals("Inicia sesión como agente de campo.", result.exceptionOrNull()?.message)
        }
        assertEquals("nothing is sent without a field agent", 0, synchronizations)
    }

    @Test fun aFailedSyncSendsNoVoucherNorPhotoAndKeepsItsCause() = runTest {
        val voucher = Voucher("v1", "r1", "voucher.jpg", status = VoucherStatus.READY_TO_SYNC, capturedAt = "2026-11-19T19:05:00Z")
        val photo = IdentityDocument("d1", "r1", "d1.jpg", IdentityDocumentStatus.READY_TO_SYNC, "2026-11-19T19:04:00Z")
        val timeout = IOException("timeout")

        val result = SyncFieldRecordsUseCase(failingFields(timeout), reservations, vouchers(voucher), documents(photo), auth)()

        // The cause is kept so the background sync can tell a retryable failure from a rejection.
        assertSame(timeout, result.exceptionOrNull())
        assertTrue(sent.isEmpty())
    }

    @Test fun aVoucherThatCannotBeSentStopsTheRecordsAfterIt() = runTest {
        val first = Voucher("v1", "r1", "v1.jpg", status = VoucherStatus.READY_TO_SYNC, capturedAt = "2026-11-19T19:05:00Z")
        val second = Voucher("v3", "r1", "v3.jpg", status = VoucherStatus.READY_TO_SYNC, capturedAt = "2026-11-19T19:06:00Z")
        val photo = IdentityDocument("d1", "r1", "d1.jpg", IdentityDocumentStatus.READY_TO_SYNC, "2026-11-19T19:04:00Z")

        val result = SyncFieldRecordsUseCase(fields(SyncSummary(0, 1, 0)), reservations,
            vouchers(second, first, unreachable = "v1"), documents(photo), auth)()

        assertEquals("Sin conexión con el almacenamiento", result.exceptionOrNull()?.message)
        assertTrue("the oldest voucher goes first and nothing is sent after it fails", sent.isEmpty())
    }

    @Test fun anotherAgentSigningInMidSyncGetsNothingOfThePreviousOne() = runTest {
        val voucher = Voucher("v1", "r1", "voucher.jpg", status = VoucherStatus.READY_TO_SYNC, capturedAt = "2026-11-19T19:05:00Z")
        val photo = IdentityDocument("d1", "r1", "d1.jpg", IdentityDocumentStatus.READY_TO_SYNC, "2026-11-19T19:04:00Z")
        val switch = fields(SyncSummary(0, 1, 0)) { auth.currentUser.value = User(2, "other@example.test", UserRole.FIELD_AGENT) }

        val result = SyncFieldRecordsUseCase(switch, reservations, vouchers(voucher), documents(photo), auth)()

        assertEquals("La sesión cambió. Tus registros siguen guardados para su agente original.", result.exceptionOrNull()?.message)
        assertTrue(sent.isEmpty())
    }

    @Test fun aSyncThatOnlySentAPhotoReportsIt() {
        assertFalse(SyncSummary(0, 0, 0, identityDocumentsUploaded = 1).sentNothing)
    }
}
