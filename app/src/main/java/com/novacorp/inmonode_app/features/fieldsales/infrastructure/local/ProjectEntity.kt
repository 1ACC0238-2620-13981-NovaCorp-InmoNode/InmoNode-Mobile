package com.novacorp.inmonode_app.features.fieldsales.infrastructure.local

import androidx.room3.Entity

@Entity(tableName = "projects", primaryKeys = ["ownerId", "id"])
data class ProjectEntity(val ownerId: Long, val id: Long, val name: String, val location: String,
    val latitude: Double?, val longitude: Double?, val coverImageUrl: String?,
    val minDownPaymentPercentage: String, val annualInterestRate: String,
    val maxTermMonths: Int, val lateFeeRate: String)
