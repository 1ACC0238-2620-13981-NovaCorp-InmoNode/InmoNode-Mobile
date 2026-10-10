package com.novacorp.inmonode_app.features.fieldsales.presentation.sync
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import com.novacorp.inmonode_app.features.vouchers.domain.Voucher
data class SyncQueueUiState(val reservations:List<Reservation> = emptyList(),val prospects:List<Prospect> = emptyList(),
 val vouchers:List<Voucher> = emptyList(),val portfolio:Portfolio=Portfolio(emptyList(),null,null),val offline:Boolean=true,
 val working:Boolean=false,val message:String?=null,val errorMessage:String?=null,val replacementId:String?=null,val conflictId:String?=null) {
 fun lot(id:Long)=portfolio.projects.flatMap { it.lots }.firstOrNull { it.id==id }
}
