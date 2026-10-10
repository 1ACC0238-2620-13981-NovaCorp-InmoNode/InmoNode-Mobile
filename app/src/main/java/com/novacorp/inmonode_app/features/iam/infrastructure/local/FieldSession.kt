package com.novacorp.inmonode_app.features.iam.infrastructure.local

import com.novacorp.inmonode_app.features.iam.domain.AuthRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** A device can be used by several agents. Never display or submit another agent's local records. */
@Singleton
class FieldSession @Inject constructor(repository: AuthRepository) {
    val ownerIds = repository.currentUser.map { it?.takeIf { user -> user.isFieldAgent }?.id }.distinctUntilChanged()
    suspend fun requireOwner(): Long = ownerIds.first() ?: error("Inicia sesión como agente de campo.")
    suspend fun requireUnchanged(ownerId: Long) {
        check(requireOwner() == ownerId) { "La sesión cambió. Vuelve a intentar la operación." }
    }
}
