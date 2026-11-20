package com.novacorp.inmonode_app.features.fieldsales

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.novacorp.inmonode_app.core.designsystem.theme.InmoNodeAppTheme
import com.novacorp.inmonode_app.features.fieldsales.domain.model.*
import com.novacorp.inmonode_app.features.fieldsales.presentation.map.components.LotDetailSheet
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

/** The lot sheet shows only the measures the plan gives: an irregular lot has no single front or depth (T49). */
class LotDetailSheetTest {
    @get:Rule val compose = createComposeRule()

    private val lShaped = listOf(GeoPoint(-76.87, -12.25), GeoPoint(-76.8699, -12.25), GeoPoint(-76.8699, -12.2501),
        GeoPoint(-76.86995, -12.2501), GeoPoint(-76.86995, -12.2502), GeoPoint(-76.87, -12.2502), GeoPoint(-76.87, -12.25))

    private fun show(dimensions: LotDimensions) = compose.setContent { InmoNodeAppTheme {
        LotDetailSheet(Lot(3, 1, "P-03", dimensions, listOf(lShaped), BigDecimal("46800"), "PEN", LotStatus.AVAILABLE),
            onDismiss = {}, onSimulate = {}, onReserve = {})
    } }

    @Test fun anIrregularLotShowsItsAreaWithoutFrontOrDepth() {
        show(LotDimensions(null, null, BigDecimal("205")))

        compose.onNodeWithText("205 m²").assertIsDisplayed()
        compose.onNodeWithText("Frente").assertDoesNotExist()
        compose.onNodeWithText("Fondo").assertDoesNotExist()
    }

    @Test fun aRegularLotShowsItsFrontAndDepth() {
        show(LotDimensions(BigDecimal("9"), BigDecimal("15"), BigDecimal("135")))

        compose.onNodeWithText("Frente").assertIsDisplayed()
        compose.onNodeWithText("9 m").assertIsDisplayed()
        compose.onNodeWithText("15 m").assertIsDisplayed()
    }
}
