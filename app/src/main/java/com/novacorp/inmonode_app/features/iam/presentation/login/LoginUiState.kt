package com.novacorp.inmonode_app.features.iam.presentation.login

import com.novacorp.inmonode_app.features.iam.domain.SignInInput

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordHidden: Boolean = true,
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: LoginError? = null,
    val retryAfterSecondsRemaining: Long = 0
) {
    val canSubmit: Boolean get() = SignInInput.isValid(email, password) && !isLoading && !isAuthenticated && retryAfterSecondsRemaining == 0L
}

sealed interface LoginError {
    data object InvalidCredentials : LoginError
    data object AccountLocked : LoginError
    data object AccountInactive : LoginError
    data object NotFieldAgent : LoginError
    data object TooManyRequests : LoginError
    data object Network : LoginError
    data class Unknown(val message: String?) : LoginError
}
