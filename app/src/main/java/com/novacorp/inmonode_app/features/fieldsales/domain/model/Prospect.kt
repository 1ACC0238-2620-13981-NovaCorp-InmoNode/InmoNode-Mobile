package com.novacorp.inmonode_app.features.fieldsales.domain.model

data class Prospect(
    val id: String,
    val document: String,
    val fullName: String,
    val phone: String,
    val maritalStatus: String?,
    val registeredAt: String,
    val synced: Boolean = false,
)
