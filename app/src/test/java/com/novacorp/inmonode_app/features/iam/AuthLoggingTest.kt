package com.novacorp.inmonode_app.features.iam

import com.novacorp.inmonode_app.core.di.NetworkModule
import com.novacorp.inmonode_app.core.network.*
import com.novacorp.inmonode_app.features.iam.infrastructure.local.TokenManager
import dagger.Lazy
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class AuthLoggingTest {
    @Test fun configuredLoggerOmitsAuthBodiesAndHeaders() {
        val manager = TokenManager(MemorySessionStore())
        val client = NetworkModule.provideOkHttpClient(AuthInterceptor(manager), TokenAuthenticator(manager, Lazy { FixtureAuthService() }))
        val configured = client.interceptors.filterIsInstance<HttpLoggingInterceptor>().single()
        val logs = mutableListOf<String>()
        val capturing = HttpLoggingInterceptor { logs.add(it) }.apply { level = configured.level }
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("{\"token\":\"synthetic-access\",\"refreshToken\":\"synthetic-refresh\"}"))
            OkHttpClient.Builder().addInterceptor(capturing).build().newCall(
                Request.Builder().url(server.url("/auth/login"))
                    .header("Authorization", "Bearer synthetic-header")
                    .post("{\"password\":\"synthetic-password\"}".toRequestBody()).build()
            ).execute().use { assertEquals(200, it.code) }
        }
        val output = logs.joinToString("\n")
        assertFalse(output.contains("synthetic-password"))
        assertFalse(output.contains("synthetic-access"))
        assertFalse(output.contains("synthetic-refresh"))
        assertFalse(output.contains("synthetic-header"))
        assertTrue(output.contains("POST"))
        assertTrue(output.contains("200"))
        assertTrue(output.contains("ms"))
    }
}
