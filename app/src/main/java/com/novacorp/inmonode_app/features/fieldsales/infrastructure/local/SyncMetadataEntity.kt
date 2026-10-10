package com.novacorp.inmonode_app.features.fieldsales.infrastructure.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(@PrimaryKey val ownerId: Long, val etag: String? = null,
    val downloadedAt: String? = null, val lastSyncedAt: String? = null)
