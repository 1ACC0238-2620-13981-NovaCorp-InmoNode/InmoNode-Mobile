package com.novacorp.inmonode_app.core.network

import com.novacorp.inmonode_app.BuildConfig
import com.novacorp.inmonode_app.features.iam.infrastructure.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject

private val apiHost = BuildConfig.BASE_URL.toHttpUrl().host

/** True for calls to the InmoNode API (not for presigned storage URLs). */
internal fun Request.isApiRequest(): Boolean = url.host == apiHost

/** True for the public auth endpoints (login, refresh, logout, ...), which never carry a token. */
internal fun Request.isAuthRequest(): Boolean = url.encodedPath.contains("/auth/")

/** Adds "Authorization: Bearer <token>" to every protected API request. */
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.isApiRequest() || request.isAuthRequest()) return chain.proceed(request)

        val token = runBlocking { tokenManager.getAccessToken() } ?: return chain.proceed(request)
        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        )
    }
}
