package com.novacorp.inmonode_app.core.network

import com.novacorp.inmonode_app.features.iam.infrastructure.local.TokenManager
import com.novacorp.inmonode_app.features.iam.infrastructure.remote.AuthService
import com.novacorp.inmonode_app.features.iam.infrastructure.remote.RefreshTokenRequestDto
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On a 401 from a protected endpoint, rotates the tokens with POST auth/refresh and retries once.
 *
 * Refresh tokens are single-use and reusing one revokes the whole session, so refreshes are
 * serialized and a request that failed with an already-replaced token is simply retried.
 * AuthService is injected lazily because it is built on top of the OkHttpClient that uses this class.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authService: Lazy<AuthService>
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request
        if (!request.isApiRequest() || request.isAuthRequest()) return null
        if (response.priorResponse != null) return null

        val failedToken = request.header("Authorization")?.removePrefix("Bearer ")

        synchronized(lock) {
            return runBlocking {
                val currentToken = tokenManager.getAccessToken()
                if (currentToken != null && currentToken != failedToken) {
                    return@runBlocking request.withToken(currentToken)
                }

                val refreshToken = tokenManager.getRefreshToken() ?: return@runBlocking null
                val refreshResponse = try {
                    authService.get().refresh(RefreshTokenRequestDto(refreshToken))
                } catch (e: IOException) {
                    return@runBlocking null
                }

                val tokens = refreshResponse.body()
                if (refreshResponse.isSuccessful && tokens != null) {
                    tokenManager.saveTokens(tokens.token, tokens.refreshToken)
                    request.withToken(tokens.token)
                } else {
                    // Refresh token expired or revoked: the session is over
                    tokenManager.clearTokens()
                    null
                }
            }
        }
    }

    private fun Request.withToken(token: String): Request =
        newBuilder().header("Authorization", "Bearer $token").build()
}
