package com.novacorp.inmonode_app.features.fieldsales.presentation.map.components
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.core.designsystem.theme.*

/** Map key (mock-up M06): each state with the same fill that CadastralMapCanvas paints on the lots. */
@Composable
fun LotLegend(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(LightGreen, stringResource(R.string.map_legend_available))
        LegendItem(AlertYellow, stringResource(R.string.map_reserved))
        LegendItem(TerracottaOrange, stringResource(R.string.map_sold))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    val shape = RoundedCornerShape(2.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        // The outline keeps the light yellow swatch visible on the light background.
        Box(Modifier.size(12.dp).background(color, shape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
