package com.novacorp.inmonode_app.features.vouchers.presentation.separations
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
fun SeparationListScreen(modifier:Modifier=Modifier,viewModel:SeparationsViewModel=hiltViewModel(),onProspects:()->Unit,onDetail:(String)->Unit) {
 val s by viewModel.uiState.collectAsStateWithLifecycle()
 Column(modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Text(stringResource(R.string.prospects_title),style=MaterialTheme.typography.headlineMedium)
  Row { TextButton(onClick=onProspects) { Text(stringResource(R.string.nav_prospects)) }
   TextButton(onClick={}) { Text(stringResource(R.string.separations_title)) } }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
   TextButton(onClick=viewModel::refreshList,enabled=!s.loading) { Text(stringResource(R.string.field_refresh)) }
   if(s.loading)CircularProgressIndicator(modifier=Modifier.size(24.dp))
  }
  FieldError(s.errorMessage)
  LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
   if(s.reservations.isEmpty())item { Text(stringResource(R.string.field_empty)) }
   items(s.reservations,key={it.id}) { row -> OutlinedCard(onClick={onDetail(row.id)},modifier=Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     Text(s.lot(row.lotId)?.code?:row.lotId.toString(),style=MaterialTheme.typography.headlineSmall)
     Text("${s.prospects.firstOrNull { it.id==row.prospectId }?.fullName.orEmpty()} · ${row.initialAmount}")
     StatusChip(s.summaries[row.id]?.status?:row.serverStatus?:row.status.name)
     (s.summaries[row.id]?.waitingUntil?:row.blockedUntil)?.let { Text(displayTimestamp(it),style=MaterialTheme.typography.bodySmall) }
    } } }
  }
 }
}
