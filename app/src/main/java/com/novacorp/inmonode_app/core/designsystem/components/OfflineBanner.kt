package com.novacorp.inmonode_app.core.designsystem.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.novacorp.inmonode_app.R

@Composable
fun OfflineBanner(offline: Boolean) {
    if (offline) Surface(color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.field_offline), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}
