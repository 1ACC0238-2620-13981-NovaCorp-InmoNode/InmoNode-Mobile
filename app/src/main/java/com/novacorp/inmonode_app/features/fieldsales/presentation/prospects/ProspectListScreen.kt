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
fun ProspectListScreen(modifier: Modifier=Modifier,viewModel: ProspectsViewModel=hiltViewModel(),newProspectId:String?=null,onNew:()->Unit,onSeparations:()->Unit) {
 val s by viewModel.uiState.collectAsStateWithLifecycle()
 Column(modifier.fillMaxSize()) {
  OfflineBanner(s.offline)
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text(stringResource(R.string.prospects_title),style=MaterialTheme.typography.headlineMedium)
   Row { TextButton(onClick={}) { Text(stringResource(R.string.nav_prospects)) }
    TextButton(onClick=onSeparations) { Text(stringResource(R.string.separations_title)) } }
   OutlinedTextField(s.query,viewModel::query,label={Text(stringResource(R.string.field_search))},modifier=Modifier.fillMaxWidth(),singleLine=true,shape=androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
   FieldError(s.errorMessage)
  }
  LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   if(!s.loaded)item { CircularProgressIndicator() }
   if(s.loaded&&s.filtered.isEmpty()) item { Text(stringResource(R.string.prospect_empty)) }
   items(s.filtered,key={it.id}) { p -> OutlinedCard(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
     Text(p.fullName,style=MaterialTheme.typography.titleMedium); Text("${p.document} · ${p.phone}",style=MaterialTheme.typography.bodySmall)
     Text(stringResource(if(p.synced)R.string.prospect_synced else R.string.prospect_pending),style=MaterialTheme.typography.labelMedium)
    } } }
  }
  if(newProspectId!=null) Surface(color=MaterialTheme.colorScheme.primaryContainer) { Text(stringResource(R.string.prospect_saved_help),modifier=Modifier.padding(16.dp)) }
  Box(Modifier.padding(16.dp)) { PrimaryButton(stringResource(R.string.prospect_new),onClick=onNew) }
 }
}
