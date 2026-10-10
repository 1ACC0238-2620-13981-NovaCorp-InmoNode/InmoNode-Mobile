package com.novacorp.inmonode_app.features.fieldsales.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {
    @Query("SELECT * FROM projects WHERE ownerId = :ownerId ORDER BY name")
    fun observeProjects(ownerId: Long): Flow<List<ProjectEntity>>
    @Query("SELECT * FROM projects WHERE ownerId = :ownerId AND id = :id")
    suspend fun find(ownerId: Long, id: Long): ProjectEntity?
    @Query("DELETE FROM projects WHERE ownerId = :ownerId")
    suspend fun deleteProjects(ownerId: Long)
    @Upsert suspend fun upsert(projects: List<ProjectEntity>)
    @Query("SELECT * FROM sync_metadata WHERE ownerId = :ownerId")
    fun observeMetadata(ownerId: Long): Flow<SyncMetadataEntity?>
    @Query("SELECT * FROM sync_metadata WHERE ownerId = :ownerId")
    suspend fun metadata(ownerId: Long): SyncMetadataEntity?
    @Upsert suspend fun upsert(metadata: SyncMetadataEntity)
}
