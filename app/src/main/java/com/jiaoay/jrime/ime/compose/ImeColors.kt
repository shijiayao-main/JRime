/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.jiaoay.jrime.data.theme.ImeFonts
import com.jiaoay.jrime.data.theme.ThemeColors

/**
 * The legacy Compose palette still consumed by the M1 panel slot content
 * (clipboard chip/panel, liquid pages) until those panels move to the shared
 * [com.jiaoay.jrime.ui.design.ImePalette]. The new surface never reads it.
 */
@Immutable
class ImeColors
internal constructor(
    val keyboardBackground: Color,
    val keyBackground: Color,
    val keyText: Color,
    val keySymbol: Color,
    val functionKey: Color,
    val pressedKey: Color,
    val enter: Color,
    val enterText: Color,
    val stripBackground: Color,
    val stripDivider: Color,
    val candidateText: Color,
    val candidateHighlighted: Color,
    val candidateComment: Color,
    val activeToggle: Color,
    val activeToggleLabel: Color,
    val liquidBackground: Color,
) {
    constructor(t: ThemeColors) : this(
        keyboardBackground = Color(t.keyboardBackColor),
        keyBackground = Color(t.keyBackColor),
        keyText = Color(t.keyTextColor),
        keySymbol = Color(t.keySymbolColor),
        functionKey = Color(t.offKeyBackColor),
        pressedKey = Color(t.hilitedKeyBackColor),
        enter = Color(t.onKeyBackColor),
        enterText = Color(t.onKeyTextColor),
        stripBackground = Color(t.backColor),
        stripDivider = Color(t.candidateSeparatorColor),
        candidateText = Color(t.candidateTextColor),
        candidateHighlighted = Color(t.hilitedCandidateTextColor),
        candidateComment = Color(t.commentTextColor),
        activeToggle = Color(t.onKeyBackColor),
        activeToggleLabel = Color(t.onKeyTextColor),
        liquidBackground = Color(t.liquidKeyboardBackground),
    )
}

/** The palette the legacy panel content reads; provided by the host slot. */
val LocalImeColors = staticCompositionLocalOf<ImeColors> { error("ImeColors not provided") }

/** The Gboard key-face styles derived from the active palette. */
@Immutable
class GboardStyle
internal constructor(
    colors: ImeColors,
    family: FontFamily? = null,
) {
    val board = colors.keyboardBackground
    val letter = colors.keyBackground
    val function = colors.functionKey
    val pressed = colors.pressedKey
    val enter = colors.enter
    val enterText = colors.enterText
    val text = colors.keyText
    val textVariant = colors.keySymbol
    val highlight = colors.activeToggle
    val highlightText = colors.activeToggleLabel
    val hintStyle =
        TextStyle(
            fontSize = 9.sp,
            color = textVariant,
            textAlign = TextAlign.Center,
            fontFamily = family,
        )
    val candidateStyle =
        TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            color = colors.candidateText,
            fontFamily = family,
        )
    val candidateHighlightedStyle =
        TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.candidateHighlighted,
            fontFamily = family,
        )
    val candidateCommentStyle =
        TextStyle(
            fontSize = 11.sp,
            color = colors.candidateComment,
            fontFamily = family,
        )
    val preeditStyle =
        TextStyle(
            fontSize = 16.sp,
            color = colors.candidateComment,
            fontFamily = family,
        )
}

@Composable
fun gboardStyle(): GboardStyle {
    val colors = LocalImeColors.current
    val context = LocalContext.current
    // The font chain rides under every style: it resolves per palette, but
    // the typeface itself is cached in ImeFonts on the directory signature,
    // so a day/night flip never re-reads the chained files.
    return remember(colors) { GboardStyle(colors, FontFamily(ImeFonts.currentTypeface(context))) }
}
