/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.design

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The single source of IME text sizes. Components build their [TextStyle]s
 * from these values (and palette colors) instead of carrying `sp` literals.
 */
object ImeTypography {
    val KeyLetter = 19.sp
    val KeySymbol = 14.sp
    val KeyHint = 9.sp
    val Candidate = 18.sp
    val CandidateComment = 11.sp
    val Preedit = 16.sp
    val TopBar = 14.sp
    val PanelTitle = 16.sp
    val PanelItem = 14.sp
    val Badge = 9.sp

    /** Range allowed for the user-adjustable candidate text size. */
    val CandidateMin = 15.sp
    val CandidateMax = 22.sp

    fun keyLetterStyle(
        color: androidx.compose.ui.graphics.Color,
        family: FontFamily? = null,
    ) = TextStyle(fontSize = KeyLetter, color = color, fontFamily = family)

    fun keySymbolStyle(
        color: androidx.compose.ui.graphics.Color,
        family: FontFamily? = null,
    ) = TextStyle(fontSize = KeySymbol, color = color, fontFamily = family)

    fun keyHintStyle(
        color: androidx.compose.ui.graphics.Color,
        family: FontFamily? = null,
    ) = TextStyle(fontSize = KeyHint, color = color, fontFamily = family)

    fun candidateStyle(
        color: androidx.compose.ui.graphics.Color,
        size: androidx.compose.ui.unit.TextUnit = Candidate,
        family: FontFamily? = null,
    ) = TextStyle(fontSize = size, color = color, fontFamily = family)

    fun candidateCommentStyle(
        color: androidx.compose.ui.graphics.Color,
        family: FontFamily? = null,
    ) = TextStyle(fontSize = CandidateComment, color = color, fontFamily = family)

    fun preeditStyle(
        color: androidx.compose.ui.graphics.Color,
        family: FontFamily? = null,
    ) = TextStyle(fontSize = Preedit, color = color, fontFamily = family)

    fun topBarStyle(
        color: androidx.compose.ui.graphics.Color,
        family: FontFamily? = null,
    ) = TextStyle(fontSize = TopBar, fontWeight = FontWeight.Medium, color = color, fontFamily = family)
}
