package com.novacorp.inmonode_app.features.vouchers.presentation.ocrreview
import com.novacorp.inmonode_app.features.vouchers.domain.OcrData
import com.novacorp.inmonode_app.features.vouchers.domain.OcrFieldError
import com.novacorp.inmonode_app.features.vouchers.domain.Voucher
data class OcrReviewUiState(val voucher:Voucher?=null,val amount:String="",val currency:String="",val date:String="",val code:String="",
 val working:Boolean=false,val completed:Boolean=false,val errorMessage:String?=null,val checked:Boolean=false) {
 /** What the agent typed, over the OCR reading; an amount that is not a number stays empty. */
 fun typed(reading:OcrData=OcrData())=reading.copy(amount=amount.trim().replace(',','.').toBigDecimalOrNull(),
  currency=currency.trim().uppercase(),operationDate=date.trim(),operationCode=code.trim())
 /** Fields to fix, shown once the agent tries to continue or when the OCR left something out (US-10). */
 val fieldErrors:Set<OcrFieldError> get()=if(checked) typed().fieldErrors() else emptySet()
}
