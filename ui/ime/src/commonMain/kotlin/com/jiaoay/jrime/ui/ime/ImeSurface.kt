/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.ime

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import com.jiaoay.jrime.engine.AccentPreset
import com.jiaoay.jrime.engine.ImeSessionState
import com.jiaoay.jrime.engine.KeyAppearance
import com.jiaoay.jrime.engine.KeyboardCatalog
import com.jiaoay.jrime.engine.KeyboardLayoutOps
import com.jiaoay.jrime.engine.PaletteMode
import com.jiaoay.jrime.ui.design.ImeAccent
import com.jiaoay.jrime.ui.design.ImeDimens
import com.jiaoay.jrime.ui.design.ImePalette
import com.jiaoay.jrime.ui.design.LocalImePalette

/**
 * The shared IME surface: a bottom-anchored band carrying the top bar and the
 * active keyboard or panel. Platform-neutral — every platform action goes
 * through [ImeSurfaceHost], every visual value through the design tokens.
 *
 * The band is one constant-height composition: the top bar and the six-row
 * content band never move between composing/idle or keyboard/panel, which is
 * the no-jump contract the current keyboard already holds.
 */
@Suppress("FunctionName")
@Composable
fun ImeSurface(
    state: ImeSessionState,
    config: ImeSurfaceConfig,
    catalog: KeyboardCatalog,
    host: ImeSurfaceHost,
    modifier: Modifier = Modifier,
) {
    val dark =
        when (state.paletteMode) {
            PaletteMode.FollowSystem -> isSystemInDarkTheme()
            PaletteMode.ForceLight -> false
            PaletteMode.ForceDark -> true
        }
    val accent =
        when (state.accent) {
            AccentPreset.Blue -> ImeAccent.Blue
            AccentPreset.Green -> ImeAccent.Green
            AccentPreset.Purple -> ImeAccent.Purple
        }
    val palette =
        remember(dark, accent) {
            ImePalette.of(dark).withAccent(accent.color(dark), accent.onColor(dark))
        }
    val board = catalog.board(state.keyboardId)
    val qwerty =
        remember(board, catalog.presetKeys) {
            KeyboardLayoutOps.isQwertyBoard(board, catalog.presetKeys)
        }
    val candidatesInKeyboardRow =
        qwerty && KeyboardLayoutOps.hasLeadingDigitRow(board, catalog.presetKeys)
    // The content band's row count: the Gboard structure always renders five
    // rows (digit, two letter rows, shift row, chrome row); the full-data
    // boards render one row per folded row. The band height is derived from
    // this count, so panels and keyboards always agree on it.
    val contentRows =
        remember(board, catalog.presetKeys) {
            if (qwerty) {
                GBOARD_CONTENT_ROWS
            } else {
                KeyboardLayoutOps.foldRows(board).size.coerceAtLeast(1)
            }
        }
    val appearance =
        remember(state.composing, state.candidatePageCount, state.asciiMode, state.composition?.preedit) {
            KeyAppearance(
                composing = state.composing,
                paging = state.candidatePageCount > 1,
                auxActive = state.composition?.preedit?.contains('/') == true,
                isAsciiMode = state.asciiMode,
            )
        }
    CompositionLocalProvider(LocalImePalette provides palette) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .navigationBarsPadding()
                    .padding(bottom = ImeDimens.BottomPadding)
                    .onGloballyPositioned { coordinates ->
                        host.reportBandTop(coordinates.positionInWindow().y.toInt())
                    },
            ) {
                SurfaceTopBar(
                    state = state,
                    config = config,
                    host = host,
                    candidatesInKeyboardRow = candidatesInKeyboardRow,
                )
                ImePanelHost(
                    state = state,
                    config = config,
                    host = host,
                    contentRows = contentRows,
                    keyboard = {
                        KeyboardSurface(
                            state = state,
                            config = config,
                            catalog = catalog,
                            host = host,
                            appearance = appearance,
                            composingContent = {
                                CandidateStripView(
                                    state = state,
                                    config = config,
                                    host = host,
                                    modifier = Modifier.fillMaxSize(),
                                    emptyHint = true,
                                )
                            },
                        )
                    },
                )
            }
        }
    }
}
