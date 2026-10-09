package com.novacorp.inmonode_app.features.iam.domain

data class User(
    val id: Long,
    val email: String,
    val role: UserRole
) {
    val isFieldAgent: Boolean get() = role == UserRole.FIELD_AGENT
}
