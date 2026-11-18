package com.novacorp.inmonode_app.features.fieldsales.presentation.reservation
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import java.math.BigDecimal
data class ReservationWizardUiState(val lot:Lot?=null,val project:Project?=null,val prospects:List<Prospect> = emptyList(),
 val prospectId:String?=null,val amount:String="",val discount:String="",val bonus:String="",val notes:String="",
 val reservation:Reservation?=null,val working:Boolean=false,val completed:Boolean=false,val errorMessage:String?=null) {
 val prospect get()=prospects.firstOrNull { it.id==(reservation?.prospectId?:prospectId) }
 /** The negotiation typed in step 1 (US-58), or null while the discount is not a number. */
 val terms:NegotiationTerms? get() {
  val text=discount.replace(',','.').trim()
  val value=if(text.isEmpty()) null else text.toBigDecimalOrNull()?:return null
  return NegotiationTerms.of(value,bonus,notes)
 }
 /** Why the typed discount cannot be saved for this lot; null while it is empty or valid. */
 val discountError:String? get()=lot?.let { l -> terms?.validationError(l.price,l.currency)?:if(terms==null) INVALID_DISCOUNT else null }
 /** The price after the discount, shown only while the discount is valid. */
 val finalPrice:BigDecimal? get()=lot?.let { l -> terms?.takeIf { discountError==null }?.finalPrice(l.price) }
 companion object { const val INVALID_DISCOUNT="Ingresa un descuento válido." }
}
