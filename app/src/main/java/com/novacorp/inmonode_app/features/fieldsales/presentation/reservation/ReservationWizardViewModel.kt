package com.novacorp.inmonode_app.features.fieldsales.presentation.reservation
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.*
import com.novacorp.inmonode_app.features.fieldsales.application.*
import com.novacorp.inmonode_app.features.fieldsales.domain.model.NegotiationTerms
@HiltViewModel
class ReservationWizardViewModel @Inject constructor(portfolio:PortfolioRepository,prospects:ProspectRepository,
 reservations:ReservationRepository,private val begin:BeginReservationUseCase,private val confirm:ReserveLotUseCase,
 private val cancel:CancelReservationDraftUseCase,private val complete:CompleteProspectUseCase,private val saved:SavedStateHandle):ViewModel() {
 private val _uiState=MutableStateFlow(ReservationWizardUiState(prospectId=saved["prospectId"],amount=saved["amount"]?:"",
  discount=saved["discount"]?:"",bonus=saved["bonus"]?:"",notes=saved["notes"]?:""))
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
 // Editing a step-1 field clears the previous attempt's error, which may no longer apply.
 fun select(id:String) { saved["prospectId"]=id;_uiState.update { it.copy(prospectId=id,errorMessage=null) } }
 fun amount(v:String) { saved["amount"]=v;_uiState.update { it.copy(amount=v,errorMessage=null) } }
 fun discount(v:String) { saved["discount"]=v;_uiState.update { it.copy(discount=v,errorMessage=null) } }
 fun bonus(v:String) { val text=v.take(NegotiationTerms.MAX_BONUS_LENGTH);saved["bonus"]=text;_uiState.update { it.copy(bonus=text,errorMessage=null) } }
 fun notes(v:String) { val text=v.take(NegotiationTerms.MAX_NOTES_LENGTH);saved["notes"]=text;_uiState.update { it.copy(notes=text,errorMessage=null) } }
 fun begin() { if(_uiState.value.working)return;val s=_uiState.value
  val lot=s.lot;val person=s.prospectId;val amount=s.amount.replace(',','.').toBigDecimalOrNull()
  if(lot==null||person==null||amount==null) { _uiState.update { it.copy(errorMessage="Selecciona un prospecto e ingresa un monto válido.") };return }
  // The repository checks the discount range again before storing anything (US-58, scenario 2).
  val terms=s.terms?:run { _uiState.update { it.copy(errorMessage=ReservationWizardUiState.INVALID_DISCOUNT) };return }
  viewModelScope.launch { _uiState.update { it.copy(working=true,errorMessage=null) }
   begin(lot.id,person,amount,terms).fold({ row -> saved["reservationId"]=row.id;_uiState.update { it.copy(working=false,reservation=row) } },
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
