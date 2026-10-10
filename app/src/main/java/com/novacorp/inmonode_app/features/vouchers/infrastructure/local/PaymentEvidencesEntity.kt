package com.novacorp.inmonode_app.features.vouchers.infrastructure.local

import androidx.room3.Entity

@Entity(tableName = "payment_evidences", primaryKeys = ["ownerId", "reservationId"])
data class PaymentEvidencesEntity(val ownerId: Long, val reservationId: String,
    val payloadJson: String, val fetchedAt: String)
