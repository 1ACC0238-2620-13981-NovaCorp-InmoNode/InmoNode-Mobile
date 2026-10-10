package com.novacorp.inmonode_app.features.fieldsales.presentation.map
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.novacorp.inmonode_app.core.connectivity.ConnectivityMonitor
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.PortfolioRepository
@HiltViewModel
class CadastralMapViewModel @Inject constructor(repository: PortfolioRepository, connectivity: ConnectivityMonitor): ViewModel() {
    private val _uiState = MutableStateFlow(CadastralMapUiState())
    val uiState = _uiState.asStateFlow()
    init {
        viewModelScope.launch { repository.observePortfolio().catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
            .collect { portfolio -> _uiState.update { it.copy(portfolio = portfolio, loaded = true) } } }
        viewModelScope.launch { connectivity.isOnline.collect { online -> _uiState.update { it.copy(offline = !online, connectivityKnown = true) } } }
    }
    fun project(id: Long) = _uiState.update { it.copy(projectId = id, selectedLotId = null, areaFilter = false) }
    fun select(id: Long?) = _uiState.update { it.copy(selectedLotId = id) }
    fun available(value: Boolean) = _uiState.update { it.copy(availableOnly = value) }
    fun filter(value: Boolean) = _uiState.update { it.copy(areaFilter = value) }
    fun area(range: ClosedFloatingPointRange<Float>) = _uiState.update { it.copy(minArea = range.start, maxArea = range.endInclusive) }
}
