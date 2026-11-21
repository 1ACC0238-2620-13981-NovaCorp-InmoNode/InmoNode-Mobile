package com.novacorp.inmonode_app.features.vouchers

import androidx.room3.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.novacorp.inmonode_app.core.database.InmoNodeDatabase
import com.novacorp.inmonode_app.core.network.AuthenticatedRequestOwner
import com.novacorp.inmonode_app.features.iam.domain.*
import com.novacorp.inmonode_app.features.iam.infrastructure.local.FieldSession
import com.novacorp.inmonode_app.features.vouchers.domain.*
import com.novacorp.inmonode_app.features.vouchers.infrastructure.image.ImageCompressor
import com.novacorp.inmonode_app.features.vouchers.infrastructure.local.VoucherEntity
import com.novacorp.inmonode_app.features.vouchers.infrastructure.local.voucherGson
import com.novacorp.inmonode_app.features.vouchers.infrastructure.remote.*
import com.novacorp.inmonode_app.features.vouchers.infrastructure.repositories.VoucherRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.File
import java.math.BigDecimal
import java.util.UUID

/** An illegible capture leaves the sync queue once the agent confirms a legible replacement for its separation (T58). */
class VoucherReplacementTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: InmoNodeDatabase
    private lateinit var vouchers: VoucherRepositoryImpl
    private val legible = OcrData(BigDecimal("500.00"), "PEN", "2026-11-18", "7741208")

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder<InmoNodeDatabase>(context).build()
        vouchers = VoucherRepositoryImpl(database, FieldSession(LocalAuth()), UnusedService(), VoucherFileUploader(),
            ImageCompressor(), UnusedOcr(), context)
    }

    @After fun tearDown() { database.close() }

    @Test fun aConfirmedReplacementDiscardsTheIllegibleCaptureOfItsSeparation() = runBlocking {
        val illegible = voucher("separation-1", "RECAPTURE_REQUIRED")
        val ofAnotherSeparation = voucher("separation-2", "RECAPTURE_REQUIRED")
        val replacement = voucher("separation-1", "MANUAL_REVIEW_NEEDED")

        vouchers.review(replacement.id, legible).getOrThrow()

        val left = vouchers.observeVouchers().first().associate { it.id to it.status }
        assertEquals(mapOf(replacement.id to VoucherStatus.READY_TO_SYNC, ofAnotherSeparation.id to VoucherStatus.RECAPTURE_REQUIRED), left)
        assertFalse(File(illegible.imagePath).exists())
        assertTrue(File(ofAnotherSeparation.imagePath).exists())
    }

    @Test fun anIllegibleCaptureStaysWhileItsReplacementIsIncomplete() = runBlocking {
        val illegible = voucher("separation-1", "RECAPTURE_REQUIRED")
        val replacement = voucher("separation-1", "MANUAL_REVIEW_NEEDED")

        assertTrue(vouchers.review(replacement.id, legible.copy(operationCode = null)).isFailure)

        assertEquals(setOf(illegible.id, replacement.id), vouchers.observeVouchers().first().map { it.id }.toSet())
        assertTrue(File(illegible.imagePath).exists())
    }

    private suspend fun voucher(reservationId: String, status: String): VoucherEntity {
        val id = UUID.randomUUID().toString()
        val image = File(context.cacheDir, "voucher-replacement-test/$id.jpg").apply { parentFile?.mkdirs(); writeBytes(byteArrayOf(1)) }
        return VoucherEntity(1, id, reservationId, image.path, voucherGson.toJson(OcrData()), status = status,
            capturedAt = "2026-11-20T12:00:00.000Z").also { database.voucherDao().upsert(it) }
    }

    private class LocalAuth : AuthRepository {
        override val currentUser = MutableStateFlow<User?>(User(1, "agent@example.com", UserRole.FIELD_AGENT))
        override suspend fun signIn(email: String, password: String): Result<User> = error("unused")
        override suspend fun signOut() { currentUser.value = null }
    }

    private class UnusedOcr : OcrEngine {
        override suspend fun recognize(imagePath: String): Result<OcrReading> = error("unused")
    }

    private class UnusedService : VoucherService {
        override suspend fun requestUpload(request: UploadUrlRequestDto, owner: AuthenticatedRequestOwner): Response<UploadUrlResponseDto> = error("unused")
        override suspend fun register(request: RegisterVoucherRequestDto, owner: AuthenticatedRequestOwner): Response<VoucherRegistrationDto> = error("unused")
        override suspend fun paymentEvidences(transactionId: String, owner: AuthenticatedRequestOwner): Response<PaymentEvidencesDto> = error("unused")
    }
}
