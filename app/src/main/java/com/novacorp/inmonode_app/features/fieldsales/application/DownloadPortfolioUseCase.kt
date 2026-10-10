package com.novacorp.inmonode_app.features.fieldsales.application

import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.PortfolioRepository
import javax.inject.Inject

class DownloadPortfolioUseCase @Inject constructor(private val repository: PortfolioRepository) {
    suspend operator fun invoke() = repository.download()
}
