/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic palette of the IME surface. Components read semantic roles, never
 * raw colors, so a palette swap restyles the whole surface consistently.
 *
 * Values in [Light] / [Dark] are the current Gboard-style scheme made
 * explicit; the accent presets in M2 will override [accent]/[accentText].
 */
@Immutable
class ImePalette(
    val surface: Color,
    val key: Color,
    val keyPressed: Color,
    val functionKey: Color,
    val keyText: Color,
    val keySymbol: Color,
    val enter: Color,
    val enterText: Color,
    val divider: Color,
    val candidateText: Color,
    val candidateComment: Color,
    val candidateHighlighted: Color,
    val candidateHighlightBg: Color,
    val preeditText: Color,
    val popupBackground: Color,
    val popupText: Color,
    val liquidBackground: Color,
    val accent: Color,
    val accentText: Color,
    val danger: Color,
    val hintText: Color,
) {
    companion object {
        val Light: ImePalette =
            ImePalette(
                surface = Color(0xFFF1F3F4),
                key = Color(0xFFFFFFFF),
                keyPressed = Color(0xFFDADCE0),
                functionKey = Color(0xFFE8EAED),
                keyText = Color(0xFF202124),
                keySymbol = Color(0xFF5F6368),
                enter = Color(0xFF1A73E8),
                enterText = Color(0xFFFFFFFF),
                divider = Color(0xFFDADCE0),
                candidateText = Color(0xFF202124),
                candidateComment = Color(0xFF5F6368),
                candidateHighlighted = Color(0xFF1A73E8),
                candidateHighlightBg = Color(0xFFE8F0FE),
                preeditText = Color(0xFF5F6368),
                popupBackground = Color(0xFFFFFFFF),
                popupText = Color(0xFF202124),
                liquidBackground = Color(0xFFF1F3F4),
                accent = Color(0xFF1A73E8),
                accentText = Color(0xFFFFFFFF),
                danger = Color(0xFFD93025),
                hintText = Color(0xFF5F6368),
            )

        val Dark: ImePalette =
            ImePalette(
                surface = Color(0xFF202124),
                key = Color(0xFF3C4043),
                keyPressed = Color(0xFF5F6368),
                functionKey = Color(0xFF303134),
                keyText = Color(0xFFE8EAED),
                keySymbol = Color(0xFF9AA0A6),
                enter = Color(0xFF8AB4F8),
                enterText = Color(0xFF202124),
                divider = Color(0xFF3C4043),
                candidateText = Color(0xFFE8EAED),
                candidateComment = Color(0xFF9AA0A6),
                candidateHighlighted = Color(0xFF8AB4F8),
                candidateHighlightBg = Color(0xFF303134),
                preeditText = Color(0xFF9AA0A6),
                popupBackground = Color(0xFF3C4043),
                popupText = Color(0xFFE8EAED),
                liquidBackground = Color(0xFF202124),
                accent = Color(0xFF8AB4F8),
                accentText = Color(0xFF202124),
                danger = Color(0xFFF28B82),
                hintText = Color(0xFF9AA0A6),
            )

        fun of(dark: Boolean): ImePalette = if (dark) Dark else Light
    }
}

/** The palette the IME surface reads; provided once at the surface root. */
val LocalImePalette = staticCompositionLocalOf<ImePalette> {
    error("LocalImePalette not provided")
}
