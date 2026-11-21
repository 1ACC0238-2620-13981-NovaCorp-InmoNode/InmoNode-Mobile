package com.novacorp.inmonode_app.features.vouchers

import androidx.lifecycle.SavedStateHandle
import com.novacorp.inmonode_app.features.vouchers.application.CaptureIdentityDocumentUseCase
import com.novacorp.inmonode_app.features.vouchers.domain.IdentityDocument
import com.novacorp.inmonode_app.features.vouchers.domain.IdentityDocumentRepository
import com.novacorp.inmonode_app.features.vouchers.domain.IdentityDocumentStatus
import com.novacorp.inmonode_app.features.vouchers.presentation.identitydocument.IdentityDocumentCaptureViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** US-59: the photo of the buyer's identity document is kept with its separation; the camera file never stays. */
class IdentityDocumentCaptureTest {
    private class Documents(private val result: Result<Unit>) : IdentityDocumentRepository {
        val captured = mutableListOf<Pair<String, String>>()
        override suspend fun capture(reservationId: String, imagePath: String): Result<IdentityDocument> {
            captured += reservationId to imagePath
            return result.map { IdentityDocument("d1", reservationId, "kept.jpg", IdentityDocumentStatus.READY_TO_SYNC, "2026-11-21T10:00:00Z") }
        }
        override fun observeDocuments() = flowOf(emptyList<IdentityDocument>())
        override suspend fun upload(documentId: String): Result<IdentityDocument> = error("unused")
        override suspend fun discardCancelled(): Result<Unit> = error("unused")
    }

    private fun viewModel(documents: Documents) =
        IdentityDocumentCaptureViewModel(CaptureIdentityDocumentUseCase(documents), SavedStateHandle(mapOf("reservationId" to "r1")))

    @Test fun aCapturedPhotoIsKeptWithItsSeparation() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val documents = Documents(Result.success(Unit))
            val photo = File.createTempFile("identity-document-camera-", ".jpg")
            val viewModel = viewModel(documents)

            viewModel.captureFile(photo)
            val state = viewModel.uiState.first { it.captured || it.errorMessage != null }

            assertTrue(state.captured)
            assertEquals(listOf("r1" to photo.absolutePath), documents.captured)
            assertFalse("the camera file is deleted once the compressed copy is stored", photo.exists())
        } finally { Dispatchers.resetMain() }
    }

    @Test fun aFailedCaptureShowsWhyAndStaysOnTheCamera() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val documents = Documents(Result.failure(IllegalArgumentException("La imagen no es legible. Vuelve a capturar el documento de identidad.")))
            val photo = File.createTempFile("identity-document-camera-", ".jpg")
            val viewModel = viewModel(documents)

            viewModel.captureFile(photo)
            val state = viewModel.uiState.first { it.captured || it.errorMessage != null }

            assertFalse(state.captured)
            assertFalse(state.working)
            assertEquals("La imagen no es legible. Vuelve a capturar el documento de identidad.", state.errorMessage)
            assertFalse(photo.exists())
        } finally { Dispatchers.resetMain() }
    }
}
