package com.novacorp.inmonode_app.features.iam.presentation.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novacorp.inmonode_app.features.iam.application.SignInUseCase
import com.novacorp.inmonode_app.features.iam.domain.AuthError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var lockCountdownJob: Job? = null

    init {
        savedStateHandle.get<Long>(KEY_LOCKED_UNTIL)?.let(::startLockCountdown)
    }

    fun onEmailChanged(email: String) {
        _uiState.update { currentState -> currentState.copy(email = email, error = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { currentState -> currentState.copy(password = password, error = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { currentState -> currentState.copy(isPasswordHidden = !currentState.isPasswordHidden) }
    }

    fun signIn() {
        val state = _uiState.value
        if (!state.canSubmit) return

        viewModelScope.launch {
            _uiState.update { currentState -> currentState.copy(isLoading = true, error = null) }
            signInUseCase(state.email, state.password)
                .onSuccess {
                    _uiState.update { currentState ->
                        currentState.copy(isLoading = false, isAuthenticated = true)
                    }
                }
                .onFailure { exception ->
                    _uiState.update { currentState -> currentState.copy(isLoading = false) }
                    if (exception is AuthError.AccountLocked) {
                        // The API does not say when the lock ends; 15 minutes is the upper bound.
                        startLockCountdown(
                            System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(AuthError.LOCK_MINUTES)
                        )
                    } else {
                        _uiState.update { currentState -> currentState.copy(error = exception.toLoginError()) }
                    }
                }
        }
    }

    private fun startLockCountdown(lockedUntil: Long) {
        savedStateHandle[KEY_LOCKED_UNTIL] = lockedUntil
        lockCountdownJob?.cancel()
        lockCountdownJob = viewModelScope.launch {
            while (true) {
                val remainingMillis = lockedUntil - System.currentTimeMillis()
                if (remainingMillis <= 0) break
                _uiState.update { currentState ->
                    currentState.copy(lockedSecondsRemaining = (remainingMillis + 999) / 1000)
                }
                delay(1_000)
            }
            savedStateHandle.remove<Long>(KEY_LOCKED_UNTIL)
            _uiState.update { currentState -> currentState.copy(lockedSecondsRemaining = null) }
        }
    }

    private fun Throwable.toLoginError(): LoginError = when (this) {
        is AuthError.InvalidCredentials -> LoginError.InvalidCredentials
        is AuthError.AccountInactive -> LoginError.AccountInactive
        is AuthError.NotFieldAgent -> LoginError.NotFieldAgent
        is AuthError.TooManyRequests -> LoginError.TooManyRequests
        is AuthError.Network -> LoginError.Network
        else -> LoginError.Unknown(message)
    }

    private companion object {
        const val KEY_LOCKED_UNTIL = "locked_until"
    }
}
