package com.datecount.widget.widget

import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

internal fun shouldRefreshWidget(
    lastRefreshDay: Long?,
    today: LocalDate,
    force: Boolean = false
): Boolean = force || lastRefreshDay != today.toEpochDay()

internal fun nextMidnightDelayMillis(now: ZonedDateTime): Long {
    val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return Duration.between(now.toInstant(), nextMidnight.toInstant()).toMillis()
}
