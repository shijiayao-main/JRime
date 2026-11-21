/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.design

import androidx.compose.ui.graphics.Color

/** The accent presets the surface offers; keys/candidates/enter follow them. */
enum class ImeAccent(
    val light: Color,
    val lightOn: Color,
    val dark: Color,
    val darkOn: Color,
) {
    Blue(
        light = Color(0xFF1A73E8),
        lightOn = Color(0xFFFFFFFF),
        dark = Color(0xFF8AB4F8),
        darkOn = Color(0xFF202124),
    ),
    Green(
        light = Color(0xFF0F9D58),
        lightOn = Color(0xFFFFFFFF),
        dark = Color(0xFF81C995),
        darkOn = Color(0xFF202124),
    ),
    Purple(
        light = Color(0xFF7B1FA2),
        lightOn = Color(0xFFFFFFFF),
        dark = Color(0xFFD7AEFB),
        darkOn = Color(0xFF202124),
    ),
    ;

    fun color(dark: Boolean): Color = if (dark) this.dark else this.light

    fun onColor(dark: Boolean): Color = if (dark) this.darkOn else this.lightOn
}
