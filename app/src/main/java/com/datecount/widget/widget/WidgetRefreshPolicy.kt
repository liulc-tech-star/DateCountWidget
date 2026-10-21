package com.datecount.widget.widget

import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

internal fun shouldRefreshWidget(
    lastRefreshDay: Long?,
    today: LocalDate,
    force: Boolean = false
): Boolean = force || lastRefreshDay != today.toEpochDay()

// A widget nobody has configured yet has nothing to draw, so skip it unless the
// caller is explicitly forcing a repaint.
internal fun isUnconfiguredWidget(
    boundEventId: String?,
    lastRefreshDay: Long?,
    force: Boolean
): Boolean = !force && boundEventId == null && lastRefreshDay == null

internal fun nextMidnightDelayMillis(now: ZonedDateTime): Long {
    val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return Duration.between(now.toInstant(), nextMidnight.toInstant()).toMillis()
}

internal const val MAX_REFRESH_RETRIES = 5

internal fun shouldRetryRefresh(runAttemptCount: Int): Boolean =
    runAttemptCount < MAX_REFRESH_RETRIES
