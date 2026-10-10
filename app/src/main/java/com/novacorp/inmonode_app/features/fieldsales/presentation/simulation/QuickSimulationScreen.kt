package com.novacorp.inmonode_app.features.fieldsales.presentation.simulation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.core.designsystem.components.*
@Composable
fun QuickSimulationScreen(modifier:Modifier=Modifier,viewModel:QuickSimulationViewModel=hiltViewModel(),onBack:()->Unit,onReserve:(Long)->Unit) {
 val s by viewModel.uiState.collectAsStateWithLifecycle()
 FieldPage(stringResource(R.string.simulation_title),modifier,onBack,actions={
  PrimaryButton(stringResource(R.string.lot_reserve),s.lot?.isAvailable()==true) { s.lot?.let { onReserve(it.id) } }
  Spacer(Modifier.height(16.dp))
 }) {
  s.lot?.let { lot -> Text("${lot.code} · ${s.project?.name}",style=MaterialTheme.typography.titleMedium)
   FieldValue(stringResource(R.string.lot_price),"${lot.currency} ${lot.price}") }
  OutlinedTextField(s.amount,viewModel::amount,label={Text(stringResource(R.string.field_amount))},modifier=Modifier.fillMaxWidth(),keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Decimal),singleLine=true,shape=androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
  OutlinedTextField(s.months,viewModel::months,label={Text(stringResource(R.string.simulation_months))},modifier=Modifier.fillMaxWidth(),keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Number),singleLine=true,shape=androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
  s.project?.financingRules?.let { rules ->
   FieldValue(stringResource(R.string.simulation_minimum),"${rules.minDownPaymentPercentage}%")
   FieldValue(stringResource(R.string.simulation_maximum),rules.maxTermMonths.toString())
   FieldValue(stringResource(R.string.simulation_rate),"${rules.annualInterestRate}%")
  }
  FieldError(s.errorMessage)
  PrimaryButton(stringResource(R.string.simulation_calculate),s.lot!=null,viewModel::calculate)
  s.result?.let { result -> OutlinedCard { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
   Text(stringResource(R.string.simulation_monthly));Text("${s.lot?.currency} ${result.monthlyPayment}",style=MaterialTheme.typography.headlineMedium)
   FieldValue(stringResource(R.string.simulation_financed),result.financedAmount.toPlainString())
   FieldValue(stringResource(R.string.simulation_interest),result.totalInterest.toPlainString())
  } } }

 }
}
