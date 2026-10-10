package com.novacorp.inmonode_app.features.fieldsales.presentation.reservation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.core.designsystem.components.*
@Composable
fun ReservationConfirmationScreen(modifier:Modifier=Modifier,viewModel:ReservationWizardViewModel=hiltViewModel(),onMap:()->Unit,onSync:()->Unit) {
 val s by viewModel.uiState.collectAsStateWithLifecycle()
 FieldPage(stringResource(R.string.reservation_saved),modifier) {
  Text(stringResource(R.string.reservation_saved_help))
  FieldValue(stringResource(R.string.field_name),s.prospect?.fullName.orEmpty())
  FieldValue(stringResource(R.string.nav_map),s.lot?.code.orEmpty())
  FieldValue(stringResource(R.string.field_amount),s.reservation?.initialAmount?.toPlainString().orEmpty())
  PrimaryButton(stringResource(R.string.nav_sync),onClick=onSync)
  PrimaryButton(stringResource(R.string.nav_map),onClick=onMap)
 }
}
