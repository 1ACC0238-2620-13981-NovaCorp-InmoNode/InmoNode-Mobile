package com.novacorp.inmonode_app.features.iam.application

import com.novacorp.inmonode_app.features.iam.domain.AuthRepository
import com.novacorp.inmonode_app.features.iam.domain.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke(): Flow<User?> = repository.currentUser
}
