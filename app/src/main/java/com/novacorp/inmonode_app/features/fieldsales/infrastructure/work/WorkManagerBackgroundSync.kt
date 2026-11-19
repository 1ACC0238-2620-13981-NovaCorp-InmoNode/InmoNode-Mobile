package com.novacorp.inmonode_app.features.fieldsales.infrastructure.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.novacorp.inmonode_app.features.fieldsales.application.BackgroundSync
import com.novacorp.inmonode_app.features.fieldsales.application.BackgroundSyncState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerBackgroundSync @Inject constructor(@ApplicationContext private val context: Context) : BackgroundSync {

    private val workManager get() = WorkManager.getInstance(context)

    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<FieldSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, FIRST_RETRY_SECONDS, TimeUnit.SECONDS)
            .build()
        // A record saved while a sync runs gets its own run afterwards instead of waiting for the next change.
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    // Lazy: this class can be built while Hilt is still injecting the Application, before the worker factory that
    // WorkManager's on-demand initialization reads is set.
    override val state: Flow<BackgroundSyncState> by lazy {
        workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME).map { it.toBackgroundSyncState() }.distinctUntilChanged()
    }

    companion object {
        const val WORK_NAME = "field-sync"
        /** Render's free tier can take over a minute to wake up; doubling from 30 s gives it time. */
        const val FIRST_RETRY_SECONDS = 30L
    }
}

internal fun List<WorkInfo>.toBackgroundSyncState(): BackgroundSyncState {
    val waiting = filter { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED }
    return when {
        any { it.state == WorkInfo.State.RUNNING } -> BackgroundSyncState.Running
        waiting.any { it.runAttemptCount > 0 } -> BackgroundSyncState.RetryScheduled(waiting.maxOf { it.runAttemptCount })
        waiting.isNotEmpty() -> BackgroundSyncState.WaitingForNetwork
        else -> BackgroundSyncState.Idle
    }
}
