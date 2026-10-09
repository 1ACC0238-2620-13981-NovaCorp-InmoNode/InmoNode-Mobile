package com.novacorp.inmonode_app.features.iam.presentation.login

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordHidden: Boolean = true,
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: LoginError? = null,
    /** M03: seconds until the account can try again, or null when not locked. */
    val lockedSecondsRemaining: Long? = null
) {
    val isLocked: Boolean get() = lockedSecondsRemaining != null

    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotEmpty() && !isLoading && !isLocked
}

sealed interface LoginError {
    data object InvalidCredentials : LoginError
    data object AccountInactive : LoginError
    data object NotFieldAgent : LoginError
    data object TooManyRequests : LoginError
    data object Network : LoginError
    data class Unknown(val message: String?) : LoginError
}
