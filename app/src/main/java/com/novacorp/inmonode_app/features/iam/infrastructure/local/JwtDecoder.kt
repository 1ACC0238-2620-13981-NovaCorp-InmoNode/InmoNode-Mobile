package com.novacorp.inmonode_app.features.iam.infrastructure.local

import okio.ByteString.Companion.decodeBase64
import com.google.gson.Gson
import com.novacorp.inmonode_app.features.iam.domain.SignInInput
import com.novacorp.inmonode_app.features.iam.domain.User
import com.novacorp.inmonode_app.features.iam.domain.UserRole
import com.novacorp.inmonode_app.features.iam.infrastructure.remote.TokenResponseDto
import javax.inject.Inject

/**
 * Reads the user from the access token payload ({"sub": userId, "email", "role"}).
 * The backend has no /me endpoint. The signature is not verified here: the backend does that
 * on every request, and the app only needs the claims, also while offline.
 */
class JwtDecoder @Inject constructor() {

    private val gson = Gson()

    fun decodeSession(tokens: TokenResponseDto): User? {
        if (tokens.tokenType != "Bearer" || tokens.expiresIn <= 0 || tokens.refreshToken.isBlank()) return null
        return decodeUser(tokens.token)
    }

    fun decodeUser(token: String): User? = decodeClaims(token, requireUnexpired = true)

    /** Comparison only, never authenticates an expired token or verifies its signature. */
    fun hasSameIdentity(first: String, second: String): Boolean {
        val identity = decodeClaims(first, requireUnexpired = false) ?: return false
        return identity == decodeClaims(second, requireUnexpired = false)
    }

    private fun decodeClaims(token: String, requireUnexpired: Boolean): User? = try {
        val parts = token.split(".")
        require(parts.size == 3 && parts.all { it.isNotBlank() })
        val payload = parts[1]
        val json = requireNotNull(payload.decodeBase64()).utf8()
        val claims = gson.fromJson(json, JwtClaimsDto::class.java)
        val id = claims.sub?.toLongOrNull()
        val email = claims.email
        if (id == null || id <= 0 || email == null || !SignInInput.isValidEmail(email) ||
            (requireUnexpired && claims.exp <= System.currentTimeMillis() / 1000)) {
            null
        } else {
            User(
                id = id,
                email = email,
                role = UserRole.entries.firstOrNull { role -> role.name == claims.role } ?: UserRole.UNKNOWN
            )
        }
    } catch (e: Exception) {
        null
    }

    private data class JwtClaimsDto(
        val sub: String?,
        val email: String?,
        val role: String?,
        val exp: Long
    )
}
