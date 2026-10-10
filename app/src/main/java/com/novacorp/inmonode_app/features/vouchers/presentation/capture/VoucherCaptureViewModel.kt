package com.novacorp.inmonode_app.features.vouchers.presentation.capture
import android.content.Context
import android.net.Uri
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.novacorp.inmonode_app.features.vouchers.application.CaptureVoucherUseCase
@HiltViewModel
class VoucherCaptureViewModel @Inject constructor(private val capture:CaptureVoucherUseCase,
 private val saved:SavedStateHandle,@ApplicationContext private val context:Context):ViewModel() {
 private val _uiState=MutableStateFlow(VoucherCaptureUiState())
 val uiState=_uiState.asStateFlow()
 fun captureFile(file:File) { import { file } }
 fun importUri(uri:Uri) { import {
  val target=File.createTempFile("voucher-import-",".image",context.cacheDir)
  try { context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { output ->
   val buffer=ByteArray(8192);var total=0L
   while(true) { val count=input.read(buffer);if(count<0)break;total+=count
    require(total<=40L*1024*1024) { "La imagen supera el tamaño permitido." };output.write(buffer,0,count) }
  } } ?: error("No se pudo leer la imagen seleccionada.");target }
  catch(e:Exception) { target.delete();throw e }
 } }
 private fun import(source:()->File) { if(_uiState.value.working)return
  viewModelScope.launch { _uiState.update { it.copy(working=true,errorMessage=null) }
   var file:File?=null
   try { file=withContext(Dispatchers.IO) { source() }
    val id=requireNotNull(saved.get<String>("reservationId"))
    capture(id,file.absolutePath).fold({ voucher -> _uiState.update { it.copy(working=false,voucherId=voucher.id) } },
     { e -> _uiState.update { it.copy(working=false,errorMessage=e.message) } })
   } catch(e:CancellationException) { throw e }
   catch(e:Exception) { _uiState.update { it.copy(working=false,errorMessage=e.message) } }
   finally { withContext(NonCancellable+Dispatchers.IO) { file?.delete() } }
  } }
 fun error(message:String) = _uiState.update { it.copy(errorMessage=message) }
}
