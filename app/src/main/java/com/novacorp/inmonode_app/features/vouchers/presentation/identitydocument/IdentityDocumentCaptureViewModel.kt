package com.novacorp.inmonode_app.features.vouchers.presentation.identitydocument
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.novacorp.inmonode_app.features.vouchers.application.CaptureIdentityDocumentUseCase
data class IdentityDocumentCaptureUiState(val working:Boolean=false,val captured:Boolean=false,val errorMessage:String?=null)
/** US-59: keeps the photo of the buyer's identity document with the separation; the camera file is always deleted. */
@HiltViewModel
class IdentityDocumentCaptureViewModel @Inject constructor(private val capture:CaptureIdentityDocumentUseCase,
 private val saved:SavedStateHandle):ViewModel() {
 private val _uiState=MutableStateFlow(IdentityDocumentCaptureUiState())
 val uiState=_uiState.asStateFlow()
 // The camera file goes before the outcome is shown, so leaving the screen never leaves the buyer's photo behind.
 fun captureFile(file:File) { viewModelScope.launch {
  if(_uiState.value.working) { withContext(NonCancellable+Dispatchers.IO) { file.delete() };return@launch }
  _uiState.update { it.copy(working=true,errorMessage=null) }
  val result=try { capture(requireNotNull(saved.get<String>("reservationId")),file.absolutePath) }
   catch(e:CancellationException) { throw e } catch(e:Exception) { Result.failure(e) }
   finally { withContext(NonCancellable+Dispatchers.IO) { file.delete() } }
  result.fold({ _uiState.update { it.copy(working=false,captured=true) } },
   { e -> _uiState.update { it.copy(working=false,errorMessage=e.message) } })
 } }
 fun error(message:String) = _uiState.update { it.copy(errorMessage=message) }
}
