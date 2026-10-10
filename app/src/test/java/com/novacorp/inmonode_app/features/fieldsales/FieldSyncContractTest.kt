package com.novacorp.inmonode_app.features.fieldsales

import com.google.gson.Gson
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.local.ReservationEntity
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.local.holdsLot
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.remote.*
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.repositories.validateSyncResponse
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class FieldSyncContractTest {
    private val reservation = ReservationRecordDto("a", 10, "p", BigDecimal("100.25"), "2026-10-09T12:00:00Z")
    private val request = FieldSyncRequestDto(emptyList(), listOf(reservation))
    private fun result(id: String = "a", status: String = "SYNCED", original: String? = null) =
        ReservationResultDto(id, status, "BLOCKED", "2026-10-10T12:00:00Z", null, original)

    @Test fun duplicateOfConflictMustNeverBecomeSynced() {
        assertEquals("CONFLICT", result(status = "DUPLICATE", original = "CONFLICT").effectiveResult())
        assertEquals("SYNCED", result(status = "DUPLICATE", original = "SYNCED").effectiveResult())
        assertTrue(runCatching { result(status = "DUPLICATE").effectiveResult() }.isFailure)
    }

    @Test fun partialDuplicateAndForeignAcknowledgementsAreRejected() {
        assertTrue(runCatching { validateSyncResponse(request, FieldSyncResultDto(0, emptyList())) }.isFailure)
        assertTrue(runCatching { validateSyncResponse(request, FieldSyncResultDto(0, listOf(result("other")))) }.isFailure)
        assertTrue(runCatching { validateSyncResponse(request, FieldSyncResultDto(0, listOf(result(), result()))) }.isFailure)
        assertTrue(runCatching { validateSyncResponse(request, FieldSyncResultDto(0, listOf(result(status = "UNKNOWN")))) }.isFailure)
        validateSyncResponse(request, FieldSyncResultDto(0, listOf(result())))
    }

    @Test fun pendingClaimSurvivesRedownloadAndExpiredServerHoldDoesNot() {
        val now = Instant.parse("2026-10-09T12:00:00Z")
        val row = ReservationEntity(1, "a", 10, "p", "100", now.toString())
        assertTrue(row.holdsLot(now))
        assertFalse(row.copy(status = "CONFLICT").holdsLot(now))
        assertFalse(row.copy(status = "SYNCED", blockedUntil = now.minusSeconds(1).toString()).holdsLot(now))
        assertTrue(row.copy(status = "SYNCED", blockedUntil = now.plusSeconds(1).toString()).holdsLot(now))
        assertTrue(row.copy(status = "SYNCED", serverStatus = "VERIFIED").holdsLot(now))
    }

    @Test fun serializationKeepsBackendNamesAndNumericMoney() {
        val json = Gson().toJsonTree(request).asJsonObject
        assertTrue(json.has("prospects") && json.has("reservations"))
        val record = json.getAsJsonArray("reservations").first().asJsonObject
        assertTrue(record["initialAmount"].asJsonPrimitive.isNumber)
        assertEquals(BigDecimal("100.25"), record["initialAmount"].asBigDecimal)
        assertEquals("2026-10-09T12:00:00Z", record["reservedAt"].asString)
        assertFalse(record.has("ownerId"))
    }
}
