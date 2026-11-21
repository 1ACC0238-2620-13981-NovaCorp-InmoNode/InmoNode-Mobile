package com.novacorp.inmonode_app.features.vouchers.presentation.capture
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novacorp.inmonode_app.R
@Composable
fun VoucherCaptureScreen(modifier:Modifier=Modifier,viewModel:VoucherCaptureViewModel=hiltViewModel(),onBack:()->Unit,onCaptured:(String)->Unit) {
 val s by viewModel.uiState.collectAsStateWithLifecycle()
 val gallery=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let(viewModel::importUri) }
 LaunchedEffect(s.voucherId) { s.voucherId?.let(onCaptured) }
 CameraCapturePage(stringResource(R.string.voucher_capture),stringResource(R.string.voucher_capture_hint),stringResource(R.string.voucher_step),
  stringResource(R.string.voucher_permission),s.working,s.errorMessage,"voucher-camera-",onBack,viewModel::captureFile,viewModel::error,modifier) {
  IconButton(onClick={gallery.launch("image/*")},enabled=!s.working,modifier=Modifier.weight(1f)) {
   Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_photo_library),contentDescription=stringResource(R.string.voucher_gallery),tint=Color.White) }
 }
}
