package com.novacorp.inmonode_app.features.iam.infrastructure.remote

data class SignInRequestDto(
    val email: String,
    val password: String
)
