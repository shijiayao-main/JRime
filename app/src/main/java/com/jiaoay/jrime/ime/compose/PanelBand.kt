/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jiaoay.jrime.data.prefs.AppPrefs

/** The row height the panel surfaces follow (same pref as the keyboard). */
@Composable
fun rememberKeyboardRowHeightDp(): Int {
    val keyboardPrefs = remember { AppPrefs.defaultInstance().keyboard }
    var rowHeightDp by remember { mutableIntStateOf(keyboardPrefs.rowHeight.getValue()) }
    DisposableEffect(keyboardPrefs) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == AppPrefs.Keyboard.KEYBOARD_HEIGHT) {
                    rowHeightDp = keyboardPrefs.rowHeight.getValue()
                }
            }
        keyboardPrefs.rowHeight.sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            keyboardPrefs.rowHeight.sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    return rowHeightDp
}
