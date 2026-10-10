package com.novacorp.inmonode_app.features.iam

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.novacorp.inmonode_app.features.iam.infrastructure.remote.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response

class MemorySessionStore : DataStore<Preferences> {
    override val data = MutableStateFlow(emptyPreferences())
    private val mutex = Mutex()
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = mutex.withLock {
        transform(data.value).also { data.value = it }
    }
}

class FixtureAuthService : AuthService {
    var loginResponse = Response.success(TokenResponseDto(fixtureToken(), "Bearer", 3600, "synthetic-refresh"))
    var refreshAction: suspend () -> Response<TokenResponseDto> = { loginResponse }
    val refreshRequests = mutableListOf<RefreshTokenRequestDto>()
    var logoutCalls = 0
    var logoutAction: suspend () -> Response<Unit> = { Response.success<Unit>(204, null) }
    override suspend fun signIn(request: SignInRequestDto) = loginResponse
    override suspend fun refresh(request: RefreshTokenRequestDto): Response<TokenResponseDto> {
        refreshRequests.add(request)
        return refreshAction()
    }
    override suspend fun signOut(request: RefreshTokenRequestDto): Response<Unit> {
        logoutCalls++
        return logoutAction()
    }
}
