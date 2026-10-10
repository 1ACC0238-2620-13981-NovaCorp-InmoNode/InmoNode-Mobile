package com.novacorp.inmonode_app.features.iam.application

import com.novacorp.inmonode_app.features.iam.domain.AuthRepository
import javax.inject.Inject

class SignOutUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.signOut()
}
