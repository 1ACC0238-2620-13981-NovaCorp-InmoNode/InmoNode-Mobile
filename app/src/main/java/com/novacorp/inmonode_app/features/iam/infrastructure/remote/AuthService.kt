package com.novacorp.inmonode_app.features.iam.infrastructure.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    @POST("auth/login")
    suspend fun signIn(@Body request: SignInRequestDto): Response<TokenResponseDto>

    /** Refresh tokens are single-use: always store the rotated pair returned here. */
    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequestDto): Response<TokenResponseDto>

    @POST("auth/logout")
    suspend fun signOut(@Body request: RefreshTokenRequestDto): Response<Unit>
}
