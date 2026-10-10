package com.novacorp.inmonode_app.features.iam

import com.novacorp.inmonode_app.features.iam.infrastructure.local.JwtDecoder
import java.util.Base64
import org.junit.Assert.*
import org.junit.Test

/** Synthetic unsigned fixtures for parsing tests; not valid server credentials. */
fun fixtureToken(exp: Long = System.currentTimeMillis() / 1000 + 3600, role: String = "FIELD_AGENT", id: String = "1", email: String = "agent@example.test"): String {
    val encoder = Base64.getUrlEncoder().withoutPadding()
    fun encode(value: String) = encoder.encodeToString(value.toByteArray())
    return "${encode("{\"alg\":\"HS256\"}")}.${encode("{\"sub\":\"$id\",\"email\":\"$email\",\"role\":\"$role\",\"exp\":$exp}")}.synthetic-signature"
}

class JwtDecoderTest {
    @Test fun invalidIdentityIsRejected() {
        assertNull(JwtDecoder().decodeUser(fixtureToken(id = "0")))
        assertNull(JwtDecoder().decodeUser(fixtureToken(email = "invalid")))
    }
    @Test fun incompleteTokenIsRejected() {
        assertNull(JwtDecoder().decodeUser(fixtureToken().substringBeforeLast(".")))
    }
    @Test fun expiredTokenIsNotASession() {
        assertNull(JwtDecoder().decodeUser(fixtureToken(exp = 1)))
    }
    @Test fun decodesBackendClaimsWithoutAndroidRuntime() {
        assertEquals(1L, JwtDecoder().decodeUser(fixtureToken())?.id)
    }
}
