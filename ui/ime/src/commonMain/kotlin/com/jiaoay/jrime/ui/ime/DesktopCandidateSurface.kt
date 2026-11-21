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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.jiaoay.jrime.engine.AccentPreset
import com.jiaoay.jrime.engine.ImeSessionState
import com.jiaoay.jrime.engine.KeyboardCatalog
import com.jiaoay.jrime.engine.PaletteMode
import com.jiaoay.jrime.ui.design.ImeAccent
import com.jiaoay.jrime.ui.design.ImeDimens
import com.jiaoay.jrime.ui.design.ImePalette
import com.jiaoay.jrime.ui.design.ImeTypography
import com.jiaoay.jrime.ui.design.LocalImePalette

/**
 * The desktop candidate/status window content: composition echo plus the
 * shared candidate strip, without the on-screen keyboard (a physical keyboard
 * is the input). Same palette, tokens and candidate surface as Android, so a
 * desktop skin change lands everywhere.
 */
@Suppress("FunctionName")
@Composable
fun DesktopCandidateSurface(
    state: ImeSessionState,
    config: ImeSurfaceConfig,
    host: ImeSurfaceHost,
    catalog: KeyboardCatalog,
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
    val palette = remember(dark, accent) { ImePalette.of(dark).withAccent(accent.color(dark), accent.onColor(dark)) }
    CompositionLocalProvider(LocalImePalette provides palette) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .padding(horizontal = ImeDimens.PagePadding, vertical = ImeDimens.CandidateGap),
            ) {
                val echo = host.compositionEcho().ifEmpty { state.composition?.preedit.orEmpty() }
                if (echo.isNotEmpty()) {
                    BasicText(
                        text = echo,
                        style = ImeTypography.preeditStyle(palette.preeditText),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                CandidateStripView(
                    state = state,
                    config = config,
                    host = host,
                    modifier = Modifier.fillMaxWidth(),
                    emptyHint = true,
                )
            }
        }
    }
}
