package com.novacorp.inmonode_app.features.fieldsales.presentation.map.components
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.core.designsystem.components.*
import com.novacorp.inmonode_app.features.fieldsales.domain.model.Lot
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotDetailSheet(lot: Lot, onDismiss: () -> Unit, onSimulate: () -> Unit, onReserve: () -> Unit) {
    ModalBottomSheet(onDismissRequest=onDismiss) {
        Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(lot.code,style=MaterialTheme.typography.headlineMedium)
            Text(if(lot.isAvailable()) stringResource(R.string.map_available) else stringResource(R.string.lot_unavailable))
            FieldValue(stringResource(R.string.lot_area), "${lot.dimensions.area} m²")
            FieldValue(stringResource(R.string.lot_front), "${lot.dimensions.front} m")
            FieldValue(stringResource(R.string.lot_depth), "${lot.dimensions.depth} m")
            FieldValue(stringResource(R.string.lot_price), "${lot.currency} ${lot.price}")
            OutlinedButton(onClick=onSimulate,modifier=Modifier.fillMaxWidth()) { Text(stringResource(R.string.lot_simulate)) }
            PrimaryButton(stringResource(R.string.lot_reserve),lot.isAvailable(),onReserve)
        }
    }
}
