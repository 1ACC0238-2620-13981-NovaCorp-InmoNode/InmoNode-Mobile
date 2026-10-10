package com.novacorp.inmonode_app.features.fieldsales.presentation.prospects
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
fun ProspectFormScreen(modifier:Modifier=Modifier,viewModel:ProspectsViewModel=hiltViewModel(),onBack:()->Unit,onSaved:(String)->Unit) {
 val s by viewModel.uiState.collectAsStateWithLifecycle()
 LaunchedEffect(s.savedId) { s.savedId?.let(onSaved) }
 FieldPage(stringResource(R.string.prospect_new),modifier,onBack,actions={
  PrimaryButton(stringResource(R.string.field_save),!s.saving,viewModel::save)
  Spacer(Modifier.height(16.dp))
 }) {
  OfflineBanner(s.offline)
  OutlinedTextField(s.name,viewModel::name,label={Text(stringResource(R.string.field_name))},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
  OutlinedTextField(s.document,viewModel::document,label={Text(stringResource(R.string.field_document))},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
  OutlinedTextField(s.phone,viewModel::phone,label={Text(stringResource(R.string.field_phone))},modifier=Modifier.fillMaxWidth(),keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Phone),singleLine=true,shape=androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
  MaritalStatusField(s.maritalStatus,viewModel::marital)
  FieldError(s.errorMessage)

 }
}
@Composable
fun MaritalStatusField(value:String,onChange:(String)->Unit) {
 var expanded by remember { mutableStateOf(false) }
 Box {
  OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) { Text(value.ifBlank { stringResource(R.string.field_marital) }) }
  DropdownMenu(expanded,{expanded=false}) { listOf("Soltero(a)","Casado(a)","Divorciado(a)","Viudo(a)").forEach { label ->
   DropdownMenuItem(text={Text(label)},onClick={onChange(label);expanded=false}) } }
 }
}
