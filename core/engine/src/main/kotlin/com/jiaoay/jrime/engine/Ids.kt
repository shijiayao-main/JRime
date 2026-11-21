/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.engine

import kotlin.math.abs

/** Stable identity of a keyboard layout. */
@JvmInline
value class KeyboardId(val value: String) {
    override fun toString(): String = value

    companion object {
        val Qwerty26 = KeyboardId("default")
        val Flyhe36 = KeyboardId("flyhe36")
        val Number = KeyboardId("number")
        val Phone = KeyboardId("phone")
        val Editor = KeyboardId("editor")
        val BasicSymbol = KeyboardId("basic_symbol")
        val MathSymbol = KeyboardId("math_symbol")
        val T9Symbol = KeyboardId("t9_symbol")
    }
}

/** Stable identity of a surface panel. */
@JvmInline
value class PanelId(val value: String) {
    override fun toString(): String = value

    companion object {
        val Clipboard = PanelId("clipboard")
        val Symbols = PanelId("symbols")
        val Emoji = PanelId("emoji")
        val Editor = PanelId("editor")
        val Phrase = PanelId("phrase")
    }
}
