package com.novacorp.inmonode_app.features.vouchers.presentation.ocrreview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.core.designsystem.components.*
import com.novacorp.inmonode_app.features.fieldsales.presentation.reservation.components.WizardStepper
import com.novacorp.inmonode_app.features.vouchers.domain.OcrFieldError
import com.novacorp.inmonode_app.features.vouchers.domain.VoucherStatus
import java.io.File
@Composable
fun OcrReviewScreen(modifier:Modifier=Modifier,viewModel:OcrReviewViewModel=hiltViewModel(),onBack:()->Unit,
 onRecapture:(String)->Unit,onReviewed:(String)->Unit) {
 val s by viewModel.uiState.collectAsStateWithLifecycle()
 LaunchedEffect(s.completed) { if(s.completed)s.voucher?.let { onReviewed(it.reservationId) } }
 FieldPage(stringResource(R.string.voucher_review),modifier,onBack) {
  WizardStepper(3)
  s.voucher?.let { AsyncImage(File(it.imagePath),contentDescription=stringResource(R.string.voucher_image),modifier=Modifier.fillMaxWidth().height(160.dp)) }
  if(s.working||s.voucher==null) LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
  if(s.voucher?.status==VoucherStatus.RECAPTURE_REQUIRED) FieldError(stringResource(R.string.voucher_recapture))
  Text(stringResource(R.string.voucher_review_hint))
  s.voucher?.data?.confidence?.let { confidence -> Text(stringResource(R.string.voucher_confidence,confidence.movePointRight(2).toInt()),style=MaterialTheme.typography.labelLarge) }
  if(s.voucher?.manuallyCorrected==true)Text(stringResource(R.string.voucher_manual),style=MaterialTheme.typography.bodySmall)
  val errors=s.fieldErrors
  OutlinedTextField(s.amount,viewModel::amount,label={Text(stringResource(R.string.voucher_amount))},modifier=Modifier.fillMaxWidth(),keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Decimal),singleLine=true,shape=androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
   isError=errors.any { it==OcrFieldError.AMOUNT||it==OcrFieldError.AMOUNT_DECIMALS },supportingText=errors.messageFor(OcrFieldError.AMOUNT,OcrFieldError.AMOUNT_DECIMALS))
  OutlinedTextField(s.currency,viewModel::currency,label={Text(stringResource(R.string.voucher_currency))},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
   isError=OcrFieldError.CURRENCY in errors,supportingText=errors.messageFor(OcrFieldError.CURRENCY))
  OutlinedTextField(s.date,viewModel::date,label={Text(stringResource(R.string.voucher_date))},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
   isError=errors.any { it==OcrFieldError.DATE_FORMAT||it==OcrFieldError.DATE_FUTURE },supportingText=errors.messageFor(OcrFieldError.DATE_FORMAT,OcrFieldError.DATE_FUTURE))
  OutlinedTextField(s.code,viewModel::code,label={Text(stringResource(R.string.voucher_code))},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
   isError=errors.any { it==OcrFieldError.CODE_MISSING||it==OcrFieldError.CODE_TOO_LONG },supportingText=errors.messageFor(OcrFieldError.CODE_MISSING,OcrFieldError.CODE_TOO_LONG))
  FieldError(s.errorMessage)
  if(s.errorMessage!=null) TextButton(onClick=viewModel::process,enabled=!s.working) { Text(stringResource(R.string.voucher_process)) }
  SecondaryButton(stringResource(R.string.voucher_capture),!s.working) { s.voucher?.let { onRecapture(it.reservationId) } }
  PrimaryButton(stringResource(R.string.field_continue),!s.working&&s.voucher?.status!=VoucherStatus.RECAPTURE_REQUIRED&&s.voucher!=null,viewModel::save)
 }
}

/** The hint under a field the agent must fix, or none (US-10). */
private fun Set<OcrFieldError>.messageFor(vararg fields:OcrFieldError):(@Composable ()->Unit)? {
 val error=firstOrNull { it in fields }?:return null
 val text=when(error) {
  OcrFieldError.AMOUNT->R.string.voucher_error_amount
  OcrFieldError.AMOUNT_DECIMALS->R.string.voucher_error_amount_decimals
  OcrFieldError.CURRENCY->R.string.voucher_error_currency
  OcrFieldError.DATE_FORMAT->R.string.voucher_error_date_format
  OcrFieldError.DATE_FUTURE->R.string.voucher_error_date_future
  OcrFieldError.CODE_MISSING->R.string.voucher_error_code_missing
  OcrFieldError.CODE_TOO_LONG->R.string.voucher_error_code_long
 }
 return { Text(stringResource(text)) }
}
