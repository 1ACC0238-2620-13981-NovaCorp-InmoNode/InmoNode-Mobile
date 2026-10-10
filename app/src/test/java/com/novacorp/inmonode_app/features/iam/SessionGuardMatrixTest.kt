package com.novacorp.inmonode_app.features.iam

import com.novacorp.inmonode_app.BuildConfig
import com.novacorp.inmonode_app.core.network.TokenAuthenticator
import com.novacorp.inmonode_app.features.iam.infrastructure.local.JwtDecoder
import com.novacorp.inmonode_app.features.iam.infrastructure.local.TokenManager
import com.novacorp.inmonode_app.features.iam.infrastructure.remote.TokenResponseDto
import com.novacorp.inmonode_app.features.iam.infrastructure.repositories.AuthRepositoryImpl
import dagger.Lazy
import kotlinx.coroutines.test.runTest
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Independent guard regression rows; synthetic unsigned JWTs, not server credentials. */
@RunWith(Parameterized::class)
class SessionGuardMatrixTest(private val name: String, private val tokens: TokenResponseDto) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}")
        fun rows(): List<Array<Any>> {
            val valid = TokenResponseDto(fixtureToken(), "Bearer", 3600, "synthetic-refresh")
            return listOf(
                "type" to valid.copy(tokenType = "Basic"),
                "zero ttl" to valid.copy(expiresIn = 0),
                "negative ttl" to valid.copy(expiresIn = -1),
                "blank refresh" to valid.copy(refreshToken = " "),
                "buyer role" to valid.copy(token = fixtureToken(role = "BUYER")),
                "unknown role" to valid.copy(token = fixtureToken(role = "UNKNOWN")),
                "expired" to valid.copy(token = fixtureToken(exp = 1)),
                "zero identity" to valid.copy(token = fixtureToken(id = "0")),
                "invalid email" to valid.copy(token = fixtureToken(email = "invalid")),
                "missing signature" to valid.copy(token = fixtureToken().substringBeforeLast("."))
            ).map { (label, dto) -> arrayOf<Any>(label, dto) }
        }
    }

    @Test fun loginDoesNotPersistInvalidSession() = runTest {
        val manager = TokenManager(MemorySessionStore())
        val service = FixtureAuthService().apply { loginResponse = retrofit2.Response.success(tokens) }
        val repository = AuthRepositoryImpl(service, manager, JwtDecoder(), backgroundScope)
        assertTrue(name, repository.signIn("agent@example.test", "synthetic-password").isFailure)
        assertNull(manager.getAccessToken())
        assertNull(manager.getRefreshToken())
    }

    @Test fun refreshDoesNotReplaceValidSessionWithInvalidEnvelope() = runTest {
        val manager = TokenManager(MemorySessionStore())
        val old = fixtureToken()
        manager.saveTokens(old, "synthetic-old-refresh")
        val service = FixtureAuthService().apply { loginResponse = retrofit2.Response.success(tokens) }
        val failed = Response.Builder().request(Request.Builder()
            .url(BuildConfig.BASE_URL + "field-sync/portfolio").header("Authorization", "Bearer $old").build())
            .protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized").build()
        assertNull(TokenAuthenticator(manager, Lazy { service }).authenticate(null, failed))
        assertEquals(old, manager.getAccessToken())
        assertEquals("synthetic-old-refresh", manager.getRefreshToken())
    }
}
