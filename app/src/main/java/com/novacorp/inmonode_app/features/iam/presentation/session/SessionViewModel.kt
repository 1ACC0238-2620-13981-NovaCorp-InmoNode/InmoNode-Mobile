package com.novacorp.inmonode_app.features.iam.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novacorp.inmonode_app.features.iam.application.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * App-wide session state. It turns false after a sign out or when the refresh token is rejected,
 * so the navigation can send the agent back to the login screen.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase
) : ViewModel() {

    /** Null until the stored session has been read. */
    val isSignedIn: StateFlow<Boolean?> = getCurrentUser()
        .map { user -> user != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
