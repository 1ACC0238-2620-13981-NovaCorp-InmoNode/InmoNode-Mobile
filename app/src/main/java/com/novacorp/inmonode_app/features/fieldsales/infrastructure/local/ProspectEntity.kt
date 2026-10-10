package com.novacorp.inmonode_app.features.fieldsales.infrastructure.local

import androidx.room3.Entity
import androidx.room3.Index

@Entity(tableName = "prospects", primaryKeys = ["ownerId", "id"], indices = [Index(value = ["ownerId", "document"])])
data class ProspectEntity(val ownerId: Long, val id: String, val document: String,
    val fullName: String, val phone: String, val maritalStatus: String?,
    val registeredAt: String, val synced: Boolean = false)
