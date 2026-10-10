package com.novacorp.inmonode_app.features.fieldsales.infrastructure.remote

data class FieldSyncResultDto(val prospectsSynced: Int, val reservations: List<ReservationResultDto>)
data class ReservationResultDto(val id: String, val result: String, val reservationStatus: String?,
    val blockedUntil: String?, val conflictReason: String?, val originalResult: String?) {
    fun effectiveResult(): String = if (result == "DUPLICATE") {
        requireNotNull(originalResult) { "El resultado duplicado no indica el resultado original." }
    } else result
}
