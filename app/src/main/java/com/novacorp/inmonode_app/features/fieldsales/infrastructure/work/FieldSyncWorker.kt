package com.novacorp.inmonode_app.features.fieldsales.infrastructure.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.novacorp.inmonode_app.core.network.ApiOperationException
import com.novacorp.inmonode_app.features.fieldsales.application.SyncFieldRecordsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

/** Runs the same sync as "Sincronizar ahora" from WorkManager, so it also happens with the app closed (US-11). */
@HiltWorker
class FieldSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sync: SyncFieldRecordsUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = when (syncAttemptOutcome(sync())) {
        SyncAttemptOutcome.DONE -> Result.success()
        SyncAttemptOutcome.RETRY -> Result.retry()
        SyncAttemptOutcome.STOP -> Result.failure()
    }
}

internal enum class SyncAttemptOutcome { DONE, RETRY, STOP }

/**
 * Network failures, the 15 s timeout of US-11 scenario 2 and server-side errors are retried with backoff; the
 * records stay pending meanwhile. A rejected request or a missing session cannot fix itself, so it stops.
 */
internal fun syncAttemptOutcome(result: kotlin.Result<*>): SyncAttemptOutcome {
    val error = result.exceptionOrNull() ?: return SyncAttemptOutcome.DONE
    return when (error) {
        is ApiOperationException ->
            if (error.statusCode == 408 || error.statusCode == 429 || error.statusCode >= 500) SyncAttemptOutcome.RETRY
            else SyncAttemptOutcome.STOP
        is IOException -> SyncAttemptOutcome.RETRY
        else -> SyncAttemptOutcome.STOP
    }
}
