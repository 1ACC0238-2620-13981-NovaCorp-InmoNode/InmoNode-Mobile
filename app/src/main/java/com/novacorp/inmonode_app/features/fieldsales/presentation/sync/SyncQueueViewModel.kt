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
 private val resolve:ResolveConflictUseCase,private val saved:SavedStateHandle):ViewModel() {
 private val _uiState=MutableStateFlow(SyncQueueUiState(conflictId=saved["reservationId"]))
 val uiState=_uiState.asStateFlow()
 private var retryJob:Job?=null
 private var lastAutomaticKey:String?=null
 private fun automaticSync() {
  val s=_uiState.value
  val pending=s.reservations.filter { it.status in listOf(com.novacorp.inmonode_app.features.fieldsales.domain.model.ReservationStatus.PENDING_SYNC,
   com.novacorp.inmonode_app.features.fieldsales.domain.model.ReservationStatus.FAILED,com.novacorp.inmonode_app.features.fieldsales.domain.model.ReservationStatus.FAILED_VALIDATION) }.map { it.id } +
   s.prospects.filter { !it.synced }.map { it.id } + s.vouchers.filter { it.status==VoucherStatus.READY_TO_SYNC }.map { it.id }
  val key=pending.sorted().joinToString()
  if(s.offline||s.working||pending.isEmpty()||key==lastAutomaticKey)return
  lastAutomaticKey=key;synchronize()
 }
 init {
  viewModelScope.launch { combine(reservations.observeReservations(),prospects.observeProspects(),vouchers.observeVouchers(),portfolio.observePortfolio()) { r,p,v,c ->
   SyncQueueUiState(reservations=r,prospects=p,vouchers=v,portfolio=c) }
   .catch { e -> _uiState.update { it.copy(errorMessage=e.message) } }.collect { s ->
    _uiState.update { it.copy(reservations=s.reservations,prospects=s.prospects,vouchers=s.vouchers,portfolio=s.portfolio) };automaticSync() } }
  viewModelScope.launch { connectivity.isOnline.collect { online -> _uiState.update { it.copy(offline=!online) }
    if(online) { lastAutomaticKey=null;automaticSync() } else retryJob?.cancel() } }
 }
 fun synchronize() { if(_uiState.value.working||_uiState.value.offline||saved.get<String>("reservationId")!=null)return
  retryJob?.cancel()
  viewModelScope.launch { _uiState.update { it.copy(working=true,errorMessage=null,message=null) }
   sync().fold({ summary -> _uiState.update { it.copy(working=false,message="${summary.prospectsSynced} prospectos y ${summary.reservationsSynced} separaciones enviados · ${summary.conflicts} conflictos") } },
    { e -> _uiState.update { it.copy(working=false,errorMessage=e.message) }
      if(e is java.io.IOException) retryJob=viewModelScope.launch { delay(60_000);if(!_uiState.value.offline)synchronize() }
    }) }
 }
 fun reassign(lotId:Long) { val id=saved.get<String>("reservationId")?:return;if(_uiState.value.working)return
  viewModelScope.launch { _uiState.update { it.copy(working=true,errorMessage=null) }
   resolve(id,lotId).fold({ row -> _uiState.update { it.copy(working=false,replacementId=row.id) } },
    { e -> _uiState.update { it.copy(working=false,errorMessage=e.message) } }) } }
}
