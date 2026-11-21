package com.novacorp.inmonode_app.features.vouchers.presentation.navigation
import androidx.navigation.*
import androidx.navigation.compose.*
import kotlinx.serialization.Serializable
import com.novacorp.inmonode_app.features.vouchers.presentation.capture.VoucherCaptureScreen
import com.novacorp.inmonode_app.features.vouchers.presentation.identitydocument.IdentityDocumentCaptureScreen
import com.novacorp.inmonode_app.features.vouchers.presentation.ocrreview.OcrReviewScreen
import com.novacorp.inmonode_app.features.vouchers.presentation.separations.*
import com.novacorp.inmonode_app.features.fieldsales.presentation.navigation.*
@Serializable data class VoucherCaptureRoute(val reservationId:String,val replacement:Boolean=false)
@Serializable data class OcrReviewRoute(val voucherId:String,val replacement:Boolean=false)
/** US-59: [inWizard] adds "Omitir" and continues to the contract; otherwise the photo is attached later. */
@Serializable data class IdentityDocumentCaptureRoute(val reservationId:String,val inWizard:Boolean=true)
@Serializable data object SeparationListRoute
@Serializable data class SeparationDetailRoute(val reservationId:String)
fun NavGraphBuilder.vouchersNavGraph(navController:NavHostController) {
 composable<VoucherCaptureRoute> { entry -> val route=entry.toRoute<VoucherCaptureRoute>()
  VoucherCaptureScreen(onBack={navController.popBackStack()},onCaptured={navController.navigate(OcrReviewRoute(it,route.replacement)) {
   popUpTo<VoucherCaptureRoute> { inclusive=true } } }) }
 composable<OcrReviewRoute> { entry -> val route=entry.toRoute<OcrReviewRoute>()
  OcrReviewScreen(onBack={navController.popBackStack()},onRecapture={navController.navigate(VoucherCaptureRoute(it,route.replacement)) {
   popUpTo<OcrReviewRoute> { inclusive=true } } },onReviewed={ id ->
   if(route.replacement) navController.navigate(SyncNavGraphRoute) { popUpTo<OcrReviewRoute> { inclusive=true } }
   else navController.navigate(IdentityDocumentCaptureRoute(id)) { popUpTo<OcrReviewRoute> { inclusive=true } }
  }) }
 composable<IdentityDocumentCaptureRoute> { entry -> val route=entry.toRoute<IdentityDocumentCaptureRoute>()
  IdentityDocumentCaptureScreen(inWizard=route.inWizard,onBack={navController.popBackStack()},onDone={
   if(route.inWizard) navController.navigate(ContractRoute(route.reservationId)) { popUpTo<IdentityDocumentCaptureRoute> { inclusive=true } }
   else navController.popBackStack()
  }) }
 composable<SeparationListRoute> { SeparationListScreen(onProspects={navController.popBackStack()},onDetail={navController.navigate(SeparationDetailRoute(it))}) }
 composable<SeparationDetailRoute> { SeparationDetailScreen(onBack={navController.popBackStack()},
  onReplace={navController.navigate(VoucherCaptureRoute(it,true))},onContract={navController.navigate(ContractRoute(it,true))},
  onIdentityDocument={navController.navigate(IdentityDocumentCaptureRoute(it,inWizard=false))}) }
}
