package com.novacorp.inmonode_app.features.fieldsales.presentation.reservation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.core.designsystem.components.*
import com.novacorp.inmonode_app.features.fieldsales.domain.model.NegotiationTerms
import com.novacorp.inmonode_app.features.fieldsales.presentation.reservation.components.WizardStepper
@Composable
fun ReservationWizardScreen(modifier:Modifier=Modifier,viewModel:ReservationWizardViewModel=hiltViewModel(),onBack:()->Unit,
 onNewProspect:()->Unit,onCapture:(String)->Unit) {
 val s by viewModel.uiState.collectAsStateWithLifecycle()
 LaunchedEffect(s.reservation?.id) { s.reservation?.let { onCapture(it.id) } }
 FieldPage(stringResource(R.string.reservation_title),modifier,onBack,actions={
  PrimaryButton(stringResource(R.string.field_continue),!s.working,viewModel::begin)
  Spacer(Modifier.height(16.dp))
 }) {
  WizardStepper(1)
  s.lot?.let { lot -> ElevatedCard { Column(Modifier.padding(16.dp)) {
   Text(s.project?.name.orEmpty(),style=MaterialTheme.typography.bodySmall)
   Text(lot.code,style=MaterialTheme.typography.headlineSmall);Text("${lot.dimensions.area} m² · ${lot.currency} ${lot.price}") } } }
  Text(stringResource(R.string.reservation_prospect),style=MaterialTheme.typography.titleMedium)
  FieldDropdown(stringResource(R.string.wizard_prospect),s.prospect?.fullName.orEmpty(),s.prospects,{ "${it.fullName} · ${it.document}" },{ viewModel.select(it.id) })
  TextButton(onClick=onNewProspect) { Text(stringResource(R.string.prospect_new)) }
  OutlinedTextField(s.amount,viewModel::amount,label={Text(stringResource(R.string.field_amount))},modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true,shape=RoundedCornerShape(8.dp))
  NegotiationFields(s,viewModel)
  Text(stringResource(R.string.reservation_draft_help),style=MaterialTheme.typography.bodySmall)
  FieldError(s.errorMessage)

 }
}

/** US-58: optional terms authorized by management. Only the discount changes the final price. */
@Composable
private fun NegotiationFields(s:ReservationWizardUiState,viewModel:ReservationWizardViewModel) {
 val lot=s.lot?:return
 Text(stringResource(R.string.reservation_negotiation),style=MaterialTheme.typography.titleMedium)
 OutlinedTextField(s.discount,viewModel::discount,label={Text(stringResource(R.string.reservation_discount))},modifier=Modifier.fillMaxWidth(),
  keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true,shape=RoundedCornerShape(8.dp),isError=s.discountError!=null,
  supportingText={ Text(s.discountError?:stringResource(R.string.reservation_discount_range,lot.currency,NegotiationTerms.maxDiscount(lot.price).toPlainString())) })
 s.finalPrice?.let { price -> FieldValue(stringResource(R.string.reservation_final_price),money(lot.currency,price)) }
 OutlinedTextField(s.bonus,viewModel::bonus,label={Text(stringResource(R.string.reservation_bonus))},modifier=Modifier.fillMaxWidth(),
  singleLine=true,shape=RoundedCornerShape(8.dp),
  supportingText={ Text(stringResource(R.string.reservation_text_count,s.bonus.length,NegotiationTerms.MAX_BONUS_LENGTH)) })
 OutlinedTextField(s.notes,viewModel::notes,label={Text(stringResource(R.string.reservation_notes))},modifier=Modifier.fillMaxWidth(),
  minLines=2,shape=RoundedCornerShape(8.dp),
  supportingText={ Text(stringResource(R.string.reservation_text_count,s.notes.length,NegotiationTerms.MAX_NOTES_LENGTH)) })
}
