package com.novacorp.inmonode_app.features.fieldsales

import com.novacorp.inmonode_app.core.time.utcTimestamp
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class TimestampOrderingTest {
    @Test fun textOrderingMatchesChronologicalOrderingEvenAtWholeSeconds() {
        val values = listOf("2026-10-09T12:00:00Z", "2026-10-09T12:00:00.100Z",
            "2026-10-09T12:00:00.101234Z", "2026-10-09T12:00:01Z").map(Instant::parse)
        val timestamps = values.map(::utcTimestamp)
        assertEquals(timestamps, timestamps.sorted())
        assertEquals("2026-10-09T12:00:00.000Z", timestamps.first())
        assertEquals("2026-10-09T12:00:00.101Z", timestamps[2])
    }
}
