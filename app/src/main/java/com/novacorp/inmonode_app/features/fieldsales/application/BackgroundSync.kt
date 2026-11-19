package com.novacorp.inmonode_app.features.fieldsales.application

import kotlinx.coroutines.flow.Flow

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
