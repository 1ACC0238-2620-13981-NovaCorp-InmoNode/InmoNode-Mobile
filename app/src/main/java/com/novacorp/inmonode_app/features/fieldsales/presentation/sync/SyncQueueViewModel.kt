package com.novacorp.inmonode_app.features.fieldsales.presentation.sync
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.novacorp.inmonode_app.core.connectivity.ConnectivityMonitor
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.*
import com.novacorp.inmonode_app.features.vouchers.domain.*
import com.novacorp.inmonode_app.features.fieldsales.application.*
@HiltViewModel
class SyncQueueViewModel @Inject constructor(reservations:ReservationRepository,prospects:ProspectRepository,vouchers:VoucherRepository,
 portfolio:PortfolioRepository,connectivity:ConnectivityMonitor,private val sync:SyncFieldRecordsUseCase,
 private val background:BackgroundSync,private val resolve:ResolveConflictUseCase,private val saved:SavedStateHandle):ViewModel() {
 private val _uiState=MutableStateFlow(SyncQueueUiState(conflictId=saved["reservationId"]))
 val uiState=_uiState.asStateFlow()
 init {
  viewModelScope.launch { combine(reservations.observeReservations(),prospects.observeProspects(),vouchers.observeVouchers(),portfolio.observePortfolio()) { r,p,v,c ->
   SyncQueueUiState(reservations=r,prospects=p,vouchers=v,portfolio=c) }
   .catch { e -> _uiState.update { it.copy(errorMessage=e.message) } }.collect { s ->
    _uiState.update { it.copy(reservations=s.reservations,prospects=s.prospects,vouchers=s.vouchers,portfolio=s.portfolio) } } }
  viewModelScope.launch { connectivity.isOnline.collect { online -> _uiState.update { it.copy(offline=!online) } } }
  // Records are sent automatically by the background sync (US-11), also with this screen closed; it only shows its progress.
  viewModelScope.launch { background.state.catch { }.collect { b -> _uiState.update { it.copy(background=b) } } }
 }
 fun synchronize() { val s=_uiState.value
  if(s.syncing||s.offline||saved.get<String>("reservationId")!=null)return
  viewModelScope.launch { _uiState.update { it.copy(working=true,errorMessage=null,result=null) }
   sync().fold({ summary -> _uiState.update { it.copy(working=false,result=summary) } },
    { e -> _uiState.update { it.copy(working=false,errorMessage=e.message) }
      if(isRetryableSyncFailure(e)) background.schedule()
    }) }
 }
 fun reassign(lotId:Long) { val id=saved.get<String>("reservationId")?:return;if(_uiState.value.working)return
  viewModelScope.launch { _uiState.update { it.copy(working=true,errorMessage=null) }
   resolve(id,lotId).fold({ row -> _uiState.update { it.copy(working=false,replacementId=row.id) } },
    { e -> _uiState.update { it.copy(working=false,errorMessage=e.message) } }) } }
}
