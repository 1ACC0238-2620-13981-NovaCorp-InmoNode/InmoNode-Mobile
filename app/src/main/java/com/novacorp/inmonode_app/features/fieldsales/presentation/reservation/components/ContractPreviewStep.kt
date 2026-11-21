package com.novacorp.inmonode_app.features.fieldsales.presentation.reservation.components
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.core.designsystem.components.*
import com.novacorp.inmonode_app.features.fieldsales.application.ContractPreview
import com.novacorp.inmonode_app.features.fieldsales.domain.model.ContractField
import com.novacorp.inmonode_app.features.fieldsales.domain.model.NegotiationTerms
import com.novacorp.inmonode_app.features.fieldsales.presentation.reservation.ReservationWizardViewModel
import com.novacorp.inmonode_app.features.fieldsales.presentation.reservation.money
import com.novacorp.inmonode_app.features.fieldsales.presentation.prospects.MaritalStatusField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
@Composable
fun ContractPreviewStep(modifier:Modifier=Modifier,viewModel:ReservationWizardViewModel=hiltViewModel(),readOnly:Boolean=false,onBack:()->Unit,onConfirmed:(String)->Unit) {
 val s by viewModel.uiState.collectAsStateWithLifecycle()
 var accepted by rememberSaveable { mutableStateOf(false) }
 // The agent chose "Completar dato" in the missing-data dialog (M22), so the field is completed on this page.
 var completing by rememberSaveable { mutableStateOf(false) }
 val context=LocalContext.current
 LaunchedEffect(Unit) { viewModel.previewContract() }
 LaunchedEffect(s.completed) { if(s.completed) s.reservation?.let { onConfirmed(it.id) } }
 val ready=s.contract as? ContractPreview.Ready
 val missing=(s.contract as? ContractPreview.Missing)?.fields.orEmpty()
 val onlyMarital=missing==listOf(ContractField.MARITAL_STATUS)
 val shareTitle=stringResource(R.string.contract_share)
 FieldPage(stringResource(R.string.reservation_contract),modifier,onBack,topActions={
  if(ready!=null) IconButton(onClick={ shareContract(context,ready.pdf.file,shareTitle) }) {
   Icon(painterResource(R.drawable.ic_share),contentDescription=shareTitle) } }) {
  // Opened from Separaciones the contract is read-only, outside the wizard, so it shows no wizard steps.
  if(!readOnly) WizardStepper(5)
  if(missing.isNotEmpty()&&!(completing&&onlyMarital)) MissingDataDialog(missing,onlyMarital,onComplete={completing=true},onCancel=onBack)
  if(ContractField.MARITAL_STATUS in missing) {
   FieldError(stringResource(R.string.contract_missing))
   MaritalStatusField(s.prospect?.maritalStatus.orEmpty(),viewModel::completeMarital)
  }
  Text(stringResource(R.string.reservation_contract_note),style=MaterialTheme.typography.bodySmall)
  when {
   ready!=null -> ContractPages(ready,s.contractRevision)
   s.contract==null&&s.contractError==null -> Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
    CircularProgressIndicator(Modifier.size(24.dp));Text(stringResource(R.string.contract_generating)) }
  }
  FieldError(s.contractError)
  OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
   Text(stringResource(R.string.contract_summary),style=MaterialTheme.typography.titleMedium)
   FieldValue(stringResource(R.string.field_name),s.prospect?.fullName.orEmpty())
   FieldValue(stringResource(R.string.field_document),s.prospect?.document.orEmpty())
   FieldValue(stringResource(R.string.field_marital),s.prospect?.maritalStatus.orEmpty())
   FieldValue(stringResource(R.string.reservation_lot),"${s.project?.name.orEmpty()} · ${s.lot?.code.orEmpty()}")
   FieldValue(stringResource(R.string.lot_area),"${s.lot?.dimensions?.area} m²")
   // US-58: the terms stored with the draft, not the wizard fields, so the preview matches what will sync.
   val terms=s.reservation?.terms?:NegotiationTerms()
   val currency=s.lot?.currency
   FieldValue(stringResource(R.string.reservation_lot_price),money(currency,s.lot?.price))
   terms.discount?.let { FieldValue(stringResource(R.string.reservation_discount),money(currency,it)) }
   FieldValue(stringResource(R.string.reservation_final_price),money(currency,s.lot?.price?.let(terms::finalPrice)))
   terms.bonus?.let { FieldValue(stringResource(R.string.reservation_bonus),it) }
   terms.notes?.let { FieldValue(stringResource(R.string.reservation_notes),it) }
   FieldValue(stringResource(R.string.field_amount),money(currency,s.reservation?.initialAmount))
  } }
  if(!readOnly) Row { Checkbox(accepted,{accepted=it});Text(stringResource(R.string.contract_ack),Modifier.padding(top=12.dp)) }
  FieldError(s.errorMessage)
  if(!readOnly) PrimaryButton(stringResource(R.string.reservation_confirm),accepted&&!s.working&&s.contractData.missing.isEmpty(),viewModel::confirm)
  if(!readOnly) TextButton(onClick={viewModel.cancel(onBack)},enabled=!s.working) { Text(stringResource(R.string.reservation_cancel_draft)) }
 }
}

