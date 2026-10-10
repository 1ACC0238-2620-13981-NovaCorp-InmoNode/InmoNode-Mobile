package com.novacorp.inmonode_app.features.fieldsales.presentation.reservation.components
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.core.designsystem.components.*
import com.novacorp.inmonode_app.features.fieldsales.presentation.reservation.ReservationWizardViewModel
import com.novacorp.inmonode_app.features.fieldsales.presentation.prospects.MaritalStatusField
@Composable
fun ContractPreviewStep(modifier:Modifier=Modifier,viewModel:ReservationWizardViewModel=hiltViewModel(),readOnly:Boolean=false,onBack:()->Unit,onConfirmed:(String)->Unit) {
 val s by viewModel.uiState.collectAsStateWithLifecycle()
 var accepted by rememberSaveable { mutableStateOf(false) }
 LaunchedEffect(s.completed) { if(s.completed) s.reservation?.let { onConfirmed(it.id) } }
 FieldPage(stringResource(R.string.reservation_contract),modifier,onBack) {
  WizardStepper(4)
  if(s.reservation!=null&&s.prospect?.maritalStatus.isNullOrBlank()) {
   FieldError(stringResource(R.string.contract_missing))
   MaritalStatusField(s.prospect?.maritalStatus.orEmpty(),viewModel::completeMarital)
  }
  Text(stringResource(R.string.reservation_contract_note),style=MaterialTheme.typography.bodySmall)
  OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
   Text(stringResource(R.string.reservation_contract),style=MaterialTheme.typography.titleMedium)
   FieldValue(stringResource(R.string.field_name),s.prospect?.fullName.orEmpty())
   FieldValue(stringResource(R.string.field_document),s.prospect?.document.orEmpty())
   FieldValue(stringResource(R.string.field_marital),s.prospect?.maritalStatus.orEmpty())
   FieldValue(stringResource(R.string.nav_map),"${s.project?.name.orEmpty()} · ${s.lot?.code.orEmpty()}")
   FieldValue(stringResource(R.string.lot_area),"${s.lot?.dimensions?.area} m²")
   FieldValue(stringResource(R.string.field_amount),"${s.lot?.currency} ${s.reservation?.initialAmount}")
  } }
  if(!readOnly) Row { Checkbox(accepted,{accepted=it});Text(stringResource(R.string.contract_ack),Modifier.padding(top=12.dp)) }
  FieldError(s.errorMessage)
  if(!readOnly) PrimaryButton(stringResource(R.string.reservation_confirm),accepted&&!s.working&&!s.prospect?.maritalStatus.isNullOrBlank(),viewModel::confirm)
  if(!readOnly) TextButton(onClick={viewModel.cancel(onBack)},enabled=!s.working) { Text(stringResource(R.string.reservation_cancel_draft)) }
 }
}
