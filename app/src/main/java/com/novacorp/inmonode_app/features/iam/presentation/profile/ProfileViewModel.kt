package com.novacorp.inmonode_app.features.iam.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novacorp.inmonode_app.features.iam.application.GetCurrentUserUseCase
import com.novacorp.inmonode_app.features.iam.application.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    private val signOutUseCase: SignOutUseCase,
    portfolio: com.novacorp.inmonode_app.features.fieldsales.domain.repositories.PortfolioRepository,
    prospects: com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ProspectRepository,
    reservations: com.novacorp.inmonode_app.features.fieldsales.domain.repositories.ReservationRepository,
    vouchers: com.novacorp.inmonode_app.features.vouchers.domain.VoucherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(portfolio.observePortfolio(),prospects.observeProspects(),
                reservations.observeReservations(),vouchers.observeVouchers()) { catalog,people,rows,receipts ->
                Triple(catalog.projects.size,catalog.downloadedAt,people.count { !it.synced } +
                    rows.count { it.status in listOf(com.novacorp.inmonode_app.features.fieldsales.domain.model.ReservationStatus.PENDING_SYNC,
                        com.novacorp.inmonode_app.features.fieldsales.domain.model.ReservationStatus.FAILED,
                        com.novacorp.inmonode_app.features.fieldsales.domain.model.ReservationStatus.FAILED_VALIDATION) } +
                    receipts.count { it.status==com.novacorp.inmonode_app.features.vouchers.domain.VoucherStatus.READY_TO_SYNC })
            }.collect { (count,version,pending) -> _uiState.update { it.copy(projectCount=count,catalogVersion=version,pendingCount=pending) } }
        }
        viewModelScope.launch {
            getCurrentUser().collect { user ->
                _uiState.update { currentState -> currentState.copy(user = user) }
            }
        }
    }

    fun onSignOutClicked() {
        _uiState.update { currentState -> currentState.copy(isSignOutDialogVisible = true) }
    }

    fun onSignOutDismissed() {
        _uiState.update { currentState -> currentState.copy(isSignOutDialogVisible = false) }
    }

    /** The app navigation reacts to the ended session and shows the login screen. */
    fun signOut() {
        _uiState.update { currentState ->
            currentState.copy(isSignOutDialogVisible = false, isSigningOut = true)
        }
        viewModelScope.launch {
            signOutUseCase()
        }
    }
}
