package com.novacorp.inmonode_app.core.designsystem.components
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.novacorp.inmonode_app.R
@Composable
fun StatusChip(status:String) {
 val label=when(status) {
  "DRAFT"->R.string.reservation_draft
  "SYNCED"->R.string.reservation_synced
  "CONFLICT"->R.string.reservation_conflict
  "PENDING_VERIFICATION"->R.string.status_verifying
  "VERIFIED","APPROVED"->R.string.status_verified
  "REJECTED"->R.string.status_rejected
  "EXPIRED"->R.string.status_expired
  "CANCELLED"->R.string.status_cancelled
  "FAILED","FAILED_VALIDATION"->R.string.status_failed
  "PENDING_OCR"->R.string.status_ocr_pending
  "RECAPTURE_REQUIRED"->R.string.status_recapture
  "EXTRACTED","MANUAL_REVIEW_NEEDED"->R.string.status_review
  "READY_TO_SYNC","PENDING_SYNC"->R.string.prospect_pending
  else->R.string.reservation_pending
 }
 val error=status in listOf("CONFLICT","REJECTED","EXPIRED","FAILED","FAILED_VALIDATION","RECAPTURE_REQUIRED")
 val success=status in listOf("SYNCED","VERIFIED","APPROVED")
 Surface(shape=RoundedCornerShape(16.dp),contentColor=when { error->MaterialTheme.colorScheme.onError;success->MaterialTheme.colorScheme.onPrimary;else->MaterialTheme.colorScheme.onTertiary },color=when {error->MaterialTheme.colorScheme.error
  success->MaterialTheme.colorScheme.primary;else->MaterialTheme.colorScheme.tertiary}) {
  Text(stringResource(label),style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(horizontal=12.dp,vertical=6.dp))
 }
}