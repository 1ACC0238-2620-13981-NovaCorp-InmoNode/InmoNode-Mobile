package com.novacorp.inmonode_app.features.iam.infrastructure.repositories

import com.novacorp.inmonode_app.core.di.ApplicationScope
import com.novacorp.inmonode_app.core.network.errorResource
import com.novacorp.inmonode_app.features.iam.domain.AuthError
import com.novacorp.inmonode_app.features.iam.domain.AuthRepository
import com.novacorp.inmonode_app.features.iam.domain.User
import com.novacorp.inmonode_app.features.iam.infrastructure.local.JwtDecoder
import com.novacorp.inmonode_app.features.iam.infrastructure.local.TokenManager
import com.novacorp.inmonode_app.features.iam.infrastructure.remote.AuthService
import com.novacorp.inmonode_app.features.iam.infrastructure.remote.RefreshTokenRequestDto
import com.novacorp.inmonode_app.features.iam.infrastructure.remote.SignInRequestDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val service: AuthService,
    private val tokenManager: TokenManager,
    private val jwtDecoder: JwtDecoder,
    @ApplicationScope private val applicationScope: CoroutineScope
) : AuthRepository {

    override val currentUser: Flow<User?> = tokenManager.accessToken
        .map { token -> token?.let(jwtDecoder::decodeUser) }
        .distinctUntilChanged()

    override suspend fun signIn(email: String, password: String): Result<User> {
        try {
            val response = service.signIn(SignInRequestDto(email, password))
            val tokens = response.body()
            if (!response.isSuccessful || tokens == null) {
                return Result.failure(response.toAuthError())
            }

            val user = jwtDecoder.decodeUser(tokens.token)
                ?: return Result.failure(AuthError.Unknown("Invalid access token"))
            if (!user.isFieldAgent) {
                revokeInBackground(tokens.refreshToken)
                return Result.failure(AuthError.NotFieldAgent())
            }

            tokenManager.saveTokens(tokens.token, tokens.refreshToken)
            return Result.success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            return Result.failure(AuthError.Network())
        } catch (e: Exception) {
            return Result.failure(AuthError.Unknown(e.message))
        }
    }

    override suspend fun signOut() {
        val refreshToken = tokenManager.getRefreshToken()
        tokenManager.clearTokens()
        refreshToken?.let(::revokeInBackground)
    }

    /** Best effort: if offline, the refresh token simply expires on the server. */
    private fun revokeInBackground(refreshToken: String) {
        applicationScope.launch {
            try {
                service.signOut(RefreshTokenRequestDto(refreshToken))
            } catch (e: IOException) {
                // Ignored, see above
            }
        }
    }

    private fun Response<*>.toAuthError(): AuthError {
        val error = errorResource()
        return when (error?.code) {
            "INVALID_CREDENTIALS" -> AuthError.InvalidCredentials()
            "ACCOUNT_LOCKED" -> AuthError.AccountLocked()
            "ACCOUNT_INACTIVE" -> AuthError.AccountInactive()
            "RATE_LIMIT_EXCEEDED" -> AuthError.TooManyRequests()
            else -> when (code()) {
                401 -> AuthError.InvalidCredentials()
                423 -> AuthError.AccountLocked()
                429 -> AuthError.TooManyRequests()
                else -> AuthError.Unknown(error?.message ?: message())
            }
        }
    }
}
