package com.novacorp.inmonode_app.features.fieldsales.infrastructure.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Tag
import com.novacorp.inmonode_app.core.network.AuthenticatedRequestOwner

interface FieldSyncService {
    @GET("field-sync/portfolio")
    suspend fun portfolio(@Header("If-None-Match") etag: String?, @Tag owner: AuthenticatedRequestOwner): Response<FieldPortfolioDto>
    @POST("field-sync")
    suspend fun synchronize(@Body records: FieldSyncRequestDto, @Tag owner: AuthenticatedRequestOwner): Response<FieldSyncResultDto>
}
