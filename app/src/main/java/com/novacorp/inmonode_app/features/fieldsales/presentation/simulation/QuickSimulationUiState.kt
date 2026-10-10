package com.novacorp.inmonode_app.features.fieldsales.presentation.simulation
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import com.novacorp.inmonode_app.features.fieldsales.application.FinancingSimulation
data class QuickSimulationUiState(val lot: Lot?=null,val project:Project?=null,val amount:String="",val months:String="36",
 val result:FinancingSimulation?=null,val errorMessage:String?=null)
