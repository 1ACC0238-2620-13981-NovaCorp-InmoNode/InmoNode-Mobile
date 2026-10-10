package com.novacorp.inmonode_app.features.fieldsales.presentation.navigation
import androidx.navigation.*
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import kotlinx.serialization.Serializable
import com.novacorp.inmonode_app.features.fieldsales.presentation.map.CadastralMapScreen
import com.novacorp.inmonode_app.features.fieldsales.presentation.portfolio.PortfolioDownloadScreen
import com.novacorp.inmonode_app.features.fieldsales.presentation.prospects.*
import com.novacorp.inmonode_app.features.fieldsales.presentation.simulation.QuickSimulationScreen
import com.novacorp.inmonode_app.features.fieldsales.presentation.reservation.*
import com.novacorp.inmonode_app.features.fieldsales.presentation.reservation.components.ContractPreviewStep
import com.novacorp.inmonode_app.features.fieldsales.presentation.sync.*
import com.novacorp.inmonode_app.features.vouchers.presentation.navigation.*
@Serializable data object MapNavGraphRoute
@Serializable data object CadastralMapRoute
@Serializable data object PortfolioDownloadRoute
@Serializable data class SimulationRoute(val lotId:Long)
@Serializable data class ReservationRoute(val lotId:Long)
@Serializable data class ContractRoute(val reservationId:String,val readOnly:Boolean=false)
@Serializable data class ReservationSavedRoute(val reservationId:String)
@Serializable data object ProspectsNavGraphRoute
@Serializable data object ProspectListRoute
@Serializable data object ProspectFormRoute
@Serializable data object SyncNavGraphRoute
@Serializable data object SyncQueueRoute
@Serializable data class ConflictRoute(val reservationId:String)
fun NavGraphBuilder.mapNavGraph(navController:NavHostController) {
 navigation<MapNavGraphRoute>(startDestination=CadastralMapRoute) {
  composable<CadastralMapRoute> { CadastralMapScreen(onDownload={navController.navigate(PortfolioDownloadRoute)},
   onSimulate={navController.navigate(SimulationRoute(it))},onReserve={navController.navigate(ReservationRoute(it))}) }
  composable<PortfolioDownloadRoute> { PortfolioDownloadScreen(onContinue={navController.popBackStack()}) }
  composable<SimulationRoute> { QuickSimulationScreen(onBack={navController.popBackStack()},onReserve={navController.navigate(ReservationRoute(it))}) }
  composable<ReservationRoute> { ReservationWizardScreen(onBack={navController.popBackStack()},
   onNewProspect={navController.navigate(ProspectFormRoute)},onCapture={navController.navigate(VoucherCaptureRoute(it)) {
    popUpTo<ReservationRoute> { inclusive=true } } }) }
  composable<ContractRoute> { entry -> val route=entry.toRoute<ContractRoute>()
   ContractPreviewStep(readOnly=route.readOnly,onBack={navController.popBackStack()},onConfirmed={navController.navigate(ReservationSavedRoute(it)) {
    popUpTo<CadastralMapRoute>() } }) }
  composable<ReservationSavedRoute> { ReservationConfirmationScreen(onMap={navController.navigate(CadastralMapRoute) {
   popUpTo<CadastralMapRoute> { inclusive=true } } },onSync={navController.navigate(SyncNavGraphRoute) {
   popUpTo<CadastralMapRoute>() } }) }
 }
}
fun NavGraphBuilder.prospectsNavGraph(navController:NavHostController) {
 navigation<ProspectsNavGraphRoute>(startDestination=ProspectListRoute) {
  composable<ProspectListRoute> { entry ->
   val newId by entry.savedStateHandle.getStateFlow<String?>("newProspectId",null).collectAsStateWithLifecycle()
   ProspectListScreen(newProspectId=newId,onNew={navController.navigate(ProspectFormRoute)},onSeparations={navController.navigate(SeparationListRoute)}) }
  composable<ProspectFormRoute> { ProspectFormScreen(onBack={navController.popBackStack()},onSaved={ id ->
   navController.previousBackStackEntry?.savedStateHandle?.set("prospectId",id)
   navController.previousBackStackEntry?.savedStateHandle?.set("newProspectId",id)
   navController.popBackStack()
  }) }
 }
}
fun NavGraphBuilder.syncNavGraph(navController:NavHostController) {
 navigation<SyncNavGraphRoute>(startDestination=SyncQueueRoute) {
  composable<SyncQueueRoute> { SyncQueueScreen(onConflict={navController.navigate(ConflictRoute(it))},onDraft={navController.navigate(VoucherCaptureRoute(it))}) }
  composable<ConflictRoute> { ConflictScreen(onBack={navController.popBackStack()},onResolved={navController.popBackStack()}) }
 }
}