/** US-13 scenario 2 (M22): the contract is not generated and the dialog names each datum to complete. */
@Composable
private fun MissingDataDialog(fields:List<ContractField>,canComplete:Boolean,onComplete:()->Unit,onCancel:()->Unit) {
 val names=fields.map { stringResource(it.label()) }.joinToString(", ")
 AlertDialog(onDismissRequest={},
  icon={ Icon(painterResource(R.drawable.ic_error),contentDescription=null,tint=MaterialTheme.colorScheme.error) },
  title={ Text(stringResource(R.string.contract_missing_title)) },
  text={ Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text(stringResource(R.string.contract_missing_fields,names))
   if(!canComplete) Text(stringResource(R.string.contract_missing_elsewhere)) } },
  confirmButton={ if(canComplete) Button(onClick=onComplete) { Text(stringResource(R.string.contract_complete)) }
   else Button(onClick=onCancel) { Text(stringResource(R.string.field_back)) } },
  dismissButton={ if(canComplete) TextButton(onClick=onCancel) { Text(stringResource(R.string.contract_cancel)) } })
}

@StringRes private fun ContractField.label():Int = when(this) {
 ContractField.BUYER_NAME -> R.string.contract_field_buyer_name
 ContractField.BUYER_DOCUMENT -> R.string.contract_field_buyer_document
 ContractField.MARITAL_STATUS -> R.string.contract_field_marital
 ContractField.LOT -> R.string.contract_field_lot
 ContractField.LOT_PRICE -> R.string.contract_field_lot_price
 ContractField.INITIAL_AMOUNT -> R.string.contract_field_initial_amount
}

/** The PDF pages as images; [revision] changes each time the same file is generated again. */
@Composable
private fun ContractPages(ready:ContractPreview.Ready,revision:Int) {
 BoxWithConstraints(Modifier.fillMaxWidth()) {
  // Twice the screen width keeps the text sharp when the client zooms in.
  val width=(constraints.maxWidth*2).coerceAtMost(MAX_PAGE_WIDTH)
  val pages by produceState(emptyList<ImageBitmap>(),ready.pdf.file,revision,width) {
   value=withContext(Dispatchers.IO) { runCatching { renderPages(ready.pdf.file,width) }.getOrDefault(emptyList()) } }
  Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
   pages.forEachIndexed { index,page -> ZoomablePage(page,stringResource(R.string.contract_page,index+1,pages.size)) }
   Text(pluralStringResource(R.plurals.contract_pages,ready.pdf.pages,ready.pdf.pages,ready.templateVersion),style=MaterialTheme.typography.bodySmall,
    color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
}

/** Pinch to zoom; a single finger only pans while zoomed in, so the page still scrolls. */
@Composable
private fun ZoomablePage(page:ImageBitmap,description:String) {
 var scale by remember { mutableFloatStateOf(1f) }
 var offset by remember { mutableStateOf(Offset.Zero) }
 var size by remember { mutableStateOf(IntSize.Zero) }
 val state=rememberTransformableState { zoom,pan,_ ->
  scale=(scale*zoom).coerceIn(1f,4f)
  val maxX=(scale-1)*size.width/2;val maxY=(scale-1)*size.height/2
  offset=if(scale==1f) Offset.Zero else Offset((offset.x+pan.x).coerceIn(-maxX,maxX),(offset.y+pan.y).coerceIn(-maxY,maxY))
 }
 Image(page,description,contentScale=ContentScale.FillWidth,modifier=Modifier.fillMaxWidth().aspectRatio(page.width.toFloat()/page.height)
  .border(1.dp,MaterialTheme.colorScheme.outlineVariant).clipToBounds().onSizeChanged { size=it }
  .transformable(state,canPan={ scale>1f }).graphicsLayer { scaleX=scale;scaleY=scale;translationX=offset.x;translationY=offset.y })
}

private const val MAX_PAGE_WIDTH=2048
/** Matches the FileProvider declared in AndroidManifest.xml, which exposes only the contract PDFs. */
private const val CONTRACT_AUTHORITY_SUFFIX=".contracts"

private fun renderPages(file:File,width:Int):List<ImageBitmap> =
 ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
  val renderer=PdfRenderer(fd)
  try {
   (0 until renderer.pageCount).map { index -> renderer.openPage(index).use { page ->
    val bitmap=createBitmap(width,width*page.height/page.width).apply { eraseColor(Color.WHITE) }
    page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
    bitmap.asImageBitmap() } }
  } finally { renderer.close() }
 }

private fun shareContract(context:Context,file:File,title:String) {
 val uri=FileProvider.getUriForFile(context,context.packageName+CONTRACT_AUTHORITY_SUFFIX,file)
 val send=Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM,uri)
  .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
 context.startActivity(Intent.createChooser(send,title))
}
