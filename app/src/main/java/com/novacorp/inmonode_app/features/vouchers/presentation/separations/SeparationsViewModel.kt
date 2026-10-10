package com.novacorp.inmonode_app.features.vouchers.presentation.separations
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.*
import com.novacorp.inmonode_app.features.fieldsales.domain.model.ReservationStatus
import com.novacorp.inmonode_app.features.vouchers.application.GetPaymentEvidencesUseCase
@HiltViewModel
class SeparationsViewModel @Inject constructor(reservations:ReservationRepository,prospects:ProspectRepository,portfolio:PortfolioRepository,
 private val getEvidences:GetPaymentEvidencesUseCase,saved:SavedStateHandle):ViewModel() {
 private val _uiState=MutableStateFlow(SeparationsUiState(selectedId=saved["reservationId"]))
 val uiState=_uiState.asStateFlow()
 private var requested=false
 private var listJob:Job?=null
 private val queried=mutableSetOf<String>()
 fun refreshList() { queried.clear();loadList() }
 private fun loadList() {
  if(_uiState.value.selectedId!=null||listJob?.isActive==true)return
  val rows=_uiState.value.reservations.filter { it.status==ReservationStatus.SYNCED&&it.id !in queried }
  if(rows.isEmpty())return
  listJob=viewModelScope.launch {
   _uiState.update { it.copy(loading=true,errorMessage=null) }
   for(row in rows) { queried+=row.id
    getEvidences(row.id).fold({ evidence -> _uiState.update { it.copy(summaries=it.summaries+(row.id to evidence)) } },
      { e -> _uiState.update { it.copy(errorMessage=e.message) } })
   }
   _uiState.update { it.copy(loading=false) }
  }
 }
 init { viewModelScope.launch { combine(reservations.observeReservations(),prospects.observeProspects(),portfolio.observePortfolio()) { rows,people,p ->
  Triple(rows,people,p) }.catch { e -> _uiState.update { it.copy(errorMessage=e.message) } }.collect { (rows,people,p) ->
   _uiState.update { it.copy(reservations=rows.filter { r -> r.status!=ReservationStatus.CANCELLED&&r.status!=ReservationStatus.DRAFT },prospects=people,portfolio=p) }
   if(!requested&&_uiState.value.selected?.status==ReservationStatus.SYNCED) { requested=true;retry() };loadList()
  } } }
 fun retry() { val row=_uiState.value.selected?:return;if(_uiState.value.loading)return
  if(row.status!=ReservationStatus.SYNCED)return
  viewModelScope.launch { _uiState.update { it.copy(loading=true,errorMessage=null) }
   getEvidences(row.id).fold({ e -> _uiState.update { it.copy(loading=false,evidences=e) } },
    { e -> _uiState.update { it.copy(loading=false,errorMessage=e.message) } }) } }
}
