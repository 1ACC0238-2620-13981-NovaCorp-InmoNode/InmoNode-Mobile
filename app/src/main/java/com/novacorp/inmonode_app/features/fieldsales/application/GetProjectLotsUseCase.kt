package com.novacorp.inmonode_app.features.fieldsales.application

import com.novacorp.inmonode_app.features.fieldsales.domain.model.LotStatus
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.PortfolioRepository
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject

class GetProjectLotsUseCase @Inject constructor(private val repository: PortfolioRepository) {
    operator fun invoke(projectId: Long, status: LotStatus? = null, minArea: BigDecimal? = null, maxArea: BigDecimal? = null) =
        repository.observePortfolio().map { portfolio ->
            portfolio.projects.find { it.id == projectId }?.lots.orEmpty().filter { lot ->
                (status == null || lot.status == status) &&
                    (minArea == null || lot.dimensions.area >= minArea) &&
                    (maxArea == null || lot.dimensions.area <= maxArea)
            }
        }
}
