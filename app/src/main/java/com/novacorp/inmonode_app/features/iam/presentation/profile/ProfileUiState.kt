package com.novacorp.inmonode_app.features.iam.presentation.profile

import com.novacorp.inmonode_app.features.iam.domain.User

data class ProfileUiState(
    val user: User? = null,
    val isSignOutDialogVisible: Boolean = false,
    val isSigningOut: Boolean = false,
    val projectCount: Int = 0,
    val pendingCount: Int = 0,
    val catalogVersion: String? = null
)
