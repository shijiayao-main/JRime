package com.jiaoay.jrime.desktop.settings

import com.jiaoay.jrime.ui.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/** A [SettingsStore] over the properties file; revision bumps on every write. */
class DesktopSettingsStore : SettingsStore {
    private val values = ConcurrentHashMap(DesktopSettings.read())
    private val _revision = MutableStateFlow(0)
    override val revision: StateFlow<Int> = _revision.asStateFlow()

    override fun bool(key: String, default: Boolean): Boolean = (values[key] ?: default.toString()).toBooleanStrictOrNull() ?: default

    override fun setBool(key: String, value: Boolean) = put(key, value.toString())

    override fun int(key: String, default: Int): Int = (values[key] ?: default.toString()).toIntOrNull() ?: default

    override fun setInt(key: String, value: Int) = put(key, value.toString())

    override fun string(key: String, default: String): String = values[key] ?: default

    override fun setString(key: String, value: String) = put(key, value)

    private fun put(key: String, value: String) {
        values[key] = value
        runCatching { DesktopSettings.write(values) }
        _revision.value = _revision.value + 1
    }
}
