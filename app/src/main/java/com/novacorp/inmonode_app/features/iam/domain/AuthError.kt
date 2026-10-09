package com.novacorp.inmonode_app.features.iam.domain

/** Reasons a sign-in can fail, mapped from the backend error codes. */
sealed class AuthError(message: String? = null) : Exception(message) {
    /** 401 INVALID_CREDENTIALS. */
    class InvalidCredentials : AuthError()

    /** 423 ACCOUNT_LOCKED: 5 failed attempts lock the account for [LOCK_MINUTES] minutes. */
    class AccountLocked : AuthError()

    /** 403 ACCOUNT_INACTIVE: the email has not been verified. */
    class AccountInactive : AuthError()

    /** Valid credentials, but the account is not a FIELD_AGENT. */
    class NotFieldAgent : AuthError()

    /** 429 RATE_LIMIT_EXCEEDED. */
    class TooManyRequests : AuthError()

    /** No connection or the server did not answer in time. */
    class Network : AuthError()

    class Unknown(message: String?) : AuthError(message)

    companion object {
        const val LOCK_MINUTES = 15L
    }
}
