package com.novacorp.inmonode_app.features.iam.domain

import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /** Signed-in user, or null when there is no session. Works offline. */
    val currentUser: Flow<User?>

    /** Fails with an [AuthError]. */
    suspend fun signIn(email: String, password: String): Result<User>

    /** Always ends the local session, even without connection. */
    suspend fun signOut()
}
