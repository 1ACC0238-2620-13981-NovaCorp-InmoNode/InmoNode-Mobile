package com.novacorp.inmonode_app.core.database

import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import com.novacorp.inmonode_app.features.vouchers.infrastructure.local.IdentityDocumentEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * T88: an agent who updates the app keeps what the previous version saved offline. Each earlier schema is
 * rebuilt from `app/schemas`, migrated to the current one, validated, and then opened the way the app opens it.
 */
class InmoNodeDatabaseMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val file = instrumentation.targetContext.getDatabasePath("migration-test.db")

    @get:Rule val helper = MigrationTestHelper(instrumentation, file, AndroidSQLiteDriver(), InmoNodeDatabase::class)

    @Before @After fun deleteDatabase() { instrumentation.targetContext.deleteDatabase(file.name) }

    @Test fun aSeparationSavedByVersion1SurvivesTheUpdate() = runBlocking<Unit> {
        helper.createDatabase(1).use { it.saveSeparationWithVoucher() }
        helper.runMigrationsAndValidate(CURRENT).close()

        openAsTheApp { database ->
            val separation = requireNotNull(database.reservationDao().find(OWNER, "r1"))
            assertEquals("PENDING_SYNC", separation.status)
            assertEquals("500.00", separation.initialAmount)
            // Version 2 added the negotiation terms (US-58): an older separation has none.
            assertNull(separation.discountAmount)
            assertNull(separation.bonus)
            assertNull(separation.negotiationNotes)
            assertEquals("r1", database.voucherDao().find(OWNER, "v1")?.reservationId)
            // Version 3 added the identity document (US-59): an older separation starts with it pending.
            assertNull(database.identityDocumentDao().forReservation(OWNER, "r1"))
        }
    }

    @Test fun negotiationTermsSavedByVersion2SurviveAndThePhotoCanBeAttached() = runBlocking<Unit> {
        helper.createDatabase(2).use {
            it.saveSeparationWithVoucher()
            it.execSQL("UPDATE reservations SET discountAmount = '1500.00', bonus = 'Cerco perimétrico', " +
                "negotiationNotes = 'Aprobado por gerencia' WHERE ownerId = $OWNER AND id = 'r1'")
        }
        helper.runMigrationsAndValidate(CURRENT).close()

        openAsTheApp { database ->
            val separation = requireNotNull(database.reservationDao().find(OWNER, "r1"))
            assertEquals("1500.00", separation.discountAmount)
            assertEquals("Cerco perimétrico", separation.bonus)
            assertEquals("Aprobado por gerencia", separation.negotiationNotes)
            database.identityDocumentDao().upsert(IdentityDocumentEntity(ownerId = OWNER, id = "d1", reservationId = "r1",
                imagePath = "/data/identity-documents/d1.jpg", capturedAt = "2026-11-21T10:10:00Z"))
            assertEquals("d1", database.identityDocumentDao().forReservation(OWNER, "r1")?.id)
        }
    }

    /** The prospect, the separation waiting to sync and its reviewed voucher, with the columns every version has. */
    private fun SQLiteConnection.saveSeparationWithVoucher() {
        execSQL("INSERT INTO prospects (ownerId, id, document, fullName, phone, maritalStatus, registeredAt, synced) " +
            "VALUES ($OWNER, 'p1', '12345678', 'Prospecto de prueba', '900000000', 'SOLTERO', '2026-11-21T10:00:00Z', 1)")
        execSQL("INSERT INTO reservations (ownerId, id, lotId, prospectId, initialAmount, reservedAt, status) " +
            "VALUES ($OWNER, 'r1', 10, 'p1', '500.00', '2026-11-21T10:05:00Z', 'PENDING_SYNC')")
        execSQL("INSERT INTO vouchers (ownerId, id, reservationId, imagePath, dataJson, manuallyCorrected, status, capturedAt, uploaded) " +
            "VALUES ($OWNER, 'v1', 'r1', '/data/vouchers/v1.jpg', '{}', 0, 'READY_TO_SYNC', '2026-11-21T10:04:00Z', 0)")
    }

    private suspend fun openAsTheApp(block: suspend (InmoNodeDatabase) -> Unit) {
        val database = Room.databaseBuilder<InmoNodeDatabase>(instrumentation.targetContext, file.absolutePath).build()
        try { block(database) } finally { database.close() }
    }

    private companion object {
        const val OWNER = 7L
        const val CURRENT = 3
    }
}
