package com.novacorp.inmonode_app.features.fieldsales.presentation.simulation
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
@HiltViewModel
class QuickSimulationViewModel @Inject constructor(portfolio:PortfolioRepository,private val simulate:SimulateFinancingUseCase,
 private val saved:SavedStateHandle):ViewModel() {
 private val _uiState=MutableStateFlow(QuickSimulationUiState(amount=saved["amount"]?:"",months=saved["months"]?:"36"))
 val uiState=_uiState.asStateFlow()
 init { val id=saved.get<Long>("lotId")
  viewModelScope.launch { portfolio.observePortfolio().catch { e -> _uiState.update { it.copy(errorMessage=e.message) } }.collect { p ->
   val project=p.projects.firstOrNull { pr -> pr.lots.any { it.id==id } }
   _uiState.update { state ->
    val lot=project?.lots?.firstOrNull { lot -> lot.id==id }
    state.copy(project=project,lot=lot,result=if(state.lot==lot&&state.project?.financingRules==project?.financingRules)state.result else null)
   } } } }
 fun amount(v:String) { saved["amount"]=v;_uiState.update { it.copy(amount=v,result=null) } }
 fun months(v:String) { saved["months"]=v;_uiState.update { it.copy(months=v,result=null) } }
 fun calculate() { val s=_uiState.value;val lot=s.lot?:return;val project=s.project?:return
  val amount=s.amount.replace(',','.').toBigDecimalOrNull();val months=s.months.toIntOrNull()
  if(amount==null||months==null) { _uiState.update { it.copy(errorMessage="Ingresa un monto y un plazo válidos.") };return }
  simulate(lot.price,amount,months,project.financingRules).fold({ result -> _uiState.update { it.copy(result=result,errorMessage=null) } },
   { e -> _uiState.update { it.copy(errorMessage=e.message) } }) }
}
