package com.novacorp.inmonode_app.features.vouchers.presentation.ocrreview
import com.novacorp.inmonode_app.features.vouchers.domain.Voucher
data class OcrReviewUiState(val voucher:Voucher?=null,val amount:String="",val currency:String="",val date:String="",val code:String="",
 val working:Boolean=false,val completed:Boolean=false,val errorMessage:String?=null)
