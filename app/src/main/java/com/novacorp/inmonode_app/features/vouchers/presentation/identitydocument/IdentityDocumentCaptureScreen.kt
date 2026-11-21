package com.novacorp.inmonode_app.features.vouchers.presentation.identitydocument
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.features.vouchers.presentation.capture.CameraCapturePage
/**
 * US-59: optional photo of the buyer's identity document. In the separation wizard the agent may skip it when the
 * buyer does not have the document at hand; the separation then shows "Documento de identidad pendiente".
 */
@Composable
fun IdentityDocumentCaptureScreen(inWizard:Boolean,modifier:Modifier=Modifier,viewModel:IdentityDocumentCaptureViewModel=hiltViewModel(),
 onBack:()->Unit,onDone:()->Unit) {
 val s by viewModel.uiState.collectAsStateWithLifecycle()
 LaunchedEffect(s.captured) { if(s.captured) onDone() }
 CameraCapturePage(stringResource(R.string.identity_document_capture),stringResource(R.string.identity_document_hint),
  stringResource(if(inWizard) R.string.identity_document_step else R.string.identity_document_later),
  stringResource(R.string.identity_document_permission),s.working,s.errorMessage,"identity-document-camera-",onBack,
  viewModel::captureFile,viewModel::error,modifier,frame=Modifier.fillMaxWidth(0.86f).aspectRatio(ID_CARD_RATIO)) {
  if(inWizard) TextButton(onClick=onDone,enabled=!s.working,modifier=Modifier.weight(1f)) {
   Text(stringResource(R.string.identity_document_skip),color=Color.White) }
  else Spacer(Modifier.weight(1f))
 }
}
/** ID-1 cards (85.6 × 54 mm), the size of the Peruvian DNI and most identity cards. */
private const val ID_CARD_RATIO=85.6f/54f
