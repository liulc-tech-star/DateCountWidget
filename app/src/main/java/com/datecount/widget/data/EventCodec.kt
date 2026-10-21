package com.datecount.widget.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement

internal val EVENT_JSON = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

internal fun encodeEvents(events: List<DateEvent>): String = EVENT_JSON.encodeToString(events)

// A damaged payload must never take the whole list down. When the array cannot be
// decoded as a whole, fall back to decoding element by element so that one bad
// record only costs that record.
internal fun decodeEvents(raw: String?): List<DateEvent> {
    if (raw.isNullOrBlank()) return emptyList()
    val events = runCatching { EVENT_JSON.decodeFromString<List<DateEvent>>(raw) }
        .getOrElse { decodeOneByOne(raw) }
    return events.filter { runCatching { it.date }.isSuccess }
}

private fun decodeOneByOne(raw: String): List<DateEvent> {
    val array = runCatching { EVENT_JSON.parseToJsonElement(raw) as? JsonArray }.getOrNull()
        ?: return emptyList()
    return array.mapNotNull { element ->
        runCatching { EVENT_JSON.decodeFromJsonElement<DateEvent>(element) }.getOrNull()
    }
}
