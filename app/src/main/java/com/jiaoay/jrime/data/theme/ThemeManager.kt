/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.data.theme

import android.content.res.Configuration
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.jiaoay.jrime.data.prefs.AppPrefs
import com.jiaoay.jrime.theme.NativeTheme
import com.jiaoay.jrime.util.appContext

/**
 * The theme source after the source flip (task 1.4): [NativeTheme] is the one
 * and only producer, hardcoded and immutable. No file watching, no reloading,
 * no picking, no change listeners -- the theme cannot change at runtime, so
 * nothing needs to react to it. What remains is the [prefs] the IME still
 * reads (scheme and navigation-bar settings) and the direct-write-storage
 * sync of the preference block.
 */
object ThemeManager {
    val activeTheme: Theme
        get() = NativeTheme.theme

    val prefs = AppPrefs.defaultInstance().registerProvider(::ThemePrefs)

    fun init(configuration: Configuration) {
        // The scope the whole IME reads (views, colors) is born here: the
        // old applyTheme path died with the switcher, so this is the one
        // place the hardcoded theme gets attached.
        ColorManager.attachTheme(activeTheme)
        ColorManager.init(configuration)
    }

    fun syncToDeviceEncryptedStorage() {
        val ctx = appContext.createDeviceProtectedStorageContext()
        val sp = PreferenceManager.getDefaultSharedPreferences(ctx)
        sp.edit {
            prefs.preferenceDelegates.forEach {
                it.value.putValueTo(this@edit)
            }
        }
    }
}
