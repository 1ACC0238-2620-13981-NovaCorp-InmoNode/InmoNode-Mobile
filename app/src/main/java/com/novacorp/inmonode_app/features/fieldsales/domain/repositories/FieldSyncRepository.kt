package com.novacorp.inmonode_app.features.fieldsales.domain.repositories

import com.novacorp.inmonode_app.features.fieldsales.domain.model.SyncSummary

interface FieldSyncRepository {
    suspend fun synchronize(): Result<SyncSummary>
}
