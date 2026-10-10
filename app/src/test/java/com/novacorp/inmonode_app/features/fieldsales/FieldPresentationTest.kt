package com.novacorp.inmonode_app.features.fieldsales
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.novacorp.inmonode_app.features.fieldsales.application.*
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import com.novacorp.inmonode_app.features.fieldsales.domain.repositories.*
import com.novacorp.inmonode_app.features.fieldsales.presentation.reservation.ReservationWizardViewModel
import com.novacorp.inmonode_app.features.fieldsales.presentation.simulation.QuickSimulationViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class FieldPresentationTest {
 private val lot=Lot(1,1,"A-1",LotDimensions(BigDecimal.TEN,BigDecimal.TEN,BigDecimal("100")),emptyList(),BigDecimal("10000"),"PEN",LotStatus.AVAILABLE)
 private val project=Project(1,"Proyecto","Lima",null,null,null,FinancingRules(BigDecimal("20"),BigDecimal.ZERO,120,BigDecimal.ZERO),listOf(lot))
 private val person=Prospect("person","12345678","Nombre","","Soltero(a)","2026-10-01T00:00:00.000Z")
 private inner class PortfolioFake:PortfolioRepository {
  val state=MutableStateFlow(Portfolio(listOf(project),"2026-10-01",null))
  override fun observePortfolio():Flow<Portfolio> = state
  override suspend fun download()=Result.success(state.value)
 }
 private inner class PeopleFake:ProspectRepository {
  override fun observeProspects()=flowOf(listOf(person))
  override suspend fun register(document:String,fullName:String,phone:String,maritalStatus:String?)=Result.success(person)
  override suspend fun completeMaritalStatus(prospectId:String,maritalStatus:String)=Result.success(person)
 }
 private inner class ReservationsFake:ReservationRepository {
  val state=MutableStateFlow<List<Reservation>>(emptyList());var calls=0
  var gate:CompletableDeferred<Unit>?=null
  override fun observeReservations():Flow<List<Reservation>> = state
  override suspend fun createDraft(lotId:Long,prospectId:String,initialAmount:BigDecimal):Result<Reservation> {
   calls++;gate?.await();val row=Reservation("draft",lotId,prospectId,initialAmount,"2026-10-01",ReservationStatus.DRAFT)
   state.value=listOf(row);return Result.success(row)
  }
  override suspend fun confirmDraft(reservationId:String):Result<Reservation> = Result.failure(IllegalStateException("Voucher pendiente"))
  override suspend fun cancelDraft(reservationId:String)=Result.success(Unit)
  override suspend fun reassign(reservationId:String,newLotId:Long):Result<Reservation> = Result.failure(IllegalStateException())
 }
 private fun wizard(p:PortfolioRepository,people:ProspectRepository,rows:ReservationRepository,saved:SavedStateHandle)=
  ReservationWizardViewModel(p,people,rows,BeginReservationUseCase(rows),ReserveLotUseCase(rows),CancelReservationDraftUseCase(rows),CompleteProspectUseCase(people),saved)
 @Test fun doubleTapCreatesOnlyOneDurableDraft()=runTest {
  Dispatchers.setMain(StandardTestDispatcher(testScheduler))
  try { val rows=ReservationsFake().apply { gate=CompletableDeferred() }
   val vm=wizard(PortfolioFake(),PeopleFake(),rows,SavedStateHandle(mapOf("lotId" to 1L)))
   runCurrent();vm.select(person.id);vm.amount("500");vm.begin();runCurrent();vm.begin();runCurrent()
   assertEquals(1,rows.calls);rows.gate!!.complete(Unit);runCurrent()
   assertEquals("draft",vm.uiState.value.reservation?.id);assertFalse(vm.uiState.value.working);vm.viewModelScope.cancel()
  } finally { Dispatchers.resetMain() }
 }
 @Test fun failedConfirmationKeepsDraftAndDoesNotNavigateToSuccess()=runTest {
  Dispatchers.setMain(StandardTestDispatcher(testScheduler))
  try { val rows=ReservationsFake();val saved=SavedStateHandle(mapOf("lotId" to 1L))
   val vm=wizard(PortfolioFake(),PeopleFake(),rows,saved)
   runCurrent();vm.select(person.id);vm.amount("500");vm.begin();runCurrent();vm.confirm();runCurrent()
   assertEquals(ReservationStatus.DRAFT,vm.uiState.value.reservation?.status)
   assertFalse(vm.uiState.value.completed);assertEquals("Voucher pendiente",vm.uiState.value.errorMessage)
   assertEquals("draft",saved.get<String>("reservationId"));vm.viewModelScope.cancel()
  } finally { Dispatchers.resetMain() }
 }
 @Test fun recreatedWizardRestoresSelectedProspectAndAmount()=runTest {
  Dispatchers.setMain(StandardTestDispatcher(testScheduler))
  try { val vm=wizard(PortfolioFake(),PeopleFake(),ReservationsFake(),SavedStateHandle(mapOf("lotId" to 1L,"amount" to "900","prospectId" to "person")))
   runCurrent();assertEquals("900",vm.uiState.value.amount);assertEquals(person,vm.uiState.value.prospect);vm.viewModelScope.cancel()
  } finally { Dispatchers.resetMain() }
 }
 @Test fun removedLotInvalidatesDisplayedSimulation()=runTest {
  Dispatchers.setMain(StandardTestDispatcher(testScheduler))
  try { val portfolio=PortfolioFake();val vm=QuickSimulationViewModel(portfolio,SimulateFinancingUseCase(),SavedStateHandle(mapOf("lotId" to 1L)))
   runCurrent();vm.amount("2000");vm.months("24");vm.calculate();assertNotNull(vm.uiState.value.result)
   portfolio.state.value=Portfolio(emptyList(),"2026-10-02",null);runCurrent()
   assertNull(vm.uiState.value.lot);assertNull(vm.uiState.value.result);vm.viewModelScope.cancel()
  } finally { Dispatchers.resetMain() }
 }
}
