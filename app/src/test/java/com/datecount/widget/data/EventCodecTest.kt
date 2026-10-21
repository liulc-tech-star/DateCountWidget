package com.datecount.widget.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EventCodecTest {
    private fun event(id: String, dateIso: String = "2020-06-01", createdAt: Long = 1L) = DateEvent(
        id = id,
        title = "标题$id",
        dateIso = dateIso,
        createdAtMillis = createdAt
    )

    @Test
    fun roundTripsEvents() {
        val events = listOf(event("a"), event("b", createdAt = 2L))

        assertEquals(events, decodeEvents(encodeEvents(events)))
    }

    @Test
    fun missingPayloadDecodesToEmptyList() {
        assertEquals(emptyList<DateEvent>(), decodeEvents(null))
        assertEquals(emptyList<DateEvent>(), decodeEvents("   "))
    }

    @Test
    fun unparseablePayloadDecodesToEmptyList() {
        assertEquals(emptyList<DateEvent>(), decodeEvents("{not json"))
    }

    @Test
    fun keepsGoodEntriesWhenOneEntryIsBroken() {
        val raw = """[
            {"id":"a","title":"A","dateIso":"2020-06-01","displayMode":"BOTH","createdAtMillis":1},
            {"id":"broken"},
            {"id":"c","title":"C","dateIso":"2020-06-02","displayMode":"BOTH","createdAtMillis":3}
        ]"""

        assertEquals(listOf("a", "c"), decodeEvents(raw).map { it.id })
    }

    @Test
    fun dropsEventsWhoseDateCannotBeRead() {
        val raw = """[
            {"id":"a","title":"A","dateIso":"2020-06-01","displayMode":"BOTH","createdAtMillis":1},
            {"id":"b","title":"B","dateIso":"not-a-date","displayMode":"BOTH","createdAtMillis":2}
        ]"""

        val decoded = decodeEvents(raw)

        assertEquals(listOf("a"), decoded.map { it.id })
        assertEquals(LocalDate.of(2020, 6, 1), decoded.single().date)
    }

    @Test
    fun ignoresFieldsWrittenByNewerVersions() {
        val raw = """[{"id":"a","title":"A","dateIso":"2020-06-01","displayMode":"BOTH","createdAtMillis":1,"future":"x"}]"""

        assertEquals(listOf("a"), decodeEvents(raw).map { it.id })
    }

    @Test
    fun everyDisplayModeRoundTrips() {
        val events = DisplayMode.entries.mapIndexed { index, mode ->
            DateEvent(
                id = "e$index",
                title = "标题",
                dateIso = "2020-06-01",
                displayMode = mode,
                createdAtMillis = index.toLong()
            )
        }

        assertEquals(events, decodeEvents(encodeEvents(events)))
    }

    @Test
    fun unknownDisplayModeFallsBackToTheDefault() {
        val raw = """[{"id":"a","title":"A","dateIso":"2020-06-01","displayMode":"WEEKS_ONLY","createdAtMillis":1}]"""

        val decoded = decodeEvents(raw)

        assertEquals(1, decoded.size)
        assertEquals(DisplayMode.BOTH, decoded.single().displayMode)
    }
}
