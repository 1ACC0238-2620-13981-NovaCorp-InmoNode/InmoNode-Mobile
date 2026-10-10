package com.novacorp.inmonode_app.features.iam

import com.novacorp.inmonode_app.features.iam.infrastructure.di.AuthApiModule
import com.novacorp.inmonode_app.features.iam.infrastructure.remote.SignInRequestDto
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.*
import org.junit.Assert.*
import org.junit.Test

class AuthDispatcherTest {
    @Test fun authenticationDoesNotWaitForSaturatedProtectedDispatcher() {
        val server = MockWebServer()
        server.start()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        val client = OkHttpClient.Builder().dispatcher(Dispatcher().apply { maxRequests = 1 }).addInterceptor { chain ->
            if (chain.request().url.encodedPath == "/occupied") {
                entered.countDown()
                release.await(5, TimeUnit.SECONDS)
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK").body("{}".toResponseBody()).build()
            } else chain.proceed(chain.request())
        }.build()
        try {
            client.newCall(Request.Builder().url(server.url("/occupied")).build()).enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {}
                override fun onResponse(call: Call, response: Response) { response.close() }
            })
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            server.enqueue(MockResponse().setBody("{\"token\":\"synthetic-token\",\"tokenType\":\"Bearer\",\"expiresIn\":3600,\"refreshToken\":\"synthetic-refresh\"}"))
            val retrofit = Retrofit.Builder().baseUrl(server.url("/"))
                .client(client).addConverterFactory(GsonConverterFactory.create()).build()
            val service = AuthApiModule.provideAuthService(retrofit)
            val result = executor.submit<Boolean> {
                runBlocking { service.signIn(SignInRequestDto("agent@example.test", "synthetic-password")).isSuccessful }
            }
            assertTrue("Auth must use an independent dispatcher", result.get(2, TimeUnit.SECONDS))
        } finally {
            release.countDown()
            client.dispatcher.cancelAll()
            executor.shutdownNow()
            server.shutdown()
        }
    }
}
