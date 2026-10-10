package com.novacorp.inmonode_app.features.vouchers.infrastructure.remote

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class VoucherFileUploader @Inject constructor() {
    // Presigned storage requests must never carry the app's JWT, cookies, or API authenticator.
    private val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .callTimeout(60, TimeUnit.SECONDS).build()

    suspend fun upload(file: File, target: UploadUrlResponseDto) {
        val url = target.uploadUrl.toHttpUrl()
        require(url.isHttps && url.username.isEmpty() && url.password.isEmpty()) { "La URL de carga no es segura." }
        require(Instant.parse(target.expiresAt).isAfter(Instant.now())) { "La URL de carga venció. Reintenta la sincronización." }
        require(target.headers.entries.any { it.key.equals("Content-Type", true) && it.value == "image/jpeg" })
        require(target.headers.entries.any { it.key.equals("Content-Length", true) && it.value.toLongOrNull() == file.length() })
        require(target.headers.keys.none { it.lowercase() in setOf("authorization", "proxy-authorization", "cookie", "host") })
        val request = Request.Builder().url(url).put(file.asRequestBody("image/jpeg".toMediaType())).apply {
            target.headers.forEach { (name, value) -> header(name, value) }
        }.build()
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(IOException("No se pudo subir el voucher. Puedes reintentar.", e))
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!continuation.isActive) return
                        if (it.isSuccessful) continuation.resume(Unit)
                        else continuation.resumeWithException(IOException("La carga del voucher falló (${it.code}). Reintenta la sincronización."))
                    }
                }
            })
        }
    }
}
