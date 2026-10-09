package com.novacorp.inmonode_app.features.iam.infrastructure.local

import android.util.Base64
import com.google.gson.Gson
import com.novacorp.inmonode_app.features.iam.domain.User
import com.novacorp.inmonode_app.features.iam.domain.UserRole
import javax.inject.Inject

/**
 * Reads the user from the access token payload ({"sub": userId, "email", "role"}).
 * The backend has no /me endpoint. The signature is not verified here: the backend does that
 * on every request, and the app only needs the claims, also while offline.
 */
class JwtDecoder @Inject constructor() {

    private val gson = Gson()

    fun decodeUser(token: String): User? = try {
        val payload = token.split(".")[1]
        val json = String(Base64.decode(payload, Base64.URL_SAFE), Charsets.UTF_8)
        val claims = gson.fromJson(json, JwtClaimsDto::class.java)
        val id = claims.sub?.toLongOrNull()
        val email = claims.email
        if (id == null || email == null) {
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
        val role: String?
    )
}
