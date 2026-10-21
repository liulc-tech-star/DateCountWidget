package com.datecount.widget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DateCountTest {
    private fun event(
        dateIso: String,
        mode: DisplayMode = DisplayMode.BOTH
    ) = DateEvent(
        id = "test-event",
        title = "Test date",
        dateIso = dateIso,
        displayMode = mode,
        createdAtMillis = 0L
    )

    @Test
    fun sameDayHasZeroDays() {
        val count = calculateCount(event("2026-09-30"), LocalDate.of(2026, 9, 30))

        assertTrue(count.isToday)
        assertFalse(count.isPast)
        assertEquals(0L, count.totalDays)
        assertEquals(0L, count.weeks)
        assertEquals(0L, count.leftoverDays)
    }

    @Test
    fun pastDateSplitsIntoWeeksAndDays() {
        val count = calculateCount(event("2026-09-22"), LocalDate.of(2026, 9, 30))

        assertTrue(count.isPast)
        assertFalse(count.isToday)
        assertEquals(8L, count.totalDays)
        assertEquals(1L, count.weeks)
        assertEquals(1L, count.leftoverDays)
    }

    @Test
    fun futureDateUsesNegativeTotalAndPositiveDisplayCounts() {
        val count = calculateCount(event("2026-10-08"), LocalDate.of(2026, 9, 30))

        assertFalse(count.isPast)
        assertFalse(count.isToday)
        assertEquals(-8L, count.totalDays)
        assertEquals(1L, count.weeks)
        assertEquals(1L, count.leftoverDays)
    }

    @Test
    fun advancingTodayAcrossMonthBoundaryIncreasesElapsedCount() {
        val savedEvent = event("2026-09-22")
        val before = calculateCount(savedEvent, LocalDate.of(2026, 9, 30))
        val after = calculateCount(savedEvent, LocalDate.of(2026, 10, 1))

        assertEquals(before.totalDays + 1, after.totalDays)
        assertEquals(1L, after.weeks)
        assertEquals(2L, after.leftoverDays)
    }

    @Test
    fun countdownBecomesTodayThenPast() {
        val savedEvent = event("2026-10-01")

        assertEquals(-1L, calculateCount(savedEvent, LocalDate.of(2026, 9, 30)).totalDays)
        assertTrue(calculateCount(savedEvent, LocalDate.of(2026, 10, 1)).isToday)
        assertTrue(calculateCount(savedEvent, LocalDate.of(2026, 10, 2)).isPast)
    }

    @Test
    fun leapDayIsIncludedInElapsedCount() {
        val count = calculateCount(event("2024-02-28"), LocalDate.of(2024, 3, 1))

        assertEquals(2L, count.totalDays)
        assertEquals(2L, count.leftoverDays)
    }

    @Test
    fun weeksAndDaysModeHasNoSecondaryLine() {
        val count = calculateCount(
            event("2026-09-22", DisplayMode.WEEKS_AND_DAYS),
            LocalDate.of(2026, 9, 30)
        )

        assertEquals("1 周 1 天", count.primaryLabel)
        assertNull(count.secondaryLabel)
    }

    @Test
    fun totalDaysModeUsesTheTotalAsPrimary() {
        val count = calculateCount(
            event("2026-09-22", DisplayMode.TOTAL_DAYS),
            LocalDate.of(2026, 9, 30)
        )

        assertEquals("8 天", count.primaryLabel)
        assertNull(count.secondaryLabel)
    }

    @Test
    fun bothModeShowsTheTotalAsSecondary() {
        val count = calculateCount(event("2026-09-22", DisplayMode.BOTH), LocalDate.of(2026, 9, 30))

        assertEquals("1 周 1 天", count.primaryLabel)
        assertEquals("8 天", count.secondaryLabel)
    }
}
