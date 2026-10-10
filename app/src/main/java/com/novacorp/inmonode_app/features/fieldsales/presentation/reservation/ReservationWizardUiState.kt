package com.novacorp.inmonode_app.features.fieldsales.presentation.reservation
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
data class ReservationWizardUiState(val lot:Lot?=null,val project:Project?=null,val prospects:List<Prospect> = emptyList(),
 val prospectId:String?=null,val amount:String="",val reservation:Reservation?=null,val working:Boolean=false,
 val completed:Boolean=false,val errorMessage:String?=null) {
 val prospect get()=prospects.firstOrNull { it.id==(reservation?.prospectId?:prospectId) }
}
