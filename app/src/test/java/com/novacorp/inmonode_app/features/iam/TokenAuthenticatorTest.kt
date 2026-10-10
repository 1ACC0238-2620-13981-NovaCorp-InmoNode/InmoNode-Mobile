package com.novacorp.inmonode_app.features.iam

import com.novacorp.inmonode_app.BuildConfig
import com.novacorp.inmonode_app.core.network.TokenAuthenticator
import com.novacorp.inmonode_app.features.iam.infrastructure.local.TokenManager
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class TokenAuthenticatorTest {
    @Test fun refreshUsesOneSnapshotWhenLoginChangesAfterRead() = runBlocking {
        val delegate = MemorySessionStore()
        val old = fixtureToken()
        val newToken = fixtureToken(id = "2")
        var replaceAfterRead = true
        val store = object : androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
            override val data = kotlinx.coroutines.flow.flow {
                try {
                    emit(delegate.data.value)
                } finally {
                    if (replaceAfterRead) {
                        replaceAfterRead = false
                        delegate.updateData { preferences ->
                            preferences.toMutablePreferences().apply {
                                this[androidx.datastore.preferences.core.stringPreferencesKey("access_token")] = newToken
                                this[androidx.datastore.preferences.core.stringPreferencesKey("refresh_token")] = "synthetic-new-refresh"
                            }
                        }
                    }
                }
            }
            override suspend fun updateData(transform: suspend (androidx.datastore.preferences.core.Preferences) -> androidx.datastore.preferences.core.Preferences) = delegate.updateData(transform)
        }
        val manager = TokenManager(store)
        manager.saveTokens(old, "synthetic-old-refresh")
        val service = FixtureAuthService()
        assertNull(TokenAuthenticator(manager, Lazy { service }).authenticate(null, unauthorized(old)))
        assertEquals("synthetic-old-refresh", service.refreshRequests.single().refreshToken)
        assertEquals(newToken, manager.getAccessToken())
        assertEquals("synthetic-new-refresh", manager.getRefreshToken())
    }

    @Test fun expiredAccessCanRefreshWithoutChangingIdentity() = runBlocking {
        val manager = TokenManager(MemorySessionStore())
        val expired = fixtureToken(exp = 1)
        manager.saveTokens(expired, "synthetic-old-refresh")
        val service = FixtureAuthService()
        val retry = TokenAuthenticator(manager, Lazy { service }).authenticate(null, unauthorized(expired))
        assertEquals("Bearer ${service.loginResponse.body()!!.token}", retry?.header("Authorization"))
        assertEquals(1, service.refreshRequests.size)
    }

    @Test fun rotationCannotChangeRequestIdentity() = runBlocking {
        val manager = TokenManager(MemorySessionStore())
        val old = fixtureToken(id = "1")
        manager.saveTokens(old, "synthetic-old-refresh")
        val service = FixtureAuthService().apply {
            loginResponse = retrofit2.Response.success(
                com.novacorp.inmonode_app.features.iam.infrastructure.remote.TokenResponseDto(
                    fixtureToken(id = "2"), "Bearer", 3600, "synthetic-rotated-refresh"
                )
            )
        }
        assertNull(TokenAuthenticator(manager, Lazy { service }).authenticate(null, unauthorized(old)))
        assertEquals(old, manager.getAccessToken())
    }

    @Test fun lateUnauthorizedMustNotReplayAsAnotherUser() = runBlocking {
        val manager = TokenManager(MemorySessionStore())
        val old = fixtureToken(id = "1")
        manager.saveTokens(fixtureToken(id = "2"), "synthetic-new-login")
        val service = FixtureAuthService()
        assertNull(TokenAuthenticator(manager, Lazy { service }).authenticate(null, unauthorized(old)))
    }

    @Test fun terminalRejectionClearsMatchingSession() = runBlocking {
        val manager = TokenManager(MemorySessionStore())
        val old = fixtureToken()
        manager.saveTokens(old, "synthetic-old-refresh")
        val service = FixtureAuthService().apply { refreshAction = { retrofit2.Response.error(401, "{}".toResponseBody()) } }
        assertNull(TokenAuthenticator(manager, Lazy { service }).authenticate(null, unauthorized(old)))
        assertNull(manager.getAccessToken())
        assertNull(manager.getRefreshToken())
    }
    @Test fun validRotationCannotOverwriteNewLogin() = runBlocking {
        val manager = TokenManager(MemorySessionStore())
        val old = fixtureToken()
        manager.saveTokens(old, "synthetic-old-refresh")
        val service = FixtureAuthService().apply {
            refreshAction = {
                manager.saveTokens(old, "synthetic-new-login")
                loginResponse
            }
        }
        assertNull(TokenAuthenticator(manager, Lazy { service }).authenticate(null, unauthorized(old)))
        assertEquals("synthetic-new-login", manager.getRefreshToken())
    }
    @Test fun concurrentUnauthorizedRequestsRotateOnce() = runBlocking {
        val manager = TokenManager(MemorySessionStore())
        val old = fixtureToken()
        manager.saveTokens(old, "synthetic-old-refresh")
        val calls = java.util.concurrent.atomic.AtomicInteger()
        val service = FixtureAuthService().apply {
            loginResponse = retrofit2.Response.success(
                com.novacorp.inmonode_app.features.iam.infrastructure.remote.TokenResponseDto(
                    fixtureToken(exp = System.currentTimeMillis() / 1000 + 7200), "Bearer", 3600, "synthetic-rotated-refresh"
                )
            )
            refreshAction = { calls.incrementAndGet(); loginResponse }
        }
        val authenticator = TokenAuthenticator(manager, Lazy { service })
        val executor = java.util.concurrent.Executors.newFixedThreadPool(4)
        try {
            val barrier = java.util.concurrent.CyclicBarrier(4)
            val retries = (1..4).map {
                executor.submit<Request?> { barrier.await(); authenticator.authenticate(null, unauthorized(old)) }
            }.map { it.get(5, java.util.concurrent.TimeUnit.SECONDS) }
            assertEquals(1, calls.get())
            assertTrue(retries.all { it?.header("Authorization") == "Bearer ${service.loginResponse.body()!!.token}" })
        } finally { executor.shutdownNow() }
    }
    @Test fun refreshWithInvalidEnvelopeDoesNotReplaceSession() = runBlocking {
        val manager = TokenManager(MemorySessionStore())
        val old = fixtureToken()
        manager.saveTokens(old, "synthetic-old-refresh")
        val service = FixtureAuthService().apply {
            loginResponse = retrofit2.Response.success(
                com.novacorp.inmonode_app.features.iam.infrastructure.remote.TokenResponseDto(
                    fixtureToken(role = "BUYER"), "Basic", 0, ""
                )
            )
        }
        assertNull(TokenAuthenticator(manager, Lazy { service }).authenticate(null, unauthorized(old)))
        assertEquals("synthetic-old-refresh", manager.getRefreshToken())
    }
    @Test fun rejectedOldRefreshCannotClearNewLogin() = runBlocking {
        val manager = TokenManager(MemorySessionStore())
        val old = fixtureToken()
        manager.saveTokens(old, "synthetic-old-refresh")
        val service = FixtureAuthService().apply {
            refreshAction = {
                manager.saveTokens(old, "synthetic-new-login-refresh")
                retrofit2.Response.error(401, "{}".toResponseBody())
            }
        }
        assertNull(TokenAuthenticator(manager, Lazy { service }).authenticate(null, unauthorized(old)))
        assertEquals("synthetic-new-login-refresh", manager.getRefreshToken())
    }
    @Test fun serverFailureDoesNotDestroySession() = runBlocking {
        val manager = TokenManager(MemorySessionStore())
        val old = fixtureToken()
        manager.saveTokens(old, "synthetic-old-refresh")
        val service = FixtureAuthService().apply {
            refreshAction = { retrofit2.Response.error(500, "{}".toResponseBody()) }
        }
        assertNull(TokenAuthenticator(manager, Lazy { service }).authenticate(null, unauthorized(old)))
        assertEquals(old, manager.getAccessToken())
    }
    private fun unauthorized(token: String) = Response.Builder().request(
        Request.Builder().url(BuildConfig.BASE_URL + "field-sync/portfolio").header("Authorization", "Bearer $token").build()
    ).protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized").build()

    @Test fun refreshInFlightCannotResurrectLogout() = runBlocking {
        val manager = TokenManager(MemorySessionStore())
        val old = fixtureToken()
        manager.saveTokens(old, "synthetic-old-refresh")
        val service = FixtureAuthService().apply {
            refreshAction = { manager.clearTokens(); loginResponse }
        }
        val retry = TokenAuthenticator(manager, Lazy { service }).authenticate(null, unauthorized(old))
        assertNull(retry)
        assertNull(manager.getAccessToken())
        assertNull(manager.getRefreshToken())
    }
}
