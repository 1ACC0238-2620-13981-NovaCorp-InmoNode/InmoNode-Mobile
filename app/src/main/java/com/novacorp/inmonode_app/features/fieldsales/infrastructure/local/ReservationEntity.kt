package com.novacorp.inmonode_app.features.fieldsales.infrastructure.local

import androidx.room3.Entity
import androidx.room3.Index

@Entity(tableName = "reservations", primaryKeys = ["ownerId", "id"], indices = [Index(value = ["ownerId", "lotId"]), Index(value = ["ownerId", "prospectId"])])
data class ReservationEntity(val ownerId: Long, val id: String, val lotId: Long,
    val prospectId: String, val initialAmount: String, val reservedAt: String,
    val status: String = "PENDING_SYNC", val blockedUntil: String? = null,
    val serverStatus: String? = null, val conflictReason: String? = null)
