package com.novacorp.inmonode_app.features.fieldsales.presentation.map
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
data class CadastralMapUiState(val portfolio: Portfolio = Portfolio(emptyList(), null, null),
    val projectId: Long? = null, val selectedLotId: Long? = null, val availableOnly: Boolean = false,
    val areaFilter: Boolean = false, val minArea: Float = 0f, val maxArea: Float = 10000f,
    val connectivityKnown: Boolean = false, val loaded: Boolean = false, val offline: Boolean = false, val errorMessage: String? = null) {
    val project: Project? get() = portfolio.projects.firstOrNull { it.id == projectId } ?: portfolio.projects.firstOrNull()
    val selectedLot: Lot? get() = project?.lots?.firstOrNull { it.id == selectedLotId }
    fun matches(lot: Lot) = (!availableOnly || lot.isAvailable()) &&
        (!areaFilter || lot.dimensions.area.toFloat() in minArea..maxArea)
}
