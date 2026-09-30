package com.datecount.widget.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class WidgetBindingStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

    fun removeEvent(eventId: String) {
        val editor = prefs.edit()
        prefs.all.forEach { (storedKey, value) ->
            if (storedKey.startsWith(KEY_PREFIX) && value == eventId) editor.remove(storedKey)
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

    private fun dayKey(appWidgetId: Int) = "day_" + appWidgetId

    private fun versionKey(appWidgetId: Int) = "version_" + appWidgetId

    private fun key(appWidgetId: Int) = KEY_PREFIX + appWidgetId

    companion object {
        private const val PREFS_NAME = "widget_bindings"
        private const val KEY_PREFIX = "w_"
    }
}
