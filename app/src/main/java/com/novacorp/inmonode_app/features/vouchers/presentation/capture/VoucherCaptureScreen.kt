package com.novacorp.inmonode_app.features.vouchers.presentation.capture
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.core.designsystem.components.*
import java.io.File
@Composable
fun VoucherCaptureScreen(modifier:Modifier=Modifier,viewModel:VoucherCaptureViewModel=hiltViewModel(),onBack:()->Unit,onCaptured:(String)->Unit) {
 val s by viewModel.uiState.collectAsStateWithLifecycle()
 val context=LocalContext.current;val lifecycle=LocalLifecycleOwner.current
 val captureDescription=stringResource(R.string.voucher_capture)
 var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) }
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted=it }
 val gallery=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let(viewModel::importUri) }
 val capture=remember { ImageCapture.Builder().build() }
 var flashEnabled by remember { mutableStateOf(false) }
 val previewView=remember { PreviewView(context) }
 var cameraReady by remember { mutableStateOf(false) }
 var hasFlash by remember { mutableStateOf(false) }
 LaunchedEffect(s.voucherId) { s.voucherId?.let(onCaptured) }
 DisposableEffect(granted,lifecycle) {
  var disposed=false;var provider:ProcessCameraProvider?=null;var preview:Preview?=null
  if(granted) {
   val future=ProcessCameraProvider.getInstance(context)
   future.addListener({ if(!disposed) {
    try { provider=future.get();preview=Preview.Builder().build().also { it.surfaceProvider=previewView.surfaceProvider }
     val camera=provider?.bindToLifecycle(lifecycle,CameraSelector.DEFAULT_BACK_CAMERA,preview,capture);hasFlash=camera?.cameraInfo?.hasFlashUnit()==true;cameraReady=true
    } catch(e:Exception) { viewModel.error(e.message?:"No se pudo abrir la cámara.") }
   } },ContextCompat.getMainExecutor(context))
  }
  onDispose { disposed=true;cameraReady=false;preview?.let { provider?.unbind(it,capture) } }
 }
 Column(modifier.fillMaxSize().background(Color(0xFF292929))) {
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
   TextButton(onClick=onBack) { Text(stringResource(R.string.field_back),color=Color.White) }
   Text(stringResource(R.string.voucher_capture),Modifier.weight(1f).padding(top=16.dp),color=Color.White,style=MaterialTheme.typography.titleLarge)
   IconButton(onClick={flashEnabled=!flashEnabled;capture.flashMode=if(flashEnabled)ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF},enabled=granted&&cameraReady&&hasFlash) {
    Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_flash),contentDescription=stringResource(R.string.voucher_flash),tint=if(flashEnabled)MaterialTheme.colorScheme.tertiary else Color.White)
   }
  }
  Box(Modifier.weight(1f).fillMaxWidth()) {
   if(granted) AndroidView(factory={previewView},modifier=Modifier.fillMaxSize())
   else Column(Modifier.align(Alignment.Center).padding(24.dp)) {
    Text(stringResource(R.string.voucher_permission),color=Color.White)
    PrimaryButton(stringResource(R.string.voucher_camera)) { permission.launch(Manifest.permission.CAMERA) }
   }
   if(granted) Box(Modifier.align(Alignment.Center).fillMaxWidth(0.78f).fillMaxHeight(0.72f).border(2.dp,Color.White))
   if(s.working) CircularProgressIndicator(Modifier.align(Alignment.Center))
  }
  Text(stringResource(R.string.voucher_capture_hint),modifier=Modifier.padding(horizontal=32.dp,vertical=8.dp),color=Color.White,style=MaterialTheme.typography.bodySmall)
  FieldError(s.errorMessage)
  Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   IconButton(onClick={gallery.launch("image/*")},enabled=!s.working,modifier=Modifier.weight(1f)) {
    Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_photo_library),contentDescription=stringResource(R.string.voucher_gallery),tint=Color.White) }
   Button(onClick={
    val file=File.createTempFile("voucher-camera-",".jpg",context.cacheDir)
    capture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(),ContextCompat.getMainExecutor(context),
     object:ImageCapture.OnImageSavedCallback {
      override fun onImageSaved(result:ImageCapture.OutputFileResults) { viewModel.captureFile(file) }
      override fun onError(exception:ImageCaptureException) { file.delete();viewModel.error(exception.message?:"No se pudo capturar la imagen.") }
     })
   },enabled=granted&&cameraReady&&!s.working,shape=androidx.compose.foundation.shape.CircleShape,
    colors=ButtonDefaults.buttonColors(containerColor=Color.White),modifier=Modifier.size(72.dp).semantics { contentDescription=captureDescription }) {
    Box(Modifier.size(32.dp))
   }
   Text(stringResource(R.string.voucher_step),Modifier.weight(1f).padding(top=24.dp),color=Color.White,style=MaterialTheme.typography.bodySmall)
  }
 }
}
