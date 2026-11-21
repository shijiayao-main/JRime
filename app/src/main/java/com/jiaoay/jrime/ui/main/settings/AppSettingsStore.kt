/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.main.settings

import android.content.SharedPreferences
import com.jiaoay.jrime.ui.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The app side of the settings seam: raw reads/writes over the default
 * SharedPreferences (the same store the preference delegates use), with a
 * revision counter so the Compose hub re-reads values after every change.
 *
 * Enums travel as their serialized names, which is exactly how the delegate
 * layer persists them, so the hub can write them without knowing the types.
 */
class AppSettingsStore(
    private val prefs: SharedPreferences,
) : SettingsStore {
    private val _revision = MutableStateFlow(0)
    override val revision: StateFlow<Int> = _revision.asStateFlow()

    private val listener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            _revision.value = _revision.value + 1
        }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun dispose() {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    override fun bool(
        key: String,
        default: Boolean,
    ): Boolean = prefs.getBoolean(key, default)

    override fun setBool(
        key: String,
        value: Boolean,
    ) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun int(
        key: String,
        default: Int,
    ): Int = prefs.getInt(key, default)

    override fun setInt(
        key: String,
        value: Int,
    ) {
        prefs.edit().putInt(key, value).apply()
    }

    override fun string(
        key: String,
        default: String,
    ): String = prefs.getString(key, default) ?: default

    override fun setString(
        key: String,
        value: String,
    ) {
        prefs.edit().putString(key, value).apply()
    }
}
