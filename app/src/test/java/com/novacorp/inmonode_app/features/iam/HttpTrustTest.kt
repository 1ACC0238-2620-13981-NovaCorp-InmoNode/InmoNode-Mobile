package com.novacorp.inmonode_app.features.iam

import com.novacorp.inmonode_app.BuildConfig
import com.novacorp.inmonode_app.core.network.isApiRequest
import com.novacorp.inmonode_app.features.iam.infrastructure.di.AuthApiModule
import com.novacorp.inmonode_app.features.iam.infrastructure.remote.SignInRequestDto
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Local synthetic credentials only; no production traffic. */
class HttpTrustTest {
    @Test fun alternateOriginCannotRefreshOrRetry() = runBlocking {
        val manager = com.novacorp.inmonode_app.features.iam.infrastructure.local.TokenManager(MemorySessionStore())
        val token = fixtureToken()
        manager.saveTokens(token, "synthetic-refresh")
        val service = FixtureAuthService()
        val authenticator = com.novacorp.inmonode_app.core.network.TokenAuthenticator(manager, dagger.Lazy { service })
        val original = BuildConfig.BASE_URL.toHttpUrl()
        for (url in listOf(original.newBuilder().port(8443).build(), original.newBuilder().scheme("http").build())) {
            val request = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
            val response = okhttp3.Response.Builder().request(request).protocol(okhttp3.Protocol.HTTP_1_1)
                .code(401).message("Unauthorized").build()
            assertNull(authenticator.authenticate(null, response))
        }
        assertTrue(service.refreshRequests.isEmpty())
    }

    @Test fun authFollowUpCannotCarryBearer() = runBlocking {
        assertNull(captureNetworkAuthorization(BuildConfig.BASE_URL + "auth/login"))
    }

    @Test fun externalFollowUpCannotCarryBearer() = runBlocking {
        assertNull(captureNetworkAuthorization("https://external.example.test/storage"))
    }

    @Test fun protectedOriginRetainsAuthorization() = runBlocking {
        assertEquals("Bearer synthetic-access", captureNetworkAuthorization(BuildConfig.BASE_URL + "field-sync/portfolio"))
    }

    private fun captureNetworkAuthorization(url: String): String? {
        val manager = com.novacorp.inmonode_app.features.iam.infrastructure.local.TokenManager(MemorySessionStore())
        val client = com.novacorp.inmonode_app.core.di.NetworkModule.provideOkHttpClient(
            com.novacorp.inmonode_app.core.network.AuthInterceptor(manager),
            com.novacorp.inmonode_app.core.network.TokenAuthenticator(manager, dagger.Lazy { FixtureAuthService() })
        )
        var request = Request.Builder().url(url).header("Authorization", "Bearer synthetic-access").build()
        // Model the network-stage follow-up request, after OkHttp's redirect processing.
        for (interceptor in client.networkInterceptors) {
            val incoming = request
            val chain = java.lang.reflect.Proxy.newProxyInstance(
                okhttp3.Interceptor.Chain::class.java.classLoader, arrayOf(okhttp3.Interceptor.Chain::class.java)
            ) { _, method, arguments ->
                when (method.name) {
                    "request" -> incoming
                    "proceed" -> {
                        request = arguments!![0] as Request
                        okhttp3.Response.Builder().request(request).protocol(okhttp3.Protocol.HTTP_1_1)
                            .code(200).message("OK").body(okhttp3.ResponseBody.Companion.run { "{}".toResponseBody() }).build()
                    }
                    else -> error("Unexpected Chain call: ${method.name}")
                }
            } as okhttp3.Interceptor.Chain
            interceptor.intercept(chain).close()
        }
        return request.header("Authorization")
    }

    @Test fun originalApiOriginIsAccepted() {
        assertTrue(Request.Builder().url(BuildConfig.BASE_URL).build().isApiRequest())
    }

    @Test fun alternatePortIsNotApiOrigin() {
        val url = BuildConfig.BASE_URL.toHttpUrl().newBuilder().port(8443).build()
        assertFalse(Request.Builder().url(url).build().isApiRequest())
    }

    @Test fun alternateSchemeIsNotApiOrigin() {
        val url = BuildConfig.BASE_URL.toHttpUrl().newBuilder().scheme("http").build()
        assertFalse(Request.Builder().url(url).build().isApiRequest())
    }

    @Test fun loginBodyCannotFollow307ToAnotherOrigin() = runBlocking {
        val source = MockWebServer()
        val target = MockWebServer()
        source.start()
        target.start()
        try {
            source.enqueue(MockResponse().setResponseCode(307).setHeader("Location", target.url("/auth/login")))
            target.enqueue(MockResponse().setBody("{}"))
            val retrofit = Retrofit.Builder().baseUrl(source.url("/"))
                .client(OkHttpClient()).addConverterFactory(GsonConverterFactory.create()).build()
            val service = AuthApiModule.provideAuthService(retrofit)
            val response = service.signIn(SignInRequestDto("agent@example.test", "synthetic-password"))
            assertEquals(307, response.code())
            assertEquals(0, target.requestCount)
        } finally {
            source.shutdown()
            target.shutdown()
        }
    }
}
