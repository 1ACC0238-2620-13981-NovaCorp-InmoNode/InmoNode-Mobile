package com.novacorp.inmonode_app.features.iam


import androidx.lifecycle.viewModelScope
import com.novacorp.inmonode_app.features.iam.application.SignInUseCase
import com.novacorp.inmonode_app.features.iam.presentation.login.LoginViewModel
import com.novacorp.inmonode_app.features.iam.domain.AuthError
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @Test fun loadingCannotRevealPassword() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = RecordingAuthRepository().apply { action = { awaitCancellation() } }
            val vm = LoginViewModel(SignInUseCase(repository))
            vm.onEmailChanged("agent@example.test")
            vm.onPasswordChanged("synthetic-password")
            vm.signIn()
            runCurrent()
            vm.togglePasswordVisibility()
            assertTrue(vm.uiState.value.isPasswordHidden)
            vm.viewModelScope.cancel()
        } finally { Dispatchers.resetMain() }
    }

    @Test fun retryAfterPreventsSubmitUntilServerDelayPasses() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val raw = okhttp3.Response.Builder()
                .request(okhttp3.Request.Builder().url("https://example.test/auth/login").build())
                .protocol(okhttp3.Protocol.HTTP_1_1).code(429).message("Rate limited")
                .header("Retry-After", "2").build()
            val service = FixtureAuthService().apply {
                loginResponse = retrofit2.Response.error("{}".let { okhttp3.ResponseBody.Companion.run { it.toResponseBody() } }, raw)
            }
            val repository = com.novacorp.inmonode_app.features.iam.infrastructure.repositories.AuthRepositoryImpl(
                service, com.novacorp.inmonode_app.features.iam.infrastructure.local.TokenManager(MemorySessionStore()),
                com.novacorp.inmonode_app.features.iam.infrastructure.local.JwtDecoder(), backgroundScope
            )
            val vm = LoginViewModel(SignInUseCase(repository))
            vm.onEmailChanged("agent@example.test")
            vm.onPasswordChanged("fixture")
            vm.signIn()
            runCurrent()
            assertFalse(vm.uiState.value.canSubmit)
            vm.onEmailChanged("other@example.test")
            assertFalse(vm.uiState.value.canSubmit)
            advanceTimeBy(2000)
            runCurrent()
            assertTrue(vm.uiState.value.canSubmit)
            vm.viewModelScope.cancel()
        } finally { Dispatchers.resetMain() }
    }
    @Test fun thrownFailureBecomesGenericError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = RecordingAuthRepository().apply { action = { throw IllegalStateException("synthetic failure") } }
            val vm = LoginViewModel(SignInUseCase(repository))
            vm.onEmailChanged("agent@example.test")
            vm.onPasswordChanged("fixture")
            vm.signIn()
            runCurrent()
            assertEquals(com.novacorp.inmonode_app.features.iam.presentation.login.LoginError.Unknown(null), vm.uiState.value.error)
            assertFalse(vm.uiState.value.isLoading)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun unexpectedFailureIsSanitized() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = RecordingAuthRepository().apply {
                action = { Result.failure(IllegalStateException("synthetic-sensitive-detail")) }
            }
            val vm = LoginViewModel(SignInUseCase(repository))
            vm.onEmailChanged("agent@example.test")
            vm.onPasswordChanged("fixture")
            vm.signIn()
            runCurrent()
            assertEquals(com.novacorp.inmonode_app.features.iam.presentation.login.LoginError.Unknown(null), vm.uiState.value.error)
            assertFalse(vm.uiState.value.isLoading)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun accountLockDoesNotInventRemainingSeconds() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = RecordingAuthRepository().apply { action = { Result.failure(AuthError.AccountLocked()) } }
            val vm = LoginViewModel(SignInUseCase(repository))
            vm.onEmailChanged("agent@example.test")
            vm.onPasswordChanged("fixture")
            vm.signIn()
            runCurrent()
            vm.viewModelScope.cancel()
            assertEquals(0L, vm.uiState.value.retryAfterSecondsRemaining)
            assertNotNull(vm.uiState.value.error)
            vm.onEmailChanged("other@example.test")
            assertNull(vm.uiState.value.error)
            assertTrue(vm.uiState.value.canSubmit)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun successfulRequestRemovesPasswordFromState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val vm = LoginViewModel(SignInUseCase(RecordingAuthRepository()))
            vm.onEmailChanged("agent@example.test")
            vm.onPasswordChanged("fixture")
            vm.signIn()
            runCurrent()
            assertTrue(vm.uiState.value.isAuthenticated)
            assertEquals("", vm.uiState.value.password)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun cancelledRequestReleasesLoading() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = RecordingAuthRepository().apply {
                action = { throw CancellationException("synthetic cancellation") }
            }
            val vm = LoginViewModel(SignInUseCase(repository))
            vm.onEmailChanged("agent@example.test")
            vm.onPasswordChanged("fixture")
            vm.signIn()
            runCurrent()
            assertFalse(vm.uiState.value.isLoading)
            assertFalse(vm.uiState.value.isAuthenticated)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun submitAcquiresLoadingBeforeCoroutineStarts() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = RecordingAuthRepository()
            val vm = LoginViewModel(SignInUseCase(repository))
            vm.onEmailChanged("agent@example.test")
            vm.onPasswordChanged("fixture")
            vm.signIn()
            vm.signIn()
            assertTrue(vm.uiState.value.isLoading)
            runCurrent()
            assertEquals(1, repository.calls)
        } finally { Dispatchers.resetMain() }
    }
}
