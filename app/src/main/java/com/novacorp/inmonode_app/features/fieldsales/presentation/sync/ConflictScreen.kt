package com.novacorp.inmonode_app.features.fieldsales.presentation.sync
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
import com.novacorp.inmonode_app.features.fieldsales.presentation.map.components.CadastralMapCanvas
@Composable
fun ConflictScreen(modifier:Modifier=Modifier,viewModel:SyncQueueViewModel=hiltViewModel(),onBack:()->Unit,onResolved:()->Unit) {
 val s by viewModel.uiState.collectAsStateWithLifecycle()
 var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
 var projectId by rememberSaveable { mutableStateOf<Long?>(null) }
 var expanded by remember { mutableStateOf(false) }
 val original=s.reservations.firstOrNull { it.id==s.conflictId }
 val project=s.portfolio.projects.firstOrNull { it.id==(projectId?:s.lot(original?.lotId?:0)?.projectId) }?:s.portfolio.projects.firstOrNull()
 val selected=project?.lots?.firstOrNull { it.id==selectedId&&it.isAvailable() }
 LaunchedEffect(s.replacementId) { if(s.replacementId!=null)onResolved() }
 Column(modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  TextButton(onClick=onBack) { Text(stringResource(R.string.field_back)) }
  Text(stringResource(R.string.sync_reassign),style=MaterialTheme.typography.titleLarge)
  Text(s.prospects.firstOrNull { it.id==original?.prospectId }?.fullName.orEmpty(),style=MaterialTheme.typography.titleMedium)
  Text(stringResource(R.string.sync_reassign_help),style=MaterialTheme.typography.bodySmall)
  Box {
   OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) { Text(project?.name.orEmpty()) }
   DropdownMenu(expanded,{expanded=false}) { s.portfolio.projects.forEach { p -> DropdownMenuItem(text={Text(p.name)},onClick={projectId=p.id;selectedId=null;expanded=false}) } }
  }
  CadastralMapCanvas(project?.lots.orEmpty(),{it.isAvailable()},selectedId,Modifier.weight(1f).fillMaxWidth(),onSelect={selectedId=it})
  selected?.let { Text("${it.code} · ${it.dimensions.area} m² · ${it.currency} ${it.price}") }
  FieldError(s.errorMessage)
  PrimaryButton(stringResource(R.string.field_confirm),selected!=null&&!s.working) { selected?.let { viewModel.reassign(it.id) } }
 }
}
