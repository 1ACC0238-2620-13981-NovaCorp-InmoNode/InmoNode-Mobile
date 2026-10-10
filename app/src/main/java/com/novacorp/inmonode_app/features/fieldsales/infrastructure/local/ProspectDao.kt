package com.novacorp.inmonode_app.features.fieldsales.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProspectDao {
    @Query("SELECT * FROM prospects WHERE ownerId = :ownerId ORDER BY registeredAt DESC")
    fun observeProspects(ownerId: Long): Flow<List<ProspectEntity>>
    @Query("SELECT * FROM prospects WHERE ownerId = :ownerId AND synced = 0 ORDER BY registeredAt, id LIMIT :limit")
    suspend fun pending(ownerId: Long, limit: Int): List<ProspectEntity>
    @Query("SELECT * FROM prospects WHERE ownerId = :ownerId AND id = :id")
    suspend fun find(ownerId: Long, id: String): ProspectEntity?
    @Upsert suspend fun upsert(prospect: ProspectEntity)
    @Query("UPDATE prospects SET synced = 1 WHERE ownerId = :ownerId AND id IN (:ids)")
    suspend fun markSynced(ownerId: Long, ids: List<String>)
}
