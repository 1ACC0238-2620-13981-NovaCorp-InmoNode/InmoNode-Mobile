package com.novacorp.inmonode_app.features.fieldsales.presentation.reservation.components
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.novacorp.inmonode_app.R
/** Steps of "Separar lote" (style guide: icons and connectors in primary and on-surface; M16, M20). */
@Composable
fun WizardStepper(step:Int) {
 val names=listOf(R.string.wizard_prospect,R.string.wizard_voucher,R.string.wizard_ocr,R.string.wizard_identity,R.string.wizard_contract)
 val reached=MaterialTheme.colorScheme.primary
 val pending=MaterialTheme.colorScheme.outline
 Row(Modifier.fillMaxWidth()) {
  names.forEachIndexed { index,label ->
   val number=index+1
   val state=stringResource(when { number<step->R.string.wizard_step_done; number==step->R.string.wizard_step_current; else->R.string.wizard_step_pending })
   // TalkBack reads each step with its state: "Datos OCR, paso actual".
   Column(Modifier.weight(1f).semantics(mergeDescendants=true) { stateDescription=state },horizontalAlignment=Alignment.CenterHorizontally) {
    // Connectors run from the cell edges to the circle: green up to the current step.
    Box(Modifier.fillMaxWidth().height(28.dp).drawBehind {
     val y=size.height/2; val width=2.dp.toPx()
     if(index>0) drawLine(if(number<=step) reached else pending,Offset(0f,y),Offset(size.width/2,y),width)
     if(index<names.lastIndex) drawLine(if(number<step) reached else pending,Offset(size.width/2,y),Offset(size.width,y),width)
    },contentAlignment=Alignment.Center) {
     Surface(shape=CircleShape,color=if(number<=step) reached else MaterialTheme.colorScheme.surface,
      contentColor=if(number<=step) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
      border=BorderStroke(1.dp,if(number<=step) reached else pending)) {
      Box(Modifier.size(28.dp),contentAlignment=Alignment.Center) {
       if(number<step) Icon(painterResource(R.drawable.ic_check),contentDescription=null,modifier=Modifier.size(16.dp))
       else Text(number.toString(),style=MaterialTheme.typography.labelMedium)
      }
     }
    }
    Spacer(Modifier.height(4.dp))
    Text(stringResource(label),style=MaterialTheme.typography.labelSmall,textAlign=TextAlign.Center,
     fontWeight=if(number==step) FontWeight.Bold else null,
     color=if(number==step) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
 }
}

@androidx.compose.ui.tooling.preview.Preview(widthDp=360,showBackground=true)
@Composable
private fun WizardStepperPreview() {
 com.novacorp.inmonode_app.core.designsystem.theme.InmoNodeAppTheme { WizardStepper(3) }
}
