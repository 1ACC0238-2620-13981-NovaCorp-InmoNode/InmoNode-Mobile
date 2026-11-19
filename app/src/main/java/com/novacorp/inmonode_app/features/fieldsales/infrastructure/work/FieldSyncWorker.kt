package com.novacorp.inmonode_app.features.fieldsales.infrastructure.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.novacorp.inmonode_app.features.fieldsales.application.SyncFieldRecordsUseCase
import com.novacorp.inmonode_app.features.fieldsales.application.isRetryableSyncFailure
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Runs the same sync as "Sincronizar ahora" from WorkManager, so it also happens with the app closed (US-11). */
@HiltWorker
class FieldSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sync: SyncFieldRecordsUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val error = sync().exceptionOrNull() ?: return Result.success()
        // Retrying keeps the records pending; WorkManager waits with exponential backoff and for a connection.
        return if (isRetryableSyncFailure(error)) Result.retry() else Result.failure()
    }
}
