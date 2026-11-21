package com.novacorp.inmonode_app.features.fieldsales

import com.novacorp.inmonode_app.features.fieldsales.domain.model.GeoPoint
import com.novacorp.inmonode_app.features.fieldsales.presentation.map.components.contains
import com.novacorp.inmonode_app.features.fieldsales.presentation.map.components.labelPoint
import org.junit.Assert.*
import org.junit.Test

/** The code of each lot is written inside it, also for the irregular lots of a real plan (T49). */
class LotLabelPointTest {
    private fun ring(vararg xy: Double) = xy.toList().chunked(2).map { GeoPoint(it[0], it[1]) }.let { it + it.first() }

    @Test fun aRectangleIsLabelledAtItsCentre() {
        val label = labelPoint(ring(0.0, 0.0, 4.0, 0.0, 4.0, 2.0, 0.0, 2.0))

        assertEquals(2.0, label.longitude, 1e-9)
        assertEquals(1.0, label.latitude, 1e-9)
    }

    @Test fun aNotchedLotIsLabelledAwayFromItsNotchCorner() {
        // P-04 of the T49 check: the vertex average fell on the corner of the notch it shares with P-03.
        val notched = ring(36.0, 0.0, 48.0, 4.0, 40.0, 20.0, 31.0, 20.0, 31.0, 9.0, 36.0, 9.0)

        val label = labelPoint(notched)

        assertTrue(contains(notched, label))
        assertTrue("clear of the notch corner", label.latitude > 9.5 || label.longitude > 36.5)
    }

    @Test fun aLotWhoseCentroidFallsOutsideIsLabelledInsideItsWidestStretch() {
        // A U-shaped lot: its centroid (1.5, 1.36) lies in the gap between the arms.
        val uShaped = ring(0.0, 0.0, 3.0, 0.0, 3.0, 3.0, 2.0, 3.0, 2.0, 1.0, 1.0, 1.0, 1.0, 3.0, 0.0, 3.0)

        val label = labelPoint(uShaped)

        assertTrue(contains(uShaped, label))
        assertEquals(0.5, label.longitude, 1e-9)
    }

    @Test fun realCoordinatesKeepTheirPrecision() {
        val lurin = ring(-76.8705, -12.2752, -76.8703897, -12.2752, -76.8703897, -12.2750373, -76.8705, -12.2750373)

        val label = labelPoint(lurin)

        assertEquals(-76.87044485, label.longitude, 1e-7)
        assertEquals(-12.27511865, label.latitude, 1e-7)
    }
}
