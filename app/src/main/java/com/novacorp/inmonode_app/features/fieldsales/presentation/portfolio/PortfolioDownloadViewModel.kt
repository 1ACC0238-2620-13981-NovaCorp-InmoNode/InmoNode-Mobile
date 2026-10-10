package com.novacorp.inmonode_app.features.fieldsales.presentation.portfolio
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.novacorp.inmonode_app.features.fieldsales.application.PortfolioDownloadCoordinator
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.PortfolioRepository
@HiltViewModel
class PortfolioDownloadViewModel @Inject constructor(private val download: PortfolioDownloadCoordinator,
    repository: PortfolioRepository): ViewModel() {
    private val _uiState = MutableStateFlow(PortfolioDownloadUiState())
    val uiState = _uiState.asStateFlow()
    init {
        viewModelScope.launch {
            repository.observePortfolio().catch { e -> _uiState.update { it.copy(errorMessage=e.message) } }
                .collect { p -> _uiState.update { it.copy(portfolio=p) } }
        }
        viewModelScope.launch {
            download.observe().collect { transfer ->
                _uiState.update { it.copy(loading=transfer.loading,completed=transfer.completed,errorMessage=transfer.errorMessage) }
            }
        }
        retry()
    }
    fun retry() { download.start() }
}
