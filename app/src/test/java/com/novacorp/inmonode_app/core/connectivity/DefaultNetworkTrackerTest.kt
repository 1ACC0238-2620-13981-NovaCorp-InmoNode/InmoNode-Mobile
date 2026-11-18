package com.novacorp.inmonode_app.core.connectivity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultNetworkTrackerTest {
    @Test
    fun startsOfflineWithoutDefaultNetwork() {
        assertFalse(DefaultNetworkTracker<String>().isOnline)
    }

    @Test
    fun losingTheDefaultNetworkGoesOffline() {
        val tracker = DefaultNetworkTracker<String>()
        tracker.onAvailable("wifi", validated = true)
        assertTrue(tracker.isOnline)

        tracker.onLost("wifi")

        assertFalse(tracker.isOnline)
    }

    @Test
    fun followsValidationChangesOfTheDefaultNetwork() {
        val tracker = DefaultNetworkTracker<String>()
        tracker.onAvailable("wifi", validated = false)
        assertFalse(tracker.isOnline)

        tracker.onCapabilitiesChanged("wifi", validated = true)
        assertTrue(tracker.isOnline)

        tracker.onCapabilitiesChanged("wifi", validated = false)
        assertFalse(tracker.isOnline)
    }

    @Test
    fun lateLossOfPreviousNetworkKeepsTheNewOneOnline() {
        val tracker = DefaultNetworkTracker<String>()
        tracker.onAvailable("wifi", validated = true)
        tracker.onAvailable("cellular", validated = true)

        tracker.onLost("wifi")

        assertTrue(tracker.isOnline)
    }

    @Test
    fun recoversWhenANewDefaultNetworkValidates() {
        val tracker = DefaultNetworkTracker<String>()
        tracker.onAvailable("wifi", validated = true)
        tracker.onLost("wifi")

        tracker.onAvailable("wifi-2", validated = false)
        assertFalse(tracker.isOnline)
        tracker.onCapabilitiesChanged("wifi-2", validated = true)

        assertTrue(tracker.isOnline)
    }
}
