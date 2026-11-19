package com.novacorp.inmonode_app.features.fieldsales

import androidx.work.Data
import androidx.work.WorkInfo
import com.novacorp.inmonode_app.core.network.ApiOperationException
import com.novacorp.inmonode_app.features.fieldsales.application.BackgroundSyncState
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.work.SyncAttemptOutcome
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.work.syncAttemptOutcome
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.work.toBackgroundSyncState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.util.UUID

class BackgroundSyncTest {

    @Test fun successfulSyncIsDone() {
        assertEquals(SyncAttemptOutcome.DONE, syncAttemptOutcome(Result.success(Unit)))
    }

    @Test fun timeoutAndNetworkFailuresAreRetried() {
        // US-11 scenario 2: the 15 s timeout of FieldSyncRepositoryImpl surfaces as an IOException.
        val timeout = IOException("La sincronización se pausó por red inestable. Puedes reintentar sin duplicar registros.")
        assertEquals(SyncAttemptOutcome.RETRY, syncAttemptOutcome(Result.failure<Unit>(timeout)))
        assertEquals(SyncAttemptOutcome.RETRY, syncAttemptOutcome(Result.failure<Unit>(IOException("No se pudo conectar."))))
    }

    @Test fun serverErrorsAreRetriedAndRejectionsStop() {
        for (code in listOf(500, 502, 503, 408, 429)) {
            assertEquals("HTTP $code", SyncAttemptOutcome.RETRY, syncAttemptOutcome(Result.failure<Unit>(ApiOperationException(code, "x"))))
        }
        for (code in listOf(400, 401, 403, 409)) {
            assertEquals("HTTP $code", SyncAttemptOutcome.STOP, syncAttemptOutcome(Result.failure<Unit>(ApiOperationException(code, "x"))))
        }
    }

    @Test fun missingSessionStops() {
        val error = IllegalStateException("Inicia sesión como agente de campo.")
        assertEquals(SyncAttemptOutcome.STOP, syncAttemptOutcome(Result.failure<Unit>(error)))
    }

    @Test fun workStatesMapToWhatTheAgentSees() {
        assertEquals(BackgroundSyncState.Idle, emptyList<WorkInfo>().toBackgroundSyncState())
        assertEquals(BackgroundSyncState.Idle, listOf(work(WorkInfo.State.SUCCEEDED), work(WorkInfo.State.FAILED)).toBackgroundSyncState())
        assertEquals(BackgroundSyncState.WaitingForNetwork, listOf(work(WorkInfo.State.ENQUEUED)).toBackgroundSyncState())
        assertEquals(BackgroundSyncState.Running, listOf(work(WorkInfo.State.RUNNING), work(WorkInfo.State.BLOCKED)).toBackgroundSyncState())
        assertEquals(BackgroundSyncState.RetryScheduled(2),
            listOf(work(WorkInfo.State.ENQUEUED, attempts = 2), work(WorkInfo.State.BLOCKED)).toBackgroundSyncState())
    }

    private fun work(state: WorkInfo.State, attempts: Int = 0) =
        WorkInfo(UUID.randomUUID(), state, emptySet(), Data.EMPTY, Data.EMPTY, attempts)
}
