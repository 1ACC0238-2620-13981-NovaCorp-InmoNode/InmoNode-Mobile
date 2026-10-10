package com.novacorp.inmonode_app.features.fieldsales.presentation.reservation
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.core.designsystem.components.*
import com.novacorp.inmonode_app.features.fieldsales.presentation.reservation.components.WizardStepper
@Composable
fun ReservationWizardScreen(modifier:Modifier=Modifier,viewModel:ReservationWizardViewModel=hiltViewModel(),onBack:()->Unit,
 onNewProspect:()->Unit,onCapture:(String)->Unit) {
 val s by viewModel.uiState.collectAsStateWithLifecycle()
 LaunchedEffect(s.reservation?.id) { s.reservation?.let { onCapture(it.id) } }
 var expanded by remember { mutableStateOf(false) }
 FieldPage(stringResource(R.string.reservation_title),modifier,onBack,actions={
  PrimaryButton(stringResource(R.string.field_continue),!s.working,viewModel::begin)
  Spacer(Modifier.height(16.dp))
 }) {
  WizardStepper(1)
  s.lot?.let { lot -> ElevatedCard { Column(Modifier.padding(16.dp)) {
   Text(s.project?.name.orEmpty(),style=MaterialTheme.typography.bodySmall)
   Text(lot.code,style=MaterialTheme.typography.headlineSmall);Text("${lot.dimensions.area} m² · ${lot.currency} ${lot.price}") } } }
  Text(stringResource(R.string.reservation_prospect),style=MaterialTheme.typography.titleMedium)
  Box {
   OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) { Text(s.prospect?.fullName ?: stringResource(R.string.reservation_prospect)) }
   DropdownMenu(expanded,{expanded=false}) { s.prospects.forEach { p -> DropdownMenuItem(text={Text("${p.fullName} · ${p.document}")},onClick={viewModel.select(p.id);expanded=false}) } }
  }
  TextButton(onClick=onNewProspect) { Text(stringResource(R.string.prospect_new)) }
  OutlinedTextField(s.amount,viewModel::amount,label={Text(stringResource(R.string.field_amount))},modifier=Modifier.fillMaxWidth(),keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Decimal),singleLine=true,shape=androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
  Text(stringResource(R.string.reservation_saved_help),style=MaterialTheme.typography.bodySmall)
  FieldError(s.errorMessage)

 }
}
