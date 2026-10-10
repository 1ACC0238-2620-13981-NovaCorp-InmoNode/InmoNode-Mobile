package com.novacorp.inmonode_app.features.fieldsales.domain.repositories

import com.novacorp.inmonode_app.features.fieldsales.domain.model.Prospect
import kotlinx.coroutines.flow.Flow

interface ProspectRepository {
    fun observeProspects(): Flow<List<Prospect>>
    suspend fun completeMaritalStatus(prospectId: String, maritalStatus: String): Result<Prospect>
    suspend fun register(document: String, fullName: String, phone: String, maritalStatus: String?): Result<Prospect>
}
