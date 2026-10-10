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
  OutlinedTextField(s.amount,viewModel::amount,label={Text(stringResource(R.string.voucher_amount))},modifier=Modifier.fillMaxWidth(),keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Decimal),singleLine=true,shape=androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
  OutlinedTextField(s.currency,viewModel::currency,label={Text(stringResource(R.string.voucher_currency))},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
  OutlinedTextField(s.date,viewModel::date,label={Text(stringResource(R.string.voucher_date))},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
  OutlinedTextField(s.code,viewModel::code,label={Text(stringResource(R.string.voucher_code))},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
  FieldError(s.errorMessage)
  if(s.errorMessage!=null) TextButton(onClick=viewModel::process,enabled=!s.working) { Text(stringResource(R.string.voucher_process)) }
  OutlinedButton(onClick={s.voucher?.let { onRecapture(it.reservationId) }},modifier=Modifier.fillMaxWidth(),enabled=!s.working) { Text(stringResource(R.string.voucher_capture)) }
  PrimaryButton(stringResource(R.string.field_continue),!s.working&&s.voucher?.status!=VoucherStatus.RECAPTURE_REQUIRED&&s.voucher!=null,viewModel::save)
 }
}
