package com.datecount.widget.data

import android.content.SharedPreferences

/** In-memory SharedPreferences so the binding store can be exercised on the JVM. */
class FakeSharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()

    override fun getString(key: String?, defValue: String?): String? =
        values[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        values[key] as? MutableSet<String> ?: defValues

    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        values[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) {
        listener?.let { listeners += it }
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) {
        listener?.let { listeners -= it }
    }

    private inner class FakeEditor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor =
            stage(key, value)

        override fun putStringSet(
            key: String?,
            values: MutableSet<String>?
        ): SharedPreferences.Editor = stage(key, values)

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = stage(key, value)

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = stage(key, value)

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor =
            stage(key, value)

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor =
            stage(key, value)

        override fun remove(key: String?): SharedPreferences.Editor {
            key?.let { removals += it }
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearAll = true
            pending.clear()
            removals.clear()
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearAll) {
                values.clear()
                clearAll = false
            }
            removals.forEach { values.remove(it) }
            pending.forEach { (key, value) -> values[key] = value }
            removals.clear()
            pending.clear()
        }

        private fun stage(key: String?, value: Any?): SharedPreferences.Editor {
            key?.let { pending[it] = value }
            return this
        }
    }
}
