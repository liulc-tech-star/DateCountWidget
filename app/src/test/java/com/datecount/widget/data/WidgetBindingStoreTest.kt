package com.datecount.widget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetBindingStoreTest {
    private val prefs = FakeSharedPreferences()
    private val store = WidgetBindingStore(prefs)

    @Test
    fun bindsAndReadsBack() {
        store.bind(7, "event-a")

        assertEquals("event-a", store.eventIdFor(7))
        assertNull(store.eventIdFor(8))
    }

    @Test
    fun refreshBookkeepingStartsEmptyAndTracksTheDay() {
        assertNull(store.lastRefreshDayFor(7))

        store.markRefreshed(7, 20_000L)

        assertEquals(20_000L, store.lastRefreshDayFor(7))
    }

    @Test
    fun requestRefreshBumpsTheStoredVersion() {
        store.requestRefresh(7)
        store.requestRefresh(7)

        assertEquals(2L, prefs.getLong("version_7", 0L))
    }

    @Test
    fun removeWidgetDropsEveryKeyForThatWidget() {
        store.bind(7, "event-a")
        store.markRefreshed(7, 20_000L)
        store.requestRefresh(7)

        store.removeWidget(7)

        assertNull(store.eventIdFor(7))
        assertNull(store.lastRefreshDayFor(7))
        assertTrue(prefs.all.keys.none { it == "w_7" || it == "day_7" || it == "version_7" })
    }

    @Test
    fun removeEventReturnsAffectedWidgetsAndClearsOnlyTheirBindings() {
        store.bind(7, "event-a")
        store.bind(9, "event-a")
        store.bind(11, "event-b")

        val affected = store.removeEvent("event-a")

        assertEquals(setOf(7, 9), affected.toSet())
        assertNull(store.eventIdFor(7))
        assertNull(store.eventIdFor(9))
        assertEquals("event-b", store.eventIdFor(11))
    }

    @Test
    fun pruneKeepsLiveWidgetsAndDropsTheRest() {
        store.bind(7, "event-a")
        store.markRefreshed(7, 20_000L)
        store.bind(9, "event-b")
        store.markRefreshed(9, 20_000L)
        store.requestRefresh(9)

        store.pruneMissingWidgets(setOf(7))

        assertEquals("event-a", store.eventIdFor(7))
        assertEquals(20_000L, store.lastRefreshDayFor(7))
        assertNull(store.eventIdFor(9))
        assertNull(store.lastRefreshDayFor(9))
        assertTrue(prefs.all.keys.none { it == "w_9" || it == "day_9" || it == "version_9" })
    }

    @Test
    fun pruneLeavesEverythingAloneWhenEveryWidgetIsAlive() {
        store.bind(7, "event-a")
        store.markRefreshed(7, 20_000L)
        val before = prefs.all.toMap()

        store.pruneMissingWidgets(setOf(7))

        assertEquals(before, prefs.all.toMap())
    }
}
