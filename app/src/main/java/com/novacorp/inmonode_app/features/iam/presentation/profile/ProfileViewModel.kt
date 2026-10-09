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
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
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
