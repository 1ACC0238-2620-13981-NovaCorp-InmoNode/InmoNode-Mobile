package com.novacorp.inmonode_app.features.fieldsales.presentation.prospects
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.*
import com.novacorp.inmonode_app.features.fieldsales.application.*
import com.novacorp.inmonode_app.core.connectivity.ConnectivityMonitor
@HiltViewModel
class ProspectsViewModel @Inject constructor(repository: ProspectRepository, private val register: RegisterProspectUseCase,
 connectivity: ConnectivityMonitor, private val saved: SavedStateHandle): ViewModel() {
 private val _uiState = MutableStateFlow(ProspectsUiState(name=saved["name"]?:"",document=saved["document"]?:"",phone=saved["phone"]?:"",maritalStatus=saved["marital"]?:""))
 val uiState = _uiState.asStateFlow()
 init { viewModelScope.launch { repository.observeProspects().catch { e -> _uiState.update { it.copy(errorMessage=e.message) } }
  .collect { p -> _uiState.update { it.copy(prospects=p,loaded=true) } } }
  viewModelScope.launch { connectivity.isOnline.collect { v -> _uiState.update { it.copy(offline=!v) } } } }
 fun query(v:String) = _uiState.update { it.copy(query=v) }
 fun name(v:String) { saved["name"]=v; _uiState.update { it.copy(name=v,nameError=null) } }
 fun document(v:String) { saved["document"]=v; _uiState.update { it.copy(document=v,documentError=null) } }
 fun phone(v:String) { saved["phone"]=v; _uiState.update { it.copy(phone=v,phoneError=null) } }
 fun marital(v:String) { saved["marital"]=v; _uiState.update { it.copy(maritalStatus=v) } }
 fun save() { if(_uiState.value.saving)return
  val s=_uiState.value
  // Each invalid field shows its own error (M14); the repository checks the same rules again.
  val nameError=ProspectRules.nameError(s.name); val documentError=ProspectRules.documentError(s.document); val phoneError=ProspectRules.phoneError(s.phone)
  if(nameError!=null||documentError!=null||phoneError!=null) { _uiState.update { it.copy(nameError=nameError,documentError=documentError,phoneError=phoneError,errorMessage=null) }; return }
  viewModelScope.launch { _uiState.update { it.copy(saving=true,errorMessage=null) }
   register(s.document,s.name,s.phone,s.maritalStatus.ifBlank { null }).fold(
    { p -> _uiState.update { it.copy(saving=false,savedId=p.id) } },
    { e -> _uiState.update { it.copy(saving=false,errorMessage=e.message) } }) } }
}
