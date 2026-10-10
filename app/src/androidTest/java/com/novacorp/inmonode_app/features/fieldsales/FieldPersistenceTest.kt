package com.novacorp.inmonode_app.features.fieldsales

import androidx.room3.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.novacorp.inmonode_app.core.database.InmoNodeDatabase
import com.novacorp.inmonode_app.core.network.AuthenticatedRequestOwner
import com.novacorp.inmonode_app.features.fieldsales.domain.model.ReservationStatus
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.local.*
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.remote.*
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.repositories.*
import com.novacorp.inmonode_app.features.iam.domain.*
import com.novacorp.inmonode_app.features.iam.infrastructure.local.FieldSession
import com.novacorp.inmonode_app.features.vouchers.domain.OcrData
import com.novacorp.inmonode_app.features.vouchers.infrastructure.local.VoucherEntity
import com.novacorp.inmonode_app.features.vouchers.infrastructure.local.voucherGson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.math.BigDecimal
import java.util.UUID

/** Runs against real Room/SQLite on an Android device, with deterministic API responses. */
class FieldPersistenceTest {
    private lateinit var database: InmoNodeDatabase
    private lateinit var auth: LocalAuth
    private lateinit var session: FieldSession
    private lateinit var service: LocalService
    private lateinit var portfolio: PortfolioRepositoryImpl
    private lateinit var prospects: ProspectRepositoryImpl
    private lateinit var reservations: ReservationRepositoryImpl
    private lateinit var sync: FieldSyncRepositoryImpl

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder<InmoNodeDatabase>(InstrumentationRegistry.getInstrumentation().targetContext).build()
        auth = LocalAuth()
        session = FieldSession(auth)
        service = LocalService()
        portfolio = PortfolioRepositoryImpl(database, service, session)
        prospects = ProspectRepositoryImpl(database, session)
        reservations = ReservationRepositoryImpl(database, session)
        sync = FieldSyncRepositoryImpl(database, service, session)
    }

    @After fun tearDown() { database.close() }

    @Test fun draftCannotSyncBeforeVoucherAndContractAreReady() = runBlocking {
        portfolio.download().getOrThrow()
        val prospect = prospects.register("12345678", "Ana Pérez", "", "Soltera").getOrThrow()
        val draft = reservations.createDraft(10, prospect.id, BigDecimal("200")).getOrThrow()
        assertEquals(ReservationStatus.DRAFT, draft.status)
        assertTrue(database.reservationDao().pending(1).isEmpty())
        assertTrue(reservations.confirmDraft(draft.id).isFailure)
        readyVoucher(draft.id)
        assertEquals(ReservationStatus.PENDING_SYNC, reservations.confirmDraft(draft.id).getOrThrow().status)
        assertEquals(1, database.reservationDao().pending(1).size)
    }

    @Test fun concurrentReservationsCannotClaimSameLot() = runBlocking {
        portfolio.download().getOrThrow()
        val prospect = prospects.register("12345678", "Ana Pérez", "", "Soltera").getOrThrow()
        val outcomes = (1..5).map {
            async(Dispatchers.Default) { reservations.createDraft(10, prospect.id, BigDecimal("200")) }
        }.awaitAll()
        assertEquals(1, outcomes.count { it.isSuccess })
        assertEquals(4, outcomes.count { it.isFailure })
        assertFalse(portfolio.observePortfolio().first().projects.single().lots.single().isAvailable())
    }

    @Test fun redownloadPreservesPendingClaimAndCancellationReleasesIt() = runBlocking {
        portfolio.download().getOrThrow()
        val prospect = prospects.register("12345678", "Ana Pérez", "", "Soltera").getOrThrow()
        val draft = reservations.createDraft(10, prospect.id, BigDecimal("200")).getOrThrow()
        portfolio.download().getOrThrow()
        assertFalse(portfolio.observePortfolio().first().projects.single().lots.single().isAvailable())
        reservations.cancelDraft(draft.id).getOrThrow()
        assertTrue(portfolio.observePortfolio().first().projects.single().lots.single().isAvailable())
        assertTrue(database.reservationDao().pending(1).isEmpty())
    }

    @Test fun usersOnlySeeAndSyncTheirOwnLocalRecords() = runBlocking {
        prospects.register("12345678", "Ana Pérez", "", null).getOrThrow()
        auth.currentUser.value = User(2, "second@example.com", UserRole.FIELD_AGENT)
        assertTrue(prospects.observeProspects().first().isEmpty())
        assertTrue(reservations.observeReservations().first().isEmpty())
        sync.synchronize().getOrThrow()
        assertTrue(service.requests.isEmpty())
        auth.currentUser.value = User(1, "agent@example.com", UserRole.FIELD_AGENT)
        assertEquals(1, prospects.observeProspects().first().size)
        assertFalse(prospects.observeProspects().first().single().synced)
    }

    @Test fun etag304RetainsStablePortfolioAndIncompleteDownloadCannotEraseIt() = runBlocking {
        val stable = portfolio.download().getOrThrow()
        database.portfolioDao().upsert(SyncMetadataEntity(1, "\"catalog-v1\"", stable.downloadedAt))
        service.notModified = true
        assertEquals(stable.projects, portfolio.download().getOrThrow().projects)
        assertEquals("\"catalog-v1\"", service.lastEtag)
        service.notModified = false
        service.catalog = service.catalog.copy(projects = service.catalog.projects.map { project ->
            project.copy(lots = project.lots.copy(features = project.lots.features.map { lot ->
                lot.copy(geometry = lot.geometry.copy(coordinates = emptyList()))
            }))
        })
        assertTrue(portfolio.download().isFailure)
        assertEquals(stable.projects, portfolio.observePortfolio().first().projects)
    }

    @Test fun interruptedOrIncompleteSyncNeverAcknowledgesLocalRows() = runBlocking {
        val reservation = pendingReservation()
        service.missingAcknowledgement = true
        assertTrue(sync.synchronize().isFailure)
        assertEquals("PENDING_SYNC", database.reservationDao().find(1, reservation)?.status)
        assertFalse(database.prospectDao().pending(1, 500).single().synced)
        service.missingAcknowledgement = false
        sync.synchronize().getOrThrow()
        assertEquals("SYNCED", database.reservationDao().find(1, reservation)?.status)
        assertTrue(database.prospectDao().pending(1, 500).isEmpty())
    }

    @Test fun duplicateConflictIsPreservedAndReassignmentUsesNewUuid() = runBlocking {
        val id = pendingReservation()
        service.conflict = true
        sync.synchronize().getOrThrow()
        assertEquals("CONFLICT", database.reservationDao().find(1, id)?.status)
        database.lotDao().upsert(listOf(database.lotDao().find(1, 10)!!.copy(id = 11, code = "A-2", status = "AVAILABLE")))
        val replacement = reservations.reassign(id, 11).getOrThrow()
        assertNotEquals(id, replacement.id)
        assertEquals("REASSIGNED", database.reservationDao().find(1, id)?.conflictReason)
        assertEquals(replacement.id, database.voucherDao().pending(1).single().reservationId)
        assertEquals("BLOCKED", database.lotDao().find(1, 10)?.status)
    }

    @Test fun prospectBatchesRespectBackendMaximum() = runBlocking {
        repeat(501) { index ->
            database.prospectDao().upsert(ProspectEntity(1, UUID.randomUUID().toString(), (10000000 + index).toString(),
                "Prospecto $index", "", null, "2026-10-09T12:00:00.000Z"))
        }
        assertEquals(501, sync.synchronize().getOrThrow().prospectsSynced)
        assertEquals(listOf(500, 1), service.requests.map { it.prospects.size })
        assertTrue(database.prospectDao().pending(1, 500).isEmpty())
    }

    private suspend fun pendingReservation(): String {
        portfolio.download().getOrThrow()
        val prospect = prospects.register("12345678", "Ana Pérez", "", "Soltera").getOrThrow()
        val draft = reservations.createDraft(10, prospect.id, BigDecimal("200")).getOrThrow()
        readyVoucher(draft.id)
        return reservations.confirmDraft(draft.id).getOrThrow().id
    }

    private suspend fun readyVoucher(reservationId: String) {
        database.voucherDao().upsert(VoucherEntity(1, UUID.randomUUID().toString(), reservationId, "test.jpg",
            voucherGson.toJson(OcrData(BigDecimal("200"), "PEN", "2026-10-09", "00012345")),
            status = "READY_TO_SYNC", capturedAt = "2026-10-09T12:00:00.000Z"))
    }

    private class LocalAuth : AuthRepository {
        override val currentUser = MutableStateFlow<User?>(User(1, "agent@example.com", UserRole.FIELD_AGENT))
        override suspend fun signIn(email: String, password: String): Result<User> = error("unused")
        override suspend fun signOut() { currentUser.value = null }
    }

    private class LocalService : FieldSyncService {
        var catalog = FieldPortfolioDto(listOf(PortfolioProjectDto(1, "Valle", "Lima", null, null, null,
            FinancingRulesDto(BigDecimal("20"), BigDecimal("12.5"), 120, BigDecimal("1")),
            LotFeatureCollectionDto("FeatureCollection", listOf(LotFeatureDto("Feature", 10,
                LotGeometryDto("Polygon", listOf(listOf(listOf(-77.0, -12.0), listOf(-76.9, -12.0),
                    listOf(-76.9, -11.9), listOf(-77.0, -12.0)))),
                LotPropertiesDto("A-1", BigDecimal("120"), BigDecimal("6"), BigDecimal("20"), BigDecimal("1000"), "PEN", "AVAILABLE")))))))
        var notModified = false
        var lastEtag: String? = null
        var missingAcknowledgement = false
        var conflict = false
        val requests = mutableListOf<FieldSyncRequestDto>()
        override suspend fun portfolio(etag: String?, owner: AuthenticatedRequestOwner): Response<FieldPortfolioDto> {
            lastEtag = etag
            if (!notModified) return Response.success(catalog)
            val raw = okhttp3.Response.Builder().request(Request.Builder().url("https://example.com").build())
                .protocol(Protocol.HTTP_1_1).code(304).message("Not Modified").build()
            return Response.error("".toResponseBody("application/json".toMediaType()), raw)
        }
        override suspend fun synchronize(records: FieldSyncRequestDto, owner: AuthenticatedRequestOwner): Response<FieldSyncResultDto> {
            requests += records
            return Response.success(FieldSyncResultDto(records.prospects.size,
                if (missingAcknowledgement) emptyList() else records.reservations.map {
                    ReservationResultDto(it.id, if (conflict) "DUPLICATE" else "SYNCED", "BLOCKED",
                        "2026-10-10T12:00:00Z", if (conflict) "LOT_UNAVAILABLE" else null, if (conflict) "CONFLICT" else null)
                }))
        }
    }
}
