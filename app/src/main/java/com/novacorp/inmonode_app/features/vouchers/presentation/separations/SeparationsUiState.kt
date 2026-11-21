package com.novacorp.inmonode_app.features.vouchers.presentation.separations
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import com.novacorp.inmonode_app.features.vouchers.domain.*
/** US-59: where the photo of the buyer's identity document of a separation stands. */
enum class IdentityDocumentState { PENDING, WAITING, SENT }
data class SeparationsUiState(val reservations:List<Reservation> = emptyList(),val prospects:List<Prospect> = emptyList(),
 val portfolio:Portfolio=Portfolio(emptyList(),null,null),val evidences:PaymentEvidences?=null,
 val summaries:Map<String,PaymentEvidences> = emptyMap(),val loading:Boolean=false,val errorMessage:String?=null,val selectedId:String?=null,
 val documents:Map<String,IdentityDocument> = emptyMap()) {
 val selected get()=reservations.firstOrNull { it.id==selectedId }
 fun lot(id:Long)=portfolio.projects.flatMap { it.lots }.firstOrNull { it.id==id }
 /** "Documento de identidad pendiente" until a photo is attached; null when the separation can no longer take one. */
 fun identityDocumentState(row:Reservation):IdentityDocumentState? {
  if(row.status !in ATTACHABLE) return null
  return when(documents[row.id]?.status) {
   null -> IdentityDocumentState.PENDING
   IdentityDocumentStatus.READY_TO_SYNC -> IdentityDocumentState.WAITING
   IdentityDocumentStatus.SYNCED -> IdentityDocumentState.SENT
  }
 }
 private companion object {
  /** A conflict waits for its reassignment, which takes the photo along. */
  val ATTACHABLE=setOf(ReservationStatus.PENDING_SYNC,ReservationStatus.SYNCED,ReservationStatus.FAILED)
 }
}
