package com.novacorp.inmonode_app.features.iam

import androidx.lifecycle.viewModelScope
import com.novacorp.inmonode_app.features.iam.application.SignInUseCase
import com.novacorp.inmonode_app.features.iam.domain.AuthError
import com.novacorp.inmonode_app.features.iam.infrastructure.local.JwtDecoder
import com.novacorp.inmonode_app.features.iam.infrastructure.local.TokenManager
import com.novacorp.inmonode_app.features.iam.infrastructure.remote.AuthService
import com.novacorp.inmonode_app.features.iam.infrastructure.remote.SignInRequestDto
import com.novacorp.inmonode_app.features.iam.infrastructure.remote.TokenResponseDto
import com.novacorp.inmonode_app.features.iam.infrastructure.repositories.AuthRepositoryImpl
import com.novacorp.inmonode_app.features.iam.presentation.login.LoginError
import com.novacorp.inmonode_app.features.iam.presentation.login.LoginViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import retrofit2.Response

/** Regression matrix for the verified local contract, never a live login. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class LoginErrorMatrixTest(
    private val name: String,
    private val status: Int,
    private val code: String,
    private val domainType: Class<out AuthError>,
    private val uiError: LoginError
) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}")
        fun rows(): List<Array<Any>> = listOf(
            arrayOf("credentials", 401, "INVALID_CREDENTIALS", AuthError.InvalidCredentials::class.java, LoginError.InvalidCredentials),
            arrayOf("inactive", 403, "ACCOUNT_INACTIVE", AuthError.AccountInactive::class.java, LoginError.AccountInactive),
            arrayOf("locked", 423, "ACCOUNT_LOCKED", AuthError.AccountLocked::class.java, LoginError.AccountLocked),
            arrayOf("rate limited", 429, "RATE_LIMIT_EXCEEDED", AuthError.TooManyRequests::class.java, LoginError.TooManyRequests),
            arrayOf("validation", 400, "VALIDATION_ERROR", AuthError.Unknown::class.java, LoginError.Unknown(null)),
            arrayOf("server", 500, "INTERNAL", AuthError.Unknown::class.java, LoginError.Unknown(null)),
            arrayOf("network", 0, "", AuthError.Network::class.java, LoginError.Network)
        )
    }

    @Test fun repositoryAndViewModelPreserveSafeErrorAndDoNotPersist() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val fixture = FixtureAuthService()
            val service = object : AuthService by fixture {
                override suspend fun signIn(request: SignInRequestDto): Response<TokenResponseDto> {
                    if (status == 0) throw java.io.IOException("synthetic network failure")
                    return Response.error(status, "{\"code\":\"$code\",\"message\":\"synthetic-sensitive-detail\"}".toResponseBody())
                }
            }
            val manager = TokenManager(MemorySessionStore())
            val repository = AuthRepositoryImpl(service, manager, JwtDecoder(), backgroundScope)
            val error = repository.signIn("agent@example.test", "synthetic-password").exceptionOrNull()
            assertTrue(name, domainType.isInstance(error))
            assertNull(error?.message)
            val vm = LoginViewModel(SignInUseCase(repository))
            vm.onEmailChanged("agent@example.test")
            vm.onPasswordChanged("synthetic-password")
            vm.signIn()
            runCurrent()
            assertEquals(uiError, vm.uiState.value.error)
            assertFalse(vm.uiState.value.isLoading)
            assertFalse(vm.uiState.value.isAuthenticated)
            assertNull(manager.getAccessToken())
            assertNull(manager.getRefreshToken())
            vm.viewModelScope.cancel()
        } finally { Dispatchers.resetMain() }
    }
}
