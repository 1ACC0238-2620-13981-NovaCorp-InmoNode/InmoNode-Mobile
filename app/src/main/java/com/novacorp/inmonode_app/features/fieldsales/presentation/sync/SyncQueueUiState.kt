package com.novacorp.inmonode_app.features.fieldsales.presentation.sync
import com.novacorp.inmonode_app.features.fieldsales.application.BackgroundSyncState
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import com.novacorp.inmonode_app.features.fieldsales.presentation.reservation.money
import com.novacorp.inmonode_app.features.vouchers.domain.Voucher
import com.novacorp.inmonode_app.features.vouchers.domain.VoucherStatus
data class SyncQueueUiState(val reservations:List<Reservation> = emptyList(),val prospects:List<Prospect> = emptyList(),
 val vouchers:List<Voucher> = emptyList(),val portfolio:Portfolio=Portfolio(emptyList(),null,null),val offline:Boolean=true,
 val working:Boolean=false,val message:String?=null,val errorMessage:String?=null,val replacementId:String?=null,val conflictId:String?=null,
 val background:BackgroundSyncState=BackgroundSyncState.Idle) {
 fun lot(id:Long)=portfolio.projects.flatMap { it.lots }.firstOrNull { it.id==id }
 /** A lot missing from the catalog (LOT_NOT_FOUND) is named by its id. */
 fun lotLabel(id:Long)=lot(id)?.code?:"#$id"
 fun amountLabel(row:Reservation)=money(lot(row.lotId)?.currency,row.initialAmount).ifEmpty { row.initialAmount.setScale(2,java.math.RoundingMode.DOWN).toPlainString() }
 /** Records the next sync would send; a queued retry is only worth showing while some remain. */
 val hasPending:Boolean get()=reservations.any { it.status==ReservationStatus.PENDING_SYNC||it.status==ReservationStatus.FAILED }||
  prospects.any { !it.synced }||vouchers.any { it.status==VoucherStatus.READY_TO_SYNC }
 val syncing:Boolean get()=working||background==BackgroundSyncState.Running
}
