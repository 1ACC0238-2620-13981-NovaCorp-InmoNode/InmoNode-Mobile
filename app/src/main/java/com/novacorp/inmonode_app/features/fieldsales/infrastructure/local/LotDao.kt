package com.novacorp.inmonode_app.features.fieldsales.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LotDao {
    @Query("SELECT * FROM lots WHERE ownerId = :ownerId ORDER BY projectId, code")
    fun observeLots(ownerId: Long): Flow<List<LotEntity>>
    @Query("SELECT * FROM lots WHERE ownerId = :ownerId AND id = :id")
    suspend fun find(ownerId: Long, id: Long): LotEntity?
    @Query("DELETE FROM lots WHERE ownerId = :ownerId")
    suspend fun deletePortfolioLots(ownerId: Long)
    @Upsert suspend fun upsert(lots: List<LotEntity>)
    @Query("UPDATE lots SET status = :status WHERE ownerId = :ownerId AND id = :id")
    suspend fun setStatus(ownerId: Long, id: Long, status: String)
}
