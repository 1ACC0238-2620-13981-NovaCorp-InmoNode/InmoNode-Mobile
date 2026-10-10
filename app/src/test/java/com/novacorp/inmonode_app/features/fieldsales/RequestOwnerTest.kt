package com.novacorp.inmonode_app.features.fieldsales

import com.novacorp.inmonode_app.core.network.AuthenticatedRequestOwner
import com.novacorp.inmonode_app.core.network.RequestOwnerInterceptor
import com.novacorp.inmonode_app.features.iam.infrastructure.local.JwtDecoder
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.Base64

class RequestOwnerTest {
    private fun token(id: Long, role: String = "FIELD_AGENT"): String {
        val json = """{"sub":"$id","email":"agent@example.com","role":"$role","exp":${System.currentTimeMillis() / 1000 + 3600}}"""
        return "header.${Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())}.signature"
    }

    @Test fun switchedAccountCannotSendPreviousAgentsPayload() {
        val server = MockWebServer()
        server.start()
        try {
            val client = OkHttpClient.Builder().addInterceptor(RequestOwnerInterceptor(JwtDecoder())).build()
            for (jwt in listOf(token(2), token(1, "BUYER"), "invalid")) {
                val request = Request.Builder().url(server.url("/field-sync"))
                    .tag(AuthenticatedRequestOwner::class.java, AuthenticatedRequestOwner(1)).header("Authorization", "Bearer $jwt").build()
                try { client.newCall(request).execute().close(); fail("Owner mismatch must stop the request") }
                catch (_: IOException) { }
            }
            assertEquals(0, server.requestCount)
        } finally { server.shutdown() }
    }

    @Test fun matchingAccountCanSendAndLocalOwnerTagIsNotAnApiHeader() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("{}"))
            val request = Request.Builder().url(server.url("/field-sync"))
                .tag(AuthenticatedRequestOwner::class.java, AuthenticatedRequestOwner(1)).header("Authorization", "Bearer ${token(1)}").build()
            OkHttpClient.Builder().addInterceptor(RequestOwnerInterceptor(JwtDecoder())).build()
                .newCall(request).execute().use { assertEquals(200, it.code) }
            val recorded = server.takeRequest()
            assertNull(recorded.getHeader("ownerId"))
            assertNull(recorded.getHeader("userId"))
        } finally { server.shutdown() }
    }
}
