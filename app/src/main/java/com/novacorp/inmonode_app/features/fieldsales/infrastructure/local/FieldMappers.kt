package com.novacorp.inmonode_app.features.fieldsales.infrastructure.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import com.novacorp.inmonode_app.features.fieldsales.infrastructure.remote.*
import java.time.Instant

private val gson = Gson()
private val polygonType = object : TypeToken<List<List<List<Double>>>>() {}.type

internal fun PortfolioProjectDto.toEntity(ownerId: Long) = ProjectEntity(ownerId, id, name, location,
    latitude, longitude, coverImageUrl, financingRules.minDownPaymentPercentage.toPlainString(),
    financingRules.annualInterestRate.toPlainString(), financingRules.maxTermMonths, financingRules.lateFeeRate.toPlainString())

internal fun LotFeatureDto.toEntity(ownerId: Long, projectId: Long) = LotEntity(ownerId, id, projectId,
    properties.code, properties.front.toPlainString(), properties.depth.toPlainString(), properties.area.toPlainString(),
    properties.price.toPlainString(), properties.currency, properties.status, gson.toJson(geometry.coordinates))

internal fun LotEntity.toDomain(reservation: ReservationEntity? = null): Lot {
    val rings: List<List<List<Double>>> = gson.fromJson(polygonJson, polygonType)
    return Lot(id, projectId, code, LotDimensions(front.toBigDecimal(), depth.toBigDecimal(), area.toBigDecimal()),
        rings.map { ring -> ring.map { GeoPoint(it[0], it[1]) } }, price.toBigDecimal(), currency,
        if (reservation != null) LotStatus.BLOCKED else LotStatus.valueOf(status), reservation?.id)
}

internal fun ProjectEntity.toDomain(lots: List<Lot>) = Project(id, name, location, latitude, longitude,
    coverImageUrl, FinancingRules(minDownPaymentPercentage.toBigDecimal(), annualInterestRate.toBigDecimal(),
        maxTermMonths, lateFeeRate.toBigDecimal()), lots)

internal fun ProspectEntity.toDomain() = Prospect(id, document, fullName, phone, maritalStatus, registeredAt, synced)
internal fun ReservationEntity.toDomain() = Reservation(id, lotId, prospectId, initialAmount.toBigDecimal(),
    reservedAt, ReservationStatus.valueOf(status), blockedUntil, serverStatus, conflictReason)
internal fun ProspectEntity.toRecord() = ProspectRecordDto(id, document, fullName, phone, registeredAt)
internal fun ReservationEntity.toRecord() = ReservationRecordDto(id, lotId, prospectId, initialAmount.toBigDecimal(), reservedAt)

internal fun ReservationEntity.holdsLot(now: Instant = Instant.now()): Boolean = when (status) {
    "DRAFT", "PENDING_SYNC", "FAILED", "FAILED_VALIDATION" -> true
    "SYNCED" -> serverStatus in setOf("VERIFIED", "RESERVED", "SOLD") ||
        blockedUntil?.let { Instant.parse(it).isAfter(now) } == true
    else -> false
}

/** Reject incomplete data before replacing the last usable offline catalog. */
internal fun FieldPortfolioDto.validate() {
    require(projects.map { it.id }.distinct().size == projects.size) { "Portafolio con proyectos duplicados." }
    val features = projects.flatMap { it.lots.features }
    require(features.map { it.id }.distinct().size == features.size) { "Portafolio con lotes duplicados." }
    projects.forEach { project ->
        require(project.id > 0 && project.name.isNotBlank())
        project.toEntity(0).toDomain(emptyList()) // Validate financing rules as a domain value object.
        require(project.lots.type == "FeatureCollection")
        project.lots.features.forEach { feature ->
            require(feature.id > 0 && feature.type == "Feature" && feature.geometry.type == "Polygon")
            require(feature.properties.price.signum() > 0 && feature.properties.area.signum() > 0)
            require(feature.properties.front.signum() > 0 && feature.properties.depth.signum() > 0)
            java.util.Currency.getInstance(feature.properties.currency)
            LotStatus.valueOf(feature.properties.status)
            require(feature.geometry.coordinates.isNotEmpty())
            feature.geometry.coordinates.forEach { ring ->
                require(ring.size >= 4 && ring.first() == ring.last()) { "El polígono del lote no está cerrado." }
                require(ring.all { it.size >= 2 && it[0].isFinite() && it[1].isFinite() &&
                    it[0] in -180.0..180.0 && it[1] in -90.0..90.0 })
            }
        }
    }
}
