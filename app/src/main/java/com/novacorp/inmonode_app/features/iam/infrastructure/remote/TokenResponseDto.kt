package com.novacorp.inmonode_app.features.iam.infrastructure.remote

data class TokenResponseDto(
    val token: String,
    val tokenType: String,
    val expiresIn: Long,
    val refreshToken: String
)
