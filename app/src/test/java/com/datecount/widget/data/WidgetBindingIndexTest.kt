package com.datecount.widget.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetBindingIndexTest {
    @Test
    fun findsWidgetsBoundToEvent() {
        val bindings = mapOf(
            "w_7" to "event-a",
            "w_9" to "event-a",
            "w_11" to "event-b"
        )

        assertEquals(listOf(7, 9), widgetIdsBoundTo(bindings, "event-a").sorted())
    }

    @Test
    fun refreshBookkeepingKeysAreNotTreatedAsBindings() {
        val bindings: Map<String, Any?> = mapOf(
            "w_3" to "event-a",
            "day_3" to 20_000L,
            "version_3" to 4L
        )

        assertEquals(listOf(3), widgetIdsBoundTo(bindings, "event-a"))
    }

    @Test
    fun ignoresKeysWithoutUsableWidgetId() {
        val bindings = mapOf(
            "w_" to "event-a",
            "w_abc" to "event-a",
            "other_5" to "event-a"
        )

        assertEquals(emptyList<Int>(), widgetIdsBoundTo(bindings, "event-a"))
    }

    @Test
    fun returnsEmptyWhenNothingIsBound() {
        assertEquals(emptyList<Int>(), widgetIdsBoundTo(emptyMap<String, Any?>(), "event-a"))
    }

    @Test
    fun staleIdsCoverEveryStoredKeyFamily() {
        val bindings: Map<String, Any?> = mapOf(
            "w_4" to "event-a",
            "day_4" to 20_000L,
            "version_4" to 1L,
            "w_6" to "event-b",
            "version_6" to 2L,
            "day_9" to 20_001L,
            "unrelated" to "x"
        )

        assertEquals(setOf(6, 9), staleWidgetIds(bindings, setOf(4)))
    }

    @Test
    fun nothingIsStaleWhenEveryWidgetIsStillAlive() {
        val bindings: Map<String, Any?> = mapOf("w_4" to "event-a", "day_4" to 1L)

        assertEquals(emptySet<Int>(), staleWidgetIds(bindings, setOf(4)))
    }
}
