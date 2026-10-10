package com.novacorp.inmonode_app.features.fieldsales.presentation.portfolio
import com.novacorp.inmonode_app.features.fieldsales.domain.model.Portfolio
data class PortfolioDownloadUiState(val portfolio: Portfolio = Portfolio(emptyList(),null,null),
    val loading: Boolean = false, val completed: Boolean = false, val errorMessage: String? = null)
