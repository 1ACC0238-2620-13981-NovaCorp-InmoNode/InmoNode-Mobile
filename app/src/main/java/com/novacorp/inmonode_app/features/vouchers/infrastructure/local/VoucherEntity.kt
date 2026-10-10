package com.novacorp.inmonode_app.features.vouchers.infrastructure.local

import androidx.room3.Entity
import androidx.room3.Index

@Entity(tableName = "vouchers", primaryKeys = ["ownerId", "id"], indices = [Index(value = ["ownerId", "reservationId"])])
data class VoucherEntity(val ownerId: Long, val id: String, val reservationId: String,
    val imagePath: String, val dataJson: String, val manuallyCorrected: Boolean = false,
    val status: String = "PENDING_OCR", val capturedAt: String,
    val uploadPath: String? = null, val uploaded: Boolean = false, val errorMessage: String? = null)
