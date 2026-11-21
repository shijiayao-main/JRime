/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.design

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jiaoay.jrime.engine.ImeMetrics

/**
 * The single source of keyboard surface metrics. Every size in the IME UI
 * must come from here; components are not allowed to carry `.dp` literals
 * (guarded by tests). Values are the M1 targets: the current Compose tree's
 * numbers made explicit, to be tuned by device acceptance in M2.
 */
object ImeDimens {
    /** Gap between keys and between rows. */
    val KeyGap: Dp = 6.dp

    /** Horizontal padding of the keyboard page. */
    val PagePadding: Dp = 6.dp

    /** Padding inside panels (clipboard, symbols, emoji). */
    val PanelPadding: Dp = 12.dp

    /** Height of the top action bar. */
    val TopBarHeight: Dp = 40.dp

    /** Height of one candidate chip. */
    val CandidateHeight: Dp = 36.dp

    /** Minimum width of one candidate chip. */
    val CandidateMinWidth: Dp = 44.dp

    /** Horizontal gap between candidate chips. */
    val CandidateGap: Dp = 6.dp

    /** Padding under the keyboard content band (home indicator breathing room). */
    val BottomPadding: Dp = 12.dp

    /** Corner radius of a regular key. */
    val KeyCorner: Dp = 8.dp

    /** Corner radius of a panel container. */
    val PanelCorner: Dp = 24.dp

    /** Corner radius of a candidate chip. */
    val ChipCorner: Dp = 16.dp

    /** Minimum touch target. */
    val MinTouchTarget: Dp = 40.dp

    /** Icon size in the top bar / candidate arrows (single-line iconography). */
    val IconSmall: Dp = 18.dp

    /** Category icon size on the settings landing cards. */
    val IconCategory: Dp = 22.dp

    /** Row height bounds and default; user preference may override within bounds. */
    val RowHeightMin: Dp = ImeMetrics.ROW_HEIGHT_MIN_DP.dp
    val RowHeightMax: Dp = ImeMetrics.ROW_HEIGHT_MAX_DP.dp
    val RowHeightDefault: Dp = ImeMetrics.ROW_HEIGHT_DEFAULT_DP.dp

    /** Number of content rows the keyboard band reserves (panels reuse it). */
    const val CONTENT_ROWS: Int = ImeMetrics.CONTENT_ROWS

    /** Default page size of the candidate strip. */
    const val CANDIDATE_PAGE_SIZE: Int = ImeMetrics.CANDIDATE_PAGE_SIZE
}
