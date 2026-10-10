package com.novacorp.inmonode_app.features.fieldsales

import com.novacorp.inmonode_app.core.network.operationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class OperationCancellationTest {
    @Test fun cancellationIsNeverConvertedToRecoverableFailure() = runTest {
        try {
            operationResult<Unit> { throw CancellationException("test") }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }

    @Test fun networkFailureRetainsItsCauseAndLocalRecoveryMessage() = runTest {
        val cause = IOException("socket failed")
        val answer = operationResult<Unit> { throw cause }
        assertTrue(answer.isFailure)
        assertSame(cause, answer.exceptionOrNull()?.cause)
        assertTrue(answer.exceptionOrNull()?.message.orEmpty().contains("guardados"))
    }
}
