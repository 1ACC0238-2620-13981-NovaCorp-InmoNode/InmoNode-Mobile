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
import androidx.compose.ui.platform.LocalUriHandler
import java.time.Instant
@Composable
fun SeparationDetailScreen(modifier:Modifier=Modifier,viewModel:SeparationsViewModel=hiltViewModel(),onBack:()->Unit,onReplace:(String)->Unit,onContract:(String)->Unit) {
 val s by viewModel.uiState.collectAsStateWithLifecycle()
 val uri=LocalUriHandler.current
 var now by remember { mutableStateOf(Instant.now()) }
 LaunchedEffect(s.evidences?.waitingUntil) {
  while(true) { now=Instant.now();kotlinx.coroutines.delay(1000) }
 }
 FieldPage(stringResource(R.string.separation_detail),modifier,onBack) {
  s.selected?.let { row ->
   Text(s.lot(row.lotId)?.code.orEmpty(),style=MaterialTheme.typography.headlineSmall)
   FieldValue(stringResource(R.string.field_name),s.prospects.firstOrNull { it.id==row.prospectId }?.fullName.orEmpty())
   FieldValue(stringResource(R.string.field_amount),row.initialAmount.toPlainString())
   StatusChip(s.evidences?.status?:row.serverStatus?:row.status.name)
   (s.evidences?.waitingUntil?:row.blockedUntil)?.let { until ->
    val remaining=runCatching { java.time.Duration.between(now,Instant.parse(until)).seconds.coerceAtLeast(0) }.getOrNull()
    remaining?.let { seconds ->
     Text("%02d:%02d:%02d".format(seconds/3600,(seconds%3600)/60,seconds%60),style=MaterialTheme.typography.headlineMedium)
    }
    Text(displayTimestamp(until))
   }
   Text(stringResource(R.string.separation_registered),style=MaterialTheme.typography.titleMedium)
   Text(displayTimestamp(row.reservedAt),style=MaterialTheme.typography.bodySmall)
   OutlinedButton(onClick={onContract(row.id)},modifier=Modifier.fillMaxWidth()) { Text(stringResource(R.string.reservation_contract)) }
   if(s.loading)LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
   FieldError(s.errorMessage)
   if(s.errorMessage!=null)PrimaryButton(stringResource(R.string.field_retry),onClick=viewModel::retry)
   s.evidences?.let { result ->
    if(result.isCached)Text(stringResource(R.string.separation_cached))
    Text(stringResource(R.string.separation_evidences),style=MaterialTheme.typography.titleMedium)
    if(result.evidences.isEmpty())Text(stringResource(R.string.field_empty))
    result.evidences.forEach { evidence -> OutlinedCard(Modifier.fillMaxWidth()) {
     Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
      Text("${evidence.currency} ${evidence.amount} · ${evidence.operationCode}",style=MaterialTheme.typography.titleSmall)
      Text(evidence.operationDate);StatusChip(evidence.status);evidence.rejectionReason?.let { FieldError(it) }
      if(evidence.downloadUrl!=null&&evidence.downloadExpiresAt?.let { runCatching { Instant.parse(it)>Instant.now() }.getOrDefault(false) }==true)
       TextButton(onClick={uri.openUri(evidence.downloadUrl)}) { Text(stringResource(R.string.separation_download)) }
     } } }
    if(result.status=="REJECTED"&&result.waitingUntil?.let { runCatching { Instant.parse(it)>Instant.now() }.getOrDefault(false) }==true)
     PrimaryButton(stringResource(R.string.separation_replace)) { onReplace(row.id) }
   }
  }?:Text(stringResource(R.string.field_empty))
 }
}
