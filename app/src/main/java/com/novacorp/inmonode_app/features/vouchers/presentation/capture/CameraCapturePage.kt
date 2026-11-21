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
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.core.designsystem.components.*
import java.io.File
/**
 * Full-screen back camera with a white frame to fit the paper in, shared by the voucher (US-07) and the buyer's
 * identity document (US-59). [onPhoto] receives a temporary JPEG in the cache that the caller must delete;
 * [leading] fills the space left of the shutter (for example, a gallery or a skip button).
 */
@Composable
fun CameraCapturePage(title:String,hint:String,footer:String,permissionText:String,working:Boolean,errorMessage:String?,
 filePrefix:String,onBack:()->Unit,onPhoto:(File)->Unit,onError:(String)->Unit,modifier:Modifier=Modifier,
 frame:Modifier=Modifier.fillMaxWidth(0.78f).fillMaxHeight(0.72f),leading:@Composable RowScope.()->Unit={ Spacer(Modifier.weight(1f)) }) {
 val context=LocalContext.current;val lifecycle=LocalLifecycleOwner.current
 var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) }
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted=it }
 val capture=remember { ImageCapture.Builder().build() }
 var flashEnabled by remember { mutableStateOf(false) }
 val previewView=remember { PreviewView(context) }
 var cameraReady by remember { mutableStateOf(false) }
 var hasFlash by remember { mutableStateOf(false) }
 DisposableEffect(granted,lifecycle) {
  var disposed=false;var provider:ProcessCameraProvider?=null;var preview:Preview?=null
  if(granted) {
   val future=ProcessCameraProvider.getInstance(context)
   future.addListener({ if(!disposed) {
    try { provider=future.get();preview=Preview.Builder().build().also { it.surfaceProvider=previewView.surfaceProvider }
     val camera=provider?.bindToLifecycle(lifecycle,CameraSelector.DEFAULT_BACK_CAMERA,preview,capture);hasFlash=camera?.cameraInfo?.hasFlashUnit()==true;cameraReady=true
    } catch(e:Exception) { onError(e.message?:"No se pudo abrir la cámara.") }
   } },ContextCompat.getMainExecutor(context))
  }
  onDispose { disposed=true;cameraReady=false;preview?.let { provider?.unbind(it,capture) } }
 }
 Column(modifier.fillMaxSize().background(Color(0xFF292929))) {
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
   TextButton(onClick=onBack) { Text(stringResource(R.string.field_back),color=Color.White) }
   Text(title,Modifier.weight(1f).padding(top=16.dp),color=Color.White,style=MaterialTheme.typography.titleLarge)
   IconButton(onClick={flashEnabled=!flashEnabled;capture.flashMode=if(flashEnabled)ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF},enabled=granted&&cameraReady&&hasFlash) {
    Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_flash),contentDescription=stringResource(R.string.voucher_flash),tint=if(flashEnabled)MaterialTheme.colorScheme.tertiary else Color.White)
   }
  }
  Box(Modifier.weight(1f).fillMaxWidth()) {
   if(granted) AndroidView(factory={previewView},modifier=Modifier.fillMaxSize())
   else Column(Modifier.align(Alignment.Center).padding(24.dp)) {
    Text(permissionText,color=Color.White)
    PrimaryButton(stringResource(R.string.voucher_camera)) { permission.launch(Manifest.permission.CAMERA) }
   }
   if(granted) Box(Modifier.align(Alignment.Center).then(frame).border(2.dp,Color.White))
   if(working) CircularProgressIndicator(Modifier.align(Alignment.Center))
  }
  Text(hint,modifier=Modifier.padding(horizontal=32.dp,vertical=8.dp),color=Color.White,style=MaterialTheme.typography.bodySmall)
  FieldError(errorMessage)
  Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   leading()
   Button(onClick={
    val file=File.createTempFile(filePrefix,".jpg",context.cacheDir)
    capture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(),ContextCompat.getMainExecutor(context),
     object:ImageCapture.OnImageSavedCallback {
      override fun onImageSaved(result:ImageCapture.OutputFileResults) { onPhoto(file) }
      override fun onError(exception:ImageCaptureException) { file.delete();onError(exception.message?:"No se pudo capturar la imagen.") }
     })
   },enabled=granted&&cameraReady&&!working,shape=androidx.compose.foundation.shape.CircleShape,
    colors=ButtonDefaults.buttonColors(containerColor=Color.White),modifier=Modifier.size(72.dp).semantics { contentDescription=title }) {
    Box(Modifier.size(32.dp))
   }
   Text(footer,Modifier.weight(1f).padding(top=24.dp),color=Color.White,style=MaterialTheme.typography.bodySmall)
  }
 }
}
