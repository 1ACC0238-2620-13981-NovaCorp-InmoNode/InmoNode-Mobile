package com.novacorp.inmonode_app.features.fieldsales.infrastructure.local

import androidx.room3.Entity
import androidx.room3.Index

@Entity(tableName = "lots", primaryKeys = ["ownerId", "id"], indices = [Index(value = ["ownerId", "projectId"])])
data class LotEntity(val ownerId: Long, val id: Long, val projectId: Long, val code: String,
    val front: String, val depth: String, val area: String, val price: String,
    val currency: String, val status: String, val polygonJson: String)
