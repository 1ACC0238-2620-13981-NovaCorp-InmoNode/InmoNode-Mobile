package com.novacorp.inmonode_app.core.network

import com.novacorp.inmonode_app.features.iam.infrastructure.local.JwtDecoder
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

/** Retrofit @Tag metadata. It is never sent as an API field or header. */
data class AuthenticatedRequestOwner(val userId: Long)

/** Runs after AuthInterceptor, comparing the payload owner to the token actually attached to the request. */
class RequestOwnerInterceptor @Inject constructor(private val decoder: JwtDecoder) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val expected = request.tag(AuthenticatedRequestOwner::class.java)
            ?: throw IOException("No se identificó al agente de esta operación.")
        val token = request.header("Authorization")?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")
        // Only the owner is checked here. Expiry is the server's call: an expired token must reach it so that the
        // 401 lets TokenAuthenticator rotate it; rejecting it here blocked every sync an hour after login.
        val user = token?.let(decoder::decodeIdentity)
        if (user == null || user.id != expected.userId || !user.isFieldAgent) {
            throw IOException("La sesión cambió. Los registros siguen guardados para su agente original.")
        }
        return chain.proceed(request)
    }
}
