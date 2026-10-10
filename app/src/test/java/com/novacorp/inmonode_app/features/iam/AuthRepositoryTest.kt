package com.novacorp.inmonode_app.features.iam

import com.novacorp.inmonode_app.features.iam.infrastructure.local.*
import com.novacorp.inmonode_app.features.iam.infrastructure.repositories.AuthRepositoryImpl
import com.novacorp.inmonode_app.features.iam.infrastructure.remote.TokenResponseDto
import retrofit2.Response
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.*
import org.junit.Test

class AuthRepositoryTest {
    @Test fun logoutInFlightCannotClearNewLogin() = runTest {
        val delegate = MemorySessionStore()
        val entered = kotlinx.coroutines.CompletableDeferred<Unit>()
        val release = kotlinx.coroutines.CompletableDeferred<Unit>()
        var pauseNextEdit = false
        val store = object : androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
            override val data = delegate.data
            override suspend fun updateData(transform: suspend (androidx.datastore.preferences.core.Preferences) -> androidx.datastore.preferences.core.Preferences): androidx.datastore.preferences.core.Preferences {
                if (pauseNextEdit) {
                    pauseNextEdit = false
                    entered.complete(Unit)
                    release.await()
                }
                return delegate.updateData(transform)
            }
        }
        val manager = TokenManager(store)
        manager.saveTokens(fixtureToken(), "synthetic-old-refresh")
        val repository = AuthRepositoryImpl(FixtureAuthService(), manager, JwtDecoder(), backgroundScope)
        pauseNextEdit = true
        val logout = launch { repository.signOut() }
        entered.await()
        val newToken = fixtureToken(id = "2")
        manager.saveTokens(newToken, "synthetic-new-refresh")
        release.complete(Unit)
        logout.join()
        assertEquals(newToken, manager.getAccessToken())
        assertEquals("synthetic-new-refresh", manager.getRefreshToken())
    }

    @Test fun logoutRevokeFailureDoesNotEscapeOrRestoreSession() = runTest {
        val failures = mutableListOf<Throwable>()
        val scope = kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.test.StandardTestDispatcher(testScheduler) +
                kotlinx.coroutines.CoroutineExceptionHandler { _, error -> failures.add(error) }
        )
        val manager = TokenManager(MemorySessionStore())
        manager.saveTokens(fixtureToken(), "synthetic-refresh")
        val service = FixtureAuthService().apply { logoutAction = { throw IllegalStateException("synthetic failure") } }
        AuthRepositoryImpl(service, manager, JwtDecoder(), scope).signOut()
        runCurrent()
        assertNull(manager.getAccessToken())
        assertNull(manager.getRefreshToken())
        assertTrue(failures.isEmpty())
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }
    @Test fun unknownServerDetailsNeverEscapeRepository() = runTest {
        val service = FixtureAuthService().apply {
            loginResponse = Response.error(500,
                okhttp3.ResponseBody.Companion.run { "{\"code\":\"INTERNAL\",\"message\":\"synthetic-sensitive-detail\"}".toResponseBody() })
        }
        val repository = AuthRepositoryImpl(service, TokenManager(MemorySessionStore()), JwtDecoder(), backgroundScope)
        val error = repository.signIn("agent@example.test", "fixture").exceptionOrNull()
        assertNotNull(error)
        assertNull(error?.message)
    }
    @Test fun invalidTokenEnvelopeNeverPersists() = runTest {
        val manager = TokenManager(MemorySessionStore())
        val service = FixtureAuthService().apply {
            loginResponse = Response.success(TokenResponseDto(fixtureToken(), "Basic", 3600, "synthetic-refresh"))
        }
        val repository = AuthRepositoryImpl(service, manager, JwtDecoder(), backgroundScope)
        assertTrue(repository.signIn("agent@example.test", "fixture").isFailure)
        assertNull(manager.getAccessToken())
        assertNull(manager.getRefreshToken())
    }
    @Test fun restoredBuyerIsNotAuthenticated() = runTest {
        val manager = TokenManager(MemorySessionStore())
        manager.saveTokens(fixtureToken(role = "BUYER"), "synthetic-refresh")
        val repository = AuthRepositoryImpl(FixtureAuthService(), manager, JwtDecoder(), backgroundScope)
        assertNull(repository.currentUser.first())
    }
}
