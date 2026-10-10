package com.novacorp.inmonode_app.features.vouchers.infrastructure.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Tag
import com.novacorp.inmonode_app.core.network.AuthenticatedRequestOwner

interface VoucherService {
    @POST("vouchers/upload-url")
    suspend fun requestUpload(@Body request: UploadUrlRequestDto, @Tag owner: AuthenticatedRequestOwner): Response<UploadUrlResponseDto>
    @POST("vouchers")
    suspend fun register(@Body request: RegisterVoucherRequestDto, @Tag owner: AuthenticatedRequestOwner): Response<VoucherRegistrationDto>
    @GET("reservations/{transactionId}/payment-evidences")
    suspend fun paymentEvidences(@Path("transactionId") transactionId: String, @Tag owner: AuthenticatedRequestOwner): Response<PaymentEvidencesDto>
}
