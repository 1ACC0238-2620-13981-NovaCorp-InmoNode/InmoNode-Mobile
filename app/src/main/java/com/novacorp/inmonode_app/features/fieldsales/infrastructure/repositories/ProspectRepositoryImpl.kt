package com.novacorp.inmonode_app.features.fieldsales.infrastructure.repositories

import com.novacorp.inmonode_app.core.database.InmoNodeDatabase
import com.novacorp.inmonode_app.core.database.atomic
import com.novacorp.inmonode_app.core.network.operationResult
import com.novacorp.inmonode_app.features.fieldsales.domain.model.Prospect
import com.novacorp.inmonode_app.features.fieldsales.domain.model.ProspectRules
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ProspectRepository
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.local.ProspectEntity
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.local.toDomain
import com.novacorp.inmonode_app.features.iam.infrastructure.local.FieldSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import com.novacorp.inmonode_app.core.time.utcTimestamp
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class ProspectRepositoryImpl @Inject constructor(private val database: InmoNodeDatabase,
    private val session: FieldSession) : ProspectRepository {
    private val dao = database.prospectDao()
    override fun observeProspects(): Flow<List<Prospect>> = session.ownerIds.flatMapLatest { owner ->
        if (owner == null) flowOf(emptyList()) else dao.observeProspects(owner).map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun completeMaritalStatus(prospectId: String, maritalStatus: String): Result<Prospect> = operationResult {
        val owner = session.requireOwner()
        require(maritalStatus in listOf("Soltero(a)", "Casado(a)", "Divorciado(a)", "Viudo(a)")) { "Selecciona un estado civil válido." }
        // Contract-only information: the backend prospect contract has no marital-status field.
        database.atomic {
            val row = requireNotNull(dao.find(owner, prospectId)) { "No se encontr? el prospecto." }
            row.copy(maritalStatus = maritalStatus).also { dao.upsert(it) }.toDomain()
        }
    }

    override suspend fun register(document: String, fullName: String, phone: String, maritalStatus: String?): Result<Prospect> = operationResult {
        val owner = session.requireOwner()
        listOf(ProspectRules.documentError(document), ProspectRules.nameError(fullName), ProspectRules.phoneError(phone))
            .firstNotNullOfOrNull { it }?.let { throw IllegalArgumentException(it) }
        val prospect = ProspectEntity(owner, UUID.randomUUID().toString(), document.trim().uppercase(),
            fullName.trim(), phone.trim(), maritalStatus?.trim()?.takeIf { it.isNotEmpty() }, utcTimestamp())
        database.atomic {
            dao.upsert(prospect)
        }
        prospect.toDomain()
    }
}
