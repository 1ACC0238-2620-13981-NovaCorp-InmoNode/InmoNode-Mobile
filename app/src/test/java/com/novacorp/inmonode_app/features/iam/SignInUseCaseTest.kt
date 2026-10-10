package com.novacorp.inmonode_app.features.iam

import com.novacorp.inmonode_app.features.iam.application.SignInUseCase
import com.novacorp.inmonode_app.features.iam.domain.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/** All identities and passwords in this suite are synthetic fixtures. */
class RecordingAuthRepository : AuthRepository {
    override val currentUser = flowOf<User?>(null)
    var calls = 0
    var receivedEmail = ""
    var receivedPassword = ""
    var action: suspend () -> Result<User> = { Result.success(User(1, "agent@example.test", UserRole.FIELD_AGENT)) }
    override suspend fun signIn(email: String, password: String): Result<User> {
        calls++
        receivedEmail = email
        receivedPassword = password
        return action()
    }
    override suspend fun signOut() = Unit
}

class SignInUseCaseTest {
    @Test fun malformedEmailDisablesSubmit() {
        assertFalse(com.novacorp.inmonode_app.features.iam.presentation.login.LoginUiState(email = "bad", password = "fixture").canSubmit)
    }
    @Test fun malformedEmailNeverCallsRepository() = runTest {
        val repository = RecordingAuthRepository()
        assertTrue(SignInUseCase(repository)("not-an-email", "fixture").isFailure)
        assertEquals(0, repository.calls)
    }
}
