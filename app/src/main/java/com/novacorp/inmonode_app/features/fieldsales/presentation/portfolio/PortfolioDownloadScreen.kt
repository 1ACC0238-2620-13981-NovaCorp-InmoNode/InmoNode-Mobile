package com.novacorp.inmonode_app.features.fieldsales.presentation.portfolio
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.core.designsystem.components.*
@Composable
fun PortfolioDownloadScreen(modifier: Modifier = Modifier, viewModel: PortfolioDownloadViewModel = hiltViewModel(), onContinue: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FieldPage(stringResource(if (state.errorMessage != null) R.string.portfolio_incomplete else R.string.portfolio_preparing), modifier) {
        Text(stringResource(R.string.portfolio_description))
        Spacer(Modifier.height(64.dp))
        Box(Modifier.fillMaxWidth(),contentAlignment=androidx.compose.ui.Alignment.Center) {
            Box(Modifier.size(168.dp).then(Modifier.background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(
                MaterialTheme.colorScheme.secondary,MaterialTheme.colorScheme.primary)),androidx.compose.foundation.shape.CircleShape)),
                contentAlignment=androidx.compose.ui.Alignment.Center) {
                Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_cloud_download),contentDescription=null,
                    modifier=Modifier.size(72.dp),tint=MaterialTheme.colorScheme.onPrimary)
            }
        }
        FieldError(state.errorMessage)
        Text(stringResource(R.string.portfolio_catalog), style = MaterialTheme.typography.titleMedium)
        if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        FieldValue(stringResource(R.string.portfolio_projects), if(state.loading)stringResource(R.string.field_loading) else state.portfolio.projects.size.toString())
        HorizontalDivider()
        FieldValue(stringResource(R.string.portfolio_lots), if(state.loading)stringResource(R.string.field_loading) else state.portfolio.projects.sumOf { it.lots.size }.toString())
        HorizontalDivider()
        if (state.completed) Text(stringResource(R.string.portfolio_ready))
        if (!state.loading && !state.completed) PrimaryButton(stringResource(R.string.field_retry), onClick = viewModel::retry)
        PrimaryButton(stringResource(if(state.loading)R.string.portfolio_background else R.string.field_continue), state.portfolio.downloadedAt != null, onContinue)
    }
}
