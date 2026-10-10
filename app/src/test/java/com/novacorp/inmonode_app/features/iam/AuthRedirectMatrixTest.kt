package com.novacorp.inmonode_app.features.iam

import com.novacorp.inmonode_app.features.iam.infrastructure.di.AuthApiModule
import com.novacorp.inmonode_app.features.iam.infrastructure.remote.RefreshTokenRequestDto
import com.novacorp.inmonode_app.features.iam.infrastructure.remote.SignInRequestDto
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Regression for all credential-bearing auth endpoints and both body-preserving redirects. */
@RunWith(Parameterized::class)
class AuthRedirectMatrixTest(private val status: Int, private val endpoint: String) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0} {1}")
        fun rows(): List<Array<Any>> = listOf(307, 308).flatMap { status ->
            listOf("login", "refresh", "logout").map { arrayOf<Any>(status, it) }
        }
    }

    @Test fun credentialBodyIsNotForwarded() = runBlocking {
        val source = MockWebServer()
        val target = MockWebServer()
        source.start()
        target.start()
        try {
            source.enqueue(MockResponse().setResponseCode(status).setHeader("Location", target.url("/auth/$endpoint")))
            target.enqueue(MockResponse().setResponseCode(204))
            val retrofit = Retrofit.Builder().baseUrl(source.url("/"))
                .client(OkHttpClient()).addConverterFactory(GsonConverterFactory.create()).build()
            val service = AuthApiModule.provideAuthService(retrofit)
            val response = when (endpoint) {
                "login" -> service.signIn(SignInRequestDto("agent@example.test", "synthetic-password"))
                "refresh" -> service.refresh(RefreshTokenRequestDto("synthetic-refresh"))
                else -> service.signOut(RefreshTokenRequestDto("synthetic-refresh"))
            }
            assertEquals(status, response.code())
            assertEquals(0, target.requestCount)
        } finally {
            source.shutdown()
            target.shutdown()
        }
    }
}
