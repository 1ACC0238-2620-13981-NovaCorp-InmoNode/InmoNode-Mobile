package com.novacorp.inmonode_app.features.vouchers.presentation.separations
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import com.novacorp.inmonode_app.features.vouchers.domain.*
data class SeparationsUiState(val reservations:List<Reservation> = emptyList(),val prospects:List<Prospect> = emptyList(),
 val portfolio:Portfolio=Portfolio(emptyList(),null,null),val evidences:PaymentEvidences?=null,
 val summaries:Map<String,PaymentEvidences> = emptyMap(),val loading:Boolean=false,val errorMessage:String?=null,val selectedId:String?=null) {
 val selected get()=reservations.firstOrNull { it.id==selectedId }
 fun lot(id:Long)=portfolio.projects.flatMap { it.lots }.firstOrNull { it.id==id }
}
