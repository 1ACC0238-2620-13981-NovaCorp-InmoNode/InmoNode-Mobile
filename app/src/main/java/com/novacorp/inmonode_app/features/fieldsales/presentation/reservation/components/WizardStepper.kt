package com.novacorp.inmonode_app.features.fieldsales.presentation.reservation.components
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.novacorp.inmonode_app.R
@Composable
fun WizardStepper(step:Int) {
 val names=listOf(R.string.wizard_prospect,R.string.wizard_voucher,R.string.wizard_ocr,R.string.wizard_contract)
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
  names.forEachIndexed { index,label -> Column(horizontalAlignment=Alignment.CenterHorizontally) {
   Surface(shape=CircleShape,color=if(index+1<=step)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
    border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.primary)) {
    Box(Modifier.size(28.dp),contentAlignment=Alignment.Center) {
     if(index+1<step) Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_check),contentDescription=null,modifier=Modifier.size(16.dp))
     else Text((index+1).toString())
    } }
   Text(stringResource(label),style=MaterialTheme.typography.labelSmall)
  } }
 }
}

@androidx.compose.ui.tooling.preview.Preview(widthDp=360,showBackground=true)
@Composable
private fun WizardStepperPreview() {
 com.novacorp.inmonode_app.core.designsystem.theme.InmoNodeAppTheme { WizardStepper(3) }
}
