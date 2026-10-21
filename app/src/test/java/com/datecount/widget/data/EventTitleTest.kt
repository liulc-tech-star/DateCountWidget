package com.datecount.widget.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EventTitleTest {
    @Test
    fun trimsSurroundingWhitespace() {
        assertEquals("毕业", normalizeEventTitle("  毕业  "))
    }

    @Test
    fun blankTitleFallsBackToPlaceholder() {
        assertEquals("未命名日期", normalizeEventTitle("   "))
    }

    @Test
    fun longTitleIsClippedSoTheWidgetStaysOneLine() {
        val normalized = normalizeEventTitle("纪".repeat(MAX_EVENT_TITLE_LENGTH + 10))

        assertEquals(MAX_EVENT_TITLE_LENGTH, normalized.length)
        assertEquals("纪".repeat(MAX_EVENT_TITLE_LENGTH), normalized)
    }
}
