package com.novacorp.inmonode_app.core.time

import java.time.Instant
import java.time.format.DateTimeFormatterBuilder

private val timestampFormat = DateTimeFormatterBuilder().appendInstant(3).toFormatter()

/** Fixed millisecond precision keeps ISO timestamps sortable chronologically in SQLite TEXT columns. */
internal fun utcTimestamp(instant: Instant = Instant.now()): String = timestampFormat.format(instant)
