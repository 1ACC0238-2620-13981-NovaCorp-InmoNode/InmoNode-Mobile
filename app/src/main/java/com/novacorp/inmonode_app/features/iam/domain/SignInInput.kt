package com.novacorp.inmonode_app.features.iam.domain

/** Login validation only: never applies registration password rules. */
object SignInInput {
    fun isValidEmail(email: String): Boolean =
        Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim())

    fun isValid(email: String, password: String): Boolean =
        isValidEmail(email) && password.isNotBlank()
}
