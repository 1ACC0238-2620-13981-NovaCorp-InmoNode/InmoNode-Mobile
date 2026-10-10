package com.novacorp.inmonode_app.features.fieldsales.presentation.map
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novacorp.inmonode_app.R
import com.novacorp.inmonode_app.core.designsystem.components.*
import com.novacorp.inmonode_app.features.fieldsales.presentation.map.components.*
@Composable
fun CadastralMapScreen(modifier: Modifier=Modifier, viewModel: CadastralMapViewModel=hiltViewModel(),
    onDownload: () -> Unit, onSimulate: (Long) -> Unit, onReserve: (Long) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }; var reset by remember { mutableIntStateOf(0) }
    var initialDownloadOpened by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.loaded,state.connectivityKnown) {
        if(state.loaded&&state.connectivityKnown&&!initialDownloadOpened) {
            initialDownloadOpened=true
            if(!state.offline||state.portfolio.downloadedAt==null)onDownload()
        }
    }
    Column(modifier.fillMaxSize()) {
        OfflineBanner(state.offline)
        Column(Modifier.padding(horizontal=16.dp,vertical=16.dp)) {
            Text(stringResource(R.string.cadastral_map_title),style=MaterialTheme.typography.headlineMedium)
            Box {
                OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) {
                    Text(state.project?.name ?: stringResource(R.string.map_select_project)) }
                DropdownMenu(expanded,onDismissRequest={expanded=false}) {
                    state.portfolio.projects.forEach { project -> DropdownMenuItem(text={Text(project.name)},onClick={viewModel.project(project.id);expanded=false}) }
                }
            }
            val maximum=(state.project?.lots?.maxOfOrNull { it.dimensions.area.toFloat() } ?: 1f).coerceAtLeast(1f)
            LotFilterBar(state.availableOnly,state.areaFilter,state.minArea.coerceIn(0f,maximum)..state.maxArea.coerceIn(0f,maximum),maximum,
                viewModel::available,viewModel::filter,viewModel::area)
            FieldError(state.errorMessage)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val lots=state.project?.lots.orEmpty()
            if(lots.isEmpty()) Column(Modifier.align(Alignment.Center).padding(16.dp)) {
                Text(stringResource(R.string.field_empty)); PrimaryButton(stringResource(R.string.map_download),onClick=onDownload)
            } else {
                CadastralMapCanvas(lots,state::matches,state.selectedLotId,Modifier.fillMaxSize(),reset,viewModel::select)
                if(lots.none(state::matches)) Surface(modifier=Modifier.align(Alignment.Center).padding(24.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.map_no_results)); Text(stringResource(R.string.map_no_results_help))
                        TextButton(onClick={viewModel.filter(false);viewModel.available(false)}) { Text(stringResource(R.string.map_clear)) }
                    }
                }
                OutlinedButton(onClick={reset++},modifier=Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                    Text(stringResource(R.string.map_center)) }
            }
        }
        Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.map_available),style=MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.map_reserved),style=MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.map_sold),style=MaterialTheme.typography.bodySmall)
        }
    }
    state.selectedLot?.let { lot ->
        if(lot.isAvailable()) LotDetailSheet(lot,{viewModel.select(null)},
            {viewModel.select(null);onSimulate(lot.id)}, {viewModel.select(null);onReserve(lot.id)})
        else AlertDialog(onDismissRequest={viewModel.select(null)},
            title={Text(stringResource(R.string.lot_unavailable))},
            text={Text(lot.code)},confirmButton={TextButton(onClick={viewModel.select(null);viewModel.available(true)}) {
                Text(stringResource(R.string.map_available)) }},dismissButton={TextButton(onClick={viewModel.select(null)}) {
                Text(stringResource(R.string.field_back)) }})
    }
}
