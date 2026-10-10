package com.novacorp.inmonode_app.features.iam.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novacorp.inmonode_app.features.iam.application.SignInUseCase
import com.novacorp.inmonode_app.features.iam.domain.AuthError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { currentState -> currentState.copy(email = email, error = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { currentState -> currentState.copy(password = password, error = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { currentState ->
            if (currentState.isLoading) currentState
            else currentState.copy(isPasswordHidden = !currentState.isPasswordHidden)
        }
    }

    fun signIn() {
        val state = _uiState.value
        if (!state.canSubmit) return
        if (!_uiState.compareAndSet(state, state.copy(isLoading = true, error = null))) return

        viewModelScope.launch {
            try {
                signInUseCase(state.email, state.password)
                    .onSuccess {
                        _uiState.update { currentState ->
                            currentState.copy(isLoading = false, isAuthenticated = true, password = "", isPasswordHidden = true)
                        }
                    }
                    .onFailure { exception ->
                        val retryAfter = (exception as? AuthError.TooManyRequests)?.retryAfterSeconds ?: 0
                        _uiState.update { it.copy(error = exception.toLoginError(), retryAfterSecondsRemaining = retryAfter) }
                        if (retryAfter > 0) viewModelScope.launch {
                            while (_uiState.value.retryAfterSecondsRemaining > 0) {
                                delay(1000)
                                _uiState.update { it.copy(retryAfterSecondsRemaining = (it.retryAfterSecondsRemaining - 1).coerceAtLeast(0)) }
                            }
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = LoginError.Unknown(null)) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun Throwable.toLoginError(): LoginError = when (this) {
        is AuthError.InvalidCredentials -> LoginError.InvalidCredentials
        is AuthError.AccountLocked -> LoginError.AccountLocked
        is AuthError.AccountInactive -> LoginError.AccountInactive
        is AuthError.NotFieldAgent -> LoginError.NotFieldAgent
        is AuthError.TooManyRequests -> LoginError.TooManyRequests
        is AuthError.Network -> LoginError.Network
        else -> LoginError.Unknown(null)
    }
}
