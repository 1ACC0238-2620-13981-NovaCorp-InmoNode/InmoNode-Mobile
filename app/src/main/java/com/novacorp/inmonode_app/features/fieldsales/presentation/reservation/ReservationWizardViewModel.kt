package com.novacorp.inmonode_app.features.fieldsales.presentation.reservation
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.*
import com.novacorp.inmonode_app.features.fieldsales.application.*
@HiltViewModel
class ReservationWizardViewModel @Inject constructor(portfolio:PortfolioRepository,prospects:ProspectRepository,
 reservations:ReservationRepository,private val begin:BeginReservationUseCase,private val confirm:ReserveLotUseCase,
 private val cancel:CancelReservationDraftUseCase,private val complete:CompleteProspectUseCase,private val saved:SavedStateHandle):ViewModel() {
 private val _uiState=MutableStateFlow(ReservationWizardUiState(prospectId=saved["prospectId"],amount=saved["amount"]?:""))
 val uiState=_uiState.asStateFlow()
 init {
  viewModelScope.launch { saved.getStateFlow<String?>("prospectId",null).collect { id -> _uiState.update { it.copy(prospectId=id) } } }
  viewModelScope.launch { combine(portfolio.observePortfolio(),prospects.observeProspects(),reservations.observeReservations()) { p, people, rows ->
  Triple(p,people,rows) }.catch { e -> _uiState.update { it.copy(errorMessage=e.message) } }.collect { (p,people,rows) ->
   val reservation=rows.firstOrNull { it.id==saved.get<String>("reservationId") }
   val lotId=reservation?.lotId?:saved.get<Long>("lotId")
   val project=p.projects.firstOrNull { pr -> pr.lots.any { it.id==lotId } }
   _uiState.update { it.copy(project=project,lot=project?.lots?.firstOrNull { l -> l.id==lotId },prospects=people,reservation=reservation) }
  } } }
 fun select(id:String) { saved["prospectId"]=id;_uiState.update { it.copy(prospectId=id) } }
 fun amount(v:String) { saved["amount"]=v;_uiState.update { it.copy(amount=v) } }
 fun begin() { if(_uiState.value.working)return;val s=_uiState.value
  val lot=s.lot;val person=s.prospectId;val amount=s.amount.replace(',','.').toBigDecimalOrNull()
  if(lot==null||person==null||amount==null) { _uiState.update { it.copy(errorMessage="Selecciona un prospecto e ingresa un monto válido.") };return }
  viewModelScope.launch { _uiState.update { it.copy(working=true,errorMessage=null) }
   begin(lot.id,person,amount).fold({ row -> saved["reservationId"]=row.id;_uiState.update { it.copy(working=false,reservation=row) } },
    { e -> _uiState.update { it.copy(working=false,errorMessage=e.message) } }) } }
 fun confirm() { val row=_uiState.value.reservation?:return;if(_uiState.value.working)return
  viewModelScope.launch { _uiState.update { it.copy(working=true,errorMessage=null) }
   confirm(row.id).fold({ r -> _uiState.update { it.copy(working=false,reservation=r,completed=true) } },
    { e -> _uiState.update { it.copy(working=false,errorMessage=e.message) } }) } }
 fun completeMarital(value:String) { val id=_uiState.value.prospect?.id?:return
  viewModelScope.launch { complete(id,value).onFailure { e -> _uiState.update { it.copy(errorMessage=e.message) } } } }
 fun cancel(onDone:()->Unit) { val row=_uiState.value.reservation?:return
  viewModelScope.launch { cancel(row.id).fold({onDone()},{ e -> _uiState.update { it.copy(errorMessage=e.message) } }) } }
}
