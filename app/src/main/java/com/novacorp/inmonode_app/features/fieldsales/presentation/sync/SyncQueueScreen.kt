package com.novacorp.inmonode_app.features.fieldsales.presentation.sync
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.core.designsystem.components.*
import com.novacorp.inmonode_app.features.fieldsales.application.BackgroundSyncState
import com.novacorp.inmonode_app.features.fieldsales.domain.model.ReservationStatus
import com.novacorp.inmonode_app.features.fieldsales.domain.model.SyncSummary
import com.novacorp.inmonode_app.features.vouchers.domain.VoucherStatus
@Composable
fun SyncQueueScreen(modifier:Modifier=Modifier,viewModel:SyncQueueViewModel=hiltViewModel(),onConflict:(String)->Unit,onDraft:(String)->Unit) {
 val s by viewModel.uiState.collectAsStateWithLifecycle()
 val queueDescription=stringResource(R.string.sync_queue_title)
 Column(modifier.fillMaxSize()) {
  OfflineBanner(s.offline)
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text(stringResource(R.string.nav_sync),style=MaterialTheme.typography.headlineMedium,modifier=Modifier.semantics { heading();contentDescription=queueDescription })
   if(s.syncing) LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
   BackgroundSyncStatus(s)
   s.result?.let { Text(syncResultText(it)) };FieldError(s.errorMessage)
  }
  val rows=s.reservations.filter { it.status!=ReservationStatus.CANCELLED }.sortedBy { it.reservedAt }
  LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   if(rows.isEmpty()&&s.prospects.none { !it.synced }&&s.vouchers.none { it.status!=VoucherStatus.SYNCED }) item { Text(stringResource(R.string.sync_empty)) }
   items(rows,key={"reservation-"+it.id}) { row -> OutlinedCard(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
     Text("${stringResource(R.string.reservation_title)} · ${s.lot(row.lotId)?.code?:row.lotId}",style=MaterialTheme.typography.titleMedium)
     Text(s.prospects.firstOrNull { it.id==row.prospectId }?.fullName.orEmpty())
     Text(displayTimestamp(row.reservedAt),style=MaterialTheme.typography.bodySmall)
     Text(stringResource(when(row.status) {
      ReservationStatus.DRAFT->R.string.reservation_draft
      ReservationStatus.CONFLICT->R.string.reservation_conflict
      ReservationStatus.SYNCED->R.string.reservation_synced
      else->R.string.reservation_pending }))
     row.blockedUntil?.let { Text(stringResource(R.string.lot_held_until,displayTimestamp(it)),style=MaterialTheme.typography.bodySmall) }
     if(row.awaitsReassignment) TextButton(onClick={onConflict(row.id)}) { Text(stringResource(R.string.sync_conflicts)) }
     if(row.status==ReservationStatus.DRAFT) TextButton(onClick={onDraft(row.id)}) { Text(stringResource(R.string.field_continue)) }
    } } }
   items(s.prospects.filter { !it.synced }.sortedBy { it.registeredAt },key={"prospect-"+it.id}) { p -> OutlinedCard {
    Column(Modifier.padding(16.dp)) { Text(p.fullName,style=MaterialTheme.typography.titleMedium);Text(stringResource(R.string.prospect_pending)) } } }
   items(s.vouchers.filter { it.status!=VoucherStatus.SYNCED }.sortedBy { it.capturedAt },key={"voucher-"+it.id}) { v -> OutlinedCard {
    Column(Modifier.padding(16.dp)) { Text(stringResource(R.string.wizard_voucher),style=MaterialTheme.typography.titleMedium)
     // A voucher of a separation that lost its lot waits for the new lot instead of the next sync (US-12).
     if(s.reservations.firstOrNull { it.id==v.reservationId }?.awaitsReassignment==true) Text(stringResource(R.string.voucher_awaits_lot))
     else StatusChip(v.status.name)
     FieldError(v.errorMessage) } } }
  }
  Box(Modifier.padding(16.dp)) { PrimaryButton(stringResource(R.string.sync_start),!s.syncing&&!s.offline,viewModel::synchronize) }
 }
}

/** What the automatic sync of US-11 is doing, so the agent knows pending records will leave by themselves. */
@Composable
private fun BackgroundSyncStatus(s:SyncQueueUiState) {
 if(!s.hasPending||s.working)return
 val text=when(val b=s.background) {
  BackgroundSyncState.Idle->return
  BackgroundSyncState.Running->stringResource(R.string.sync_background_running)
  is BackgroundSyncState.RetryScheduled->stringResource(R.string.sync_background_retry,b.attempts)
  BackgroundSyncState.WaitingForNetwork->stringResource(R.string.sync_background_waiting)
 }
 Text(text,style=MaterialTheme.typography.bodyMedium)
}

/**
 * What "Sincronizar ahora" sent, vouchers and identity documents included. When the automatic sync had already sent everything, it says so
 * instead of reporting zero records sent.
 */
@Composable
private fun syncResultText(r:SyncSummary):String {
 if(r.sentNothing) return stringResource(R.string.sync_result_nothing)
 val sent=listOfNotNull(
  r.prospectsSynced.takeIf { it>0 }?.let { pluralStringResource(R.plurals.sync_result_prospects,it,it) },
  r.reservationsSynced.takeIf { it>0 }?.let { pluralStringResource(R.plurals.sync_result_reservations,it,it) },
  r.vouchersUploaded.takeIf { it>0 }?.let { pluralStringResource(R.plurals.sync_result_vouchers,it,it) },
  r.identityDocumentsUploaded.takeIf { it>0 }?.let { pluralStringResource(R.plurals.sync_result_identity_documents,it,it) })
 val and=stringResource(R.string.sync_result_and)
 val list=if(sent.size<2) sent.joinToString() else sent.dropLast(1).joinToString(", ")+" $and "+sent.last()
 return listOfNotNull(list.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.sync_result_sent,it) },
  r.conflicts.takeIf { it>0 }?.let { pluralStringResource(R.plurals.sync_result_conflicts,it,it) }).joinToString(" ")
}
