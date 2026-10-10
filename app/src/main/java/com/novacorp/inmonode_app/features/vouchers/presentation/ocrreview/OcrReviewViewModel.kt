package com.novacorp.inmonode_app.features.vouchers.presentation.ocrreview
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.novacorp.inmonode_app.features.vouchers.domain.*
import com.novacorp.inmonode_app.features.vouchers.application.*
@HiltViewModel
class OcrReviewViewModel @Inject constructor(repository:VoucherRepository,private val process:ProcessOcrUseCase,
 private val review:ReviewVoucherUseCase,private val saved:SavedStateHandle):ViewModel() {
 private val _uiState=MutableStateFlow(OcrReviewUiState())
 val uiState=_uiState.asStateFlow()
 private var initialized=false
 init { viewModelScope.launch { repository.observeVouchers().catch { e -> _uiState.update { it.copy(errorMessage=e.message) } }
  .collect { rows -> val row=rows.firstOrNull { it.id==saved.get<String>("voucherId") }?:return@collect
   _uiState.update { it.copy(voucher=row) }
   if(!initialized) { initialized=true
    if(row.status==VoucherStatus.PENDING_OCR) process() else load(row)
   }
  } } }
 private fun load(row:Voucher) { _uiState.update { it.copy(voucher=row,amount=saved["amount"]?:row.data.amount?.toPlainString().orEmpty(),
  currency=saved["currency"]?:row.data.currency.orEmpty(),date=saved["date"]?:row.data.operationDate.orEmpty(),code=saved["code"]?:row.data.operationCode.orEmpty()) } }
 fun amount(v:String) { saved["amount"]=v;_uiState.update { it.copy(amount=v) } }
 fun currency(v:String) { saved["currency"]=v;_uiState.update { it.copy(currency=v.uppercase()) } }
 fun date(v:String) { saved["date"]=v;_uiState.update { it.copy(date=v) } }
 fun code(v:String) { saved["code"]=v;_uiState.update { it.copy(code=v) } }
 fun process() { if(_uiState.value.working)return;val id=saved.get<String>("voucherId")?:return
  viewModelScope.launch { _uiState.update { it.copy(working=true,errorMessage=null) }
   process(id).fold({ row -> load(row);_uiState.update { it.copy(working=false) } },
    { e -> _uiState.update { it.copy(working=false,errorMessage=e.message) } }) } }
 fun save() { val s=_uiState.value;val row=s.voucher?:return;if(s.working)return
  val data=row.data.copy(amount=s.amount.replace(',','.').toBigDecimalOrNull(),currency=s.currency.trim().uppercase(),operationDate=s.date.trim(),operationCode=s.code.trim())
  viewModelScope.launch { _uiState.update { it.copy(working=true,errorMessage=null) }
   review(row.id,data).fold({ v -> _uiState.update { it.copy(voucher=v,working=false,completed=true) } },
    { e -> _uiState.update { it.copy(working=false,errorMessage=e.message) } }) } }
}
