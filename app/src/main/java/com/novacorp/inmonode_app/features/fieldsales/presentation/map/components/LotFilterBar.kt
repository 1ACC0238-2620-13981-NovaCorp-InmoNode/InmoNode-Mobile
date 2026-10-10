package com.novacorp.inmonode_app.features.fieldsales.presentation.map.components
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.novacorp.inmonode_app.R
@Composable
fun LotFilterBar(available: Boolean, areaFilter: Boolean, range: ClosedFloatingPointRange<Float>, maximum: Float,
    onAvailable: (Boolean) -> Unit, onFilter: (Boolean) -> Unit, onArea: (ClosedFloatingPointRange<Float>) -> Unit) {
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        FilterChip(selected=!available,onClick={onAvailable(false)},label={Text(stringResource(R.string.map_all))})
        FilterChip(selected=available,onClick={onAvailable(!available)},label={Text(stringResource(R.string.map_available))})
        FilterChip(selected=areaFilter,onClick={onFilter(!areaFilter)},label={Text(stringResource(R.string.map_area))})
    }
    if(areaFilter) {
        Text("${range.start.toInt()}–${range.endInclusive.toInt()} m²",style=MaterialTheme.typography.bodySmall)
        RangeSlider(value=range,onValueChange=onArea,valueRange=0f..maximum,modifier=Modifier.fillMaxWidth())
        TextButton(onClick={onFilter(false)}) { Text(stringResource(R.string.map_clear)) }
    }
}
