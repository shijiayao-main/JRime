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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.jiaoay.jrime.data.theme.ColorManager
import com.jiaoay.jrime.data.theme.ThemeColors

/**
 * The Compose IME's palette: one [ImeColors] instance built from the active
 * color scheme the moment the composition reads it. Both renderers -- this
 * Compose tree and the legacy tree it replaces -- draw from the same
 * [ColorManager] scheme, so day/night flips restyle both identically.
 *
 * The instance is provided through [LocalImeColors] at [ImeRoot]; a scheme
 * switch bumps [ImeUiState.colorsRevision] and the next composition reads the
 * new table.
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

/** The palette the Compose tree reads; provided by [ImeRoot]. */
val LocalImeColors = staticCompositionLocalOf<ImeColors> { error("ImeColors not provided") }

/**
 * Reads the active scheme as Compose state. The revision read is the
 * recomposition trigger; the scope itself is guaranteed to exist by the time
 * this runs (the input view is only created after the theme attached, so the
 * scheme chain has resolved).
 */
@Composable
fun rememberImeColors(state: ImeUiState): ImeColors {
    val revision = state.colorsRevision
    return remember(revision) { ImeColors(ColorManager.currentScope()!!.colors) }
}

/** The Gboard key-face styles derived from the active palette. */
@Immutable
class GboardStyle
internal constructor(
    colors: ImeColors,
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
        )
    val candidateStyle =
        TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            color = colors.candidateText,
        )
    val candidateHighlightedStyle =
        TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.candidateHighlighted,
        )
    val candidateCommentStyle =
        TextStyle(
            fontSize = 11.sp,
            color = colors.candidateComment,
        )
}

@Composable
fun gboardStyle(): GboardStyle {
    val colors = LocalImeColors.current
    return remember(colors) { GboardStyle(colors) }
}
