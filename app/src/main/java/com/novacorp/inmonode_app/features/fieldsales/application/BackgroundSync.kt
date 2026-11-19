package com.novacorp.inmonode_app.features.fieldsales.application

import com.novacorp.inmonode_app.core.network.ApiOperationException
import kotlinx.coroutines.flow.Flow
import java.io.IOException

/** Sends the pending field records once the device is online, even while the app is closed (US-11). */
interface BackgroundSync {
    /** Queues a sync that waits for a connection and retries with backoff after network failures or timeouts. */
    fun schedule()

    val state: Flow<BackgroundSyncState>
}

sealed interface BackgroundSyncState {
    /** Nothing queued. A sync that stopped on an error the agent must see also ends here: "Sincronizar ahora" shows it. */
    data object Idle : BackgroundSyncState

    /** Queued until the device has internet. */
    data object WaitingForNetwork : BackgroundSyncState

    data object Running : BackgroundSyncState

    /** An attempt failed (no connection, 15 s timeout, server error) and the records stay pending; another is scheduled. */
    data class RetryScheduled(val attempts: Int) : BackgroundSyncState
}

/**
 * Network failures, the 15 s timeout of US-11 scenario 2 and server-side errors may pass on their own, so the sync is
 * retried while the records stay pending. A rejected request or a missing session cannot fix itself.
 */
fun isRetryableSyncFailure(error: Throwable): Boolean = when (error) {
    is ApiOperationException -> error.statusCode == 408 || error.statusCode == 429 || error.statusCode >= 500
    is IOException -> true
    else -> false
}
