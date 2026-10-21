package com.datecount.widget.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class WidgetBindingStore(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    fun bind(appWidgetId: Int, eventId: String) {
        prefs.edit().putString(key(appWidgetId), eventId).apply()
    }

    fun removeWidget(appWidgetId: Int) {
        prefs.edit()
            .remove(key(appWidgetId))
            .remove(dayKey(appWidgetId))
            .remove(versionKey(appWidgetId))
            .apply()
    }

    fun eventIdFor(appWidgetId: Int): String? = prefs.getString(key(appWidgetId), null)

    fun appWidgetIdsFor(eventId: String): List<Int> = widgetIdsBoundTo(prefs.all, eventId)

    // Returns the affected widget ids so the caller can repaint them right away.
    fun removeEvent(eventId: String): List<Int> {
        val affected = appWidgetIdsFor(eventId)
        if (affected.isEmpty()) return affected
        val editor = prefs.edit()
        affected.forEach { editor.remove(key(it)) }
        editor.apply()
        return affected
    }

    // Drops every key whose appWidgetId is no longer owned by this app. Widget ids are
    // reassigned by the host, so bindings restored from a backup — or left behind when
    // onDeleted never arrived — must not survive into a new install.
    fun pruneMissingWidgets(liveAppWidgetIds: Set<Int>) {
        val stale = staleWidgetIds(prefs.all, liveAppWidgetIds)
        if (stale.isEmpty()) return
        val editor = prefs.edit()
        stale.forEach { widgetId ->
            editor.remove(key(widgetId)).remove(dayKey(widgetId)).remove(versionKey(widgetId))
        }
        editor.apply()
    }

    fun lastRefreshDayFor(appWidgetId: Int): Long? =
        if (prefs.contains(dayKey(appWidgetId))) prefs.getLong(dayKey(appWidgetId), 0L) else null

    fun markRefreshed(appWidgetId: Int, epochDay: Long) {
        prefs.edit().putLong(dayKey(appWidgetId), epochDay).apply()
    }

    fun requestRefresh(appWidgetId: Int) {
        val storedKey = versionKey(appWidgetId)
        prefs.edit().putLong(storedKey, prefs.getLong(storedKey, 0L) + 1).apply()
    }

    // This listener exists only while Glance is composing, not between daily refreshes.
    fun refreshRequests(appWidgetId: Int): Flow<Long> = callbackFlow {
        val storedKey = versionKey(appWidgetId)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == storedKey) trySend(prefs.getLong(storedKey, 0L))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getLong(storedKey, 0L))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    private fun dayKey(appWidgetId: Int) = DAY_KEY_PREFIX + appWidgetId

    private fun versionKey(appWidgetId: Int) = VERSION_KEY_PREFIX + appWidgetId

    private fun key(appWidgetId: Int) = BINDING_KEY_PREFIX + appWidgetId

    companion object {
        private const val PREFS_NAME = "widget_bindings"
    }
}

internal const val BINDING_KEY_PREFIX = "w_"
internal const val DAY_KEY_PREFIX = "day_"
internal const val VERSION_KEY_PREFIX = "version_"

internal fun widgetIdsBoundTo(bindings: Map<String, *>, eventId: String): List<Int> =
    bindings.entries.mapNotNull { (storedKey, value) ->
        if (!storedKey.startsWith(BINDING_KEY_PREFIX) || value != eventId) return@mapNotNull null
        storedKey.removePrefix(BINDING_KEY_PREFIX).toIntOrNull()
    }

internal fun widgetIdsInBindings(bindings: Map<String, *>): Set<Int> =
    bindings.keys.mapNotNull { storedKey ->
        when {
            storedKey.startsWith(BINDING_KEY_PREFIX) ->
                storedKey.removePrefix(BINDING_KEY_PREFIX).toIntOrNull()
            storedKey.startsWith(DAY_KEY_PREFIX) ->
                storedKey.removePrefix(DAY_KEY_PREFIX).toIntOrNull()
            storedKey.startsWith(VERSION_KEY_PREFIX) ->
                storedKey.removePrefix(VERSION_KEY_PREFIX).toIntOrNull()
            else -> null
        }
    }.toSet()

internal fun staleWidgetIds(bindings: Map<String, *>, liveAppWidgetIds: Set<Int>): Set<Int> =
    widgetIdsInBindings(bindings) - liveAppWidgetIds
