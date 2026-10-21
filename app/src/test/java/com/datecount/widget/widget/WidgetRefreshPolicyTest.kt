package com.datecount.widget.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class WidgetRefreshPolicyTest {
    private val today = LocalDate.of(2026, 9, 30)

    @Test
    fun newWidgetNeedsInitialRefresh() {
        assertTrue(shouldRefreshWidget(null, today))
    }

    @Test
    fun sameDateDoesNotRefreshAgain() {
        assertFalse(shouldRefreshWidget(today.toEpochDay(), today))
    }

    @Test
    fun crossingMonthBoundaryRefreshes() {
        assertTrue(shouldRefreshWidget(today.toEpochDay(), today.plusDays(1)))
    }

    @Test
    fun goingBackToPreviousDateRefreshes() {
        assertTrue(shouldRefreshWidget(today.toEpochDay(), today.minusDays(1)))
    }

    @Test
    fun openingAppCanRefreshOnSameDate() {
        assertTrue(shouldRefreshWidget(today.toEpochDay(), today, force = true))
    }

    @Test
    fun schedulesRemainingTimeUntilShanghaiMidnight() {
        val now = ZonedDateTime.of(2026, 9, 30, 23, 59, 0, 0, ZoneId.of("Asia/Shanghai"))

        assertEquals(TimeUnit.MINUTES.toMillis(1), nextMidnightDelayMillis(now))
    }

    @Test
    fun midnightSchedulesNextDayInsteadOfImmediateLoop() {
        val now = ZonedDateTime.of(2026, 10, 1, 0, 0, 0, 0, ZoneId.of("Asia/Shanghai"))

        assertEquals(TimeUnit.HOURS.toMillis(24), nextMidnightDelayMillis(now))
    }

    @Test
    fun middaySchedulesTwelveHoursNotFixedTwentyFour() {
        val now = ZonedDateTime.of(2026, 9, 30, 12, 0, 0, 0, ZoneId.of("Asia/Shanghai"))

        assertEquals(TimeUnit.HOURS.toMillis(12), nextMidnightDelayMillis(now))
    }

    @Test
    fun schedulesAcrossYearBoundary() {
        val now = ZonedDateTime.of(2026, 12, 31, 23, 30, 0, 0, ZoneId.of("Asia/Shanghai"))

        assertEquals(TimeUnit.MINUTES.toMillis(30), nextMidnightDelayMillis(now))
    }

    @Test
    fun daylightSavingStartUsesTwentyThreeHourDay() {
        val now = ZonedDateTime.of(2026, 3, 8, 0, 0, 0, 0, ZoneId.of("America/New_York"))

        assertEquals(TimeUnit.HOURS.toMillis(23), nextMidnightDelayMillis(now))
    }

    @Test
    fun daylightSavingEndUsesTwentyFiveHourDay() {
        val now = ZonedDateTime.of(2026, 11, 1, 0, 0, 0, 0, ZoneId.of("America/New_York"))

        assertEquals(TimeUnit.HOURS.toMillis(25), nextMidnightDelayMillis(now))
    }

    @Test
    fun timezoneChangeRecomputesLocalMidnight() {
        val shanghai = ZonedDateTime.of(2026, 9, 30, 12, 0, 0, 0, ZoneId.of("Asia/Shanghai"))
        val utc = shanghai.withZoneSameInstant(ZoneId.of("UTC"))

        assertEquals(TimeUnit.HOURS.toMillis(12), nextMidnightDelayMillis(shanghai))
        assertEquals(TimeUnit.HOURS.toMillis(20), nextMidnightDelayMillis(utc))
    }

    @Test
    fun retriesAreBounded() {
        assertTrue(shouldRetryRefresh(0))
        assertTrue(shouldRetryRefresh(MAX_REFRESH_RETRIES - 1))
        assertFalse(shouldRetryRefresh(MAX_REFRESH_RETRIES))
        assertFalse(shouldRetryRefresh(MAX_REFRESH_RETRIES + 3))
    }

    @Test
    fun unconfiguredWidgetIsSkippedUnlessForced() {
        assertTrue(isUnconfiguredWidget(boundEventId = null, lastRefreshDay = null, force = false))
        assertFalse(isUnconfiguredWidget(boundEventId = null, lastRefreshDay = null, force = true))
        assertFalse(isUnconfiguredWidget(boundEventId = "event-a", lastRefreshDay = null, force = false))
        assertFalse(
            isUnconfiguredWidget(boundEventId = null, lastRefreshDay = today.toEpochDay(), force = false)
        )
    }
}
