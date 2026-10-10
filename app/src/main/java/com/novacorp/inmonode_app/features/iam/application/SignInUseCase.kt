package com.novacorp.inmonode_app.features.iam.application

import com.novacorp.inmonode_app.features.iam.domain.AuthRepository
import com.novacorp.inmonode_app.features.iam.domain.User
import com.novacorp.inmonode_app.features.iam.domain.AuthError
import com.novacorp.inmonode_app.features.iam.domain.SignInInput
import javax.inject.Inject

class SignInUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<User> =
        if (SignInInput.isValid(email, password)) repository.signIn(email.trim().lowercase(), password)
        else Result.failure(AuthError.InvalidCredentials())
}
