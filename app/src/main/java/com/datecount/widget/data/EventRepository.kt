package com.datecount.widget.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

private val Context.eventStore by preferencesDataStore(
    name = "date_events",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

class EventRepository(private val context: Context) {
    val events: Flow<List<DateEvent>> = context.eventStore.data.map { prefs ->
        decodeEvents(prefs[EVENTS_KEY])
    }

    suspend fun getEvents(): List<DateEvent> = events.first()

    suspend fun getEvent(id: String): DateEvent? = getEvents().firstOrNull { it.id == id }

    suspend fun save(
        id: String?,
        title: String,
        date: LocalDate,
        displayMode: DisplayMode
    ): DateEvent {
        val existing = id?.let { getEvent(it) }
        val saved = DateEvent(
            id = existing?.id ?: UUID.randomUUID().toString(),
            title = normalizeEventTitle(title),
            dateIso = date.toString(),
            displayMode = displayMode,
            createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis()
        )
        context.eventStore.edit { prefs ->
            val current = decodeEvents(prefs[EVENTS_KEY])
            val next = current.filterNot { it.id == saved.id } + saved
            prefs[EVENTS_KEY] = encodeEvents(next.sortedBy { it.createdAtMillis })
        }
        return saved
    }

    suspend fun delete(id: String) {
        context.eventStore.edit { prefs ->
            val current = decodeEvents(prefs[EVENTS_KEY])
            prefs[EVENTS_KEY] = encodeEvents(current.filterNot { it.id == id })
        }
    }

    companion object {
        private val EVENTS_KEY = stringPreferencesKey("events_json")
    }
}
