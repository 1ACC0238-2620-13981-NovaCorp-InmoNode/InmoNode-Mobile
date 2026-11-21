package com.novacorp.inmonode_app.features.vouchers.infrastructure.remote

import com.novacorp.inmonode_app.core.network.AuthenticatedRequestOwner
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Tag

/** US-59: the photo of the buyer's identity document of a synchronized field separation (backend T55). */
interface IdentityDocumentService {
    @POST("field-sync/reservations/{transactionId}/identity-document/upload-url")
    suspend fun requestUpload(@Path("transactionId") transactionId: String, @Body request: IdentityDocumentFileDto,
        @Tag owner: AuthenticatedRequestOwner): Response<UploadUrlResponseDto>
    @POST("field-sync/reservations/{transactionId}/identity-document")
    suspend fun register(@Path("transactionId") transactionId: String, @Body request: IdentityDocumentFileDto,
        @Tag owner: AuthenticatedRequestOwner): Response<IdentityDocumentRegistrationDto>
}

/** The same values ask for the upload URL and register the uploaded photo; documentId is generated on the device. */
data class IdentityDocumentFileDto(val documentId: String, val contentType: String, val sizeBytes: Long)

/** [result] is RECEIVED, or DUPLICATE when the server already had this photo (a lost response was retried). */
data class IdentityDocumentRegistrationDto(val transactionId: String, val documentId: String, val contentType: String,
    val sizeBytes: Long, val attachedAt: String, val result: String)
