/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.ime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.jiaoay.jrime.data.theme.model.KeyActionToken
import com.jiaoay.jrime.engine.ImeCommand
import com.jiaoay.jrime.engine.ImeSessionState
import com.jiaoay.jrime.ui.design.ImeDimens
import com.jiaoay.jrime.ui.design.ImeShapes
import com.jiaoay.jrime.ui.design.ImeTypography
import com.jiaoay.jrime.ui.design.LocalImePalette

/** App-command tokens the bar dispatches through the shared [ImeCommand.Key] path. */
private const val ASCII_SWITCH_TOKEN: String = "ascii_switch"
private const val ESCAPE_TOKEN: String = "Escape"

/** Glyphs the shared surface owns; the module carries no string resources. */
private const val CJK_LABEL: String = "中"
private const val EN_LABEL: String = "EN"
private const val BACK_TO_KEYBOARD_GLYPH: String = "⌨"
private const val HIDE_GLYPH: String = "▼"

/** Chip height: the bar minus one token gap of breathing room. */
private val TopBarChipHeight: Dp = ImeDimens.TopBarHeight - ImeDimens.KeyGap

/** Chip min width: one gap under the touch-target token. */
private val TopBarChipMinWidth: Dp = ImeDimens.MinTouchTarget - ImeDimens.KeyGap

/** Hairline separators: a sixth of the gap token (one dp). */
private val TopBarHairline: Dp = ImeDimens.KeyGap / 6

/** Divider height: the bar's inner band. */
private val TopBarDividerHeight: Dp = ImeDimens.TopBarHeight / 2

/**
 * The permanent top bar above the content band: a constant-height row whose
 * content follows the session state.
 *
 * - idle: the app's toolbar actions, the quick-paste chip, the CJK/EN toggle
 *   and the hide key;
 * - composing: the code echo (tap it to cancel the composition), a hairline
 *   and then the candidate strip, unless the keyboard hosts the strip in its
 *   own digit-row slot;
 * - panel open: just the way back to the keyboard and the language/hide pair.
 *
 * Buttons only dispatch [ImeCommand]s; key feedback (sound/haptic/TTS) is the
 * app executor's job, so the shared surface stays silent.
 */
@Suppress("FunctionName")
@Composable
fun SurfaceTopBar(
    state: ImeSessionState,
    config: ImeSurfaceConfig,
    host: ImeSurfaceHost,
    candidatesInKeyboardRow: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalImePalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ImeDimens.TopBarHeight)
            .background(palette.surface)
            .padding(horizontal = ImeDimens.PagePadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            state.activePanel != null ->
                PanelTopBar(state = state, host = host)

            state.composing ->
                ComposingTopBar(
                    state = state,
                    config = config,
                    host = host,
                    candidatesInKeyboardRow = candidatesInKeyboardRow,
                )

            else ->
                IdleTopBar(state = state, config = config, host = host)
        }
    }
}

/**
 * The idle menu row: the app-configured actions in order, the quick-paste chip
 * flexing over the free space, then the language toggle and the hide key on
 * the right.
 */
@Suppress("FunctionName")
@Composable
private fun RowScope.IdleTopBar(
    state: ImeSessionState,
    config: ImeSurfaceConfig,
    host: ImeSurfaceHost,
) {
    config.toolbarActions
        .filter { it.visible }
        .forEach { action ->
            TopBarChip(
                label = action.label,
                highlighted = action.highlighted,
                onClick = { host.dispatch(action.command) },
            )
        }
    config.clipboardChip?.let { clipboardChip ->
        // The latest clip is a one-tap paste; it yields on narrow bands
        // instead of squeezing the fixed-size chips off the right edge.
        Box(
            modifier = Modifier.weight(2f, fill = false),
            contentAlignment = Alignment.CenterStart,
        ) {
            clipboardChip()
        }
    }
    Spacer(modifier = Modifier.weight(1f))
    TopBarLanguageAndHide(state = state, host = host)
}

/** The composing face: code echo, hairline, then the strip or blank space. */
@Suppress("FunctionName")
@Composable
private fun RowScope.ComposingTopBar(
    state: ImeSessionState,
    config: ImeSurfaceConfig,
    host: ImeSurfaceHost,
    candidatesInKeyboardRow: Boolean,
) {
    val palette = LocalImePalette.current
    // The host may override the echo (vendor ROMs); fall back to the preedit.
    val echo = host.compositionEcho().ifEmpty { state.composition?.preedit.orEmpty() }
    if (echo.isNotEmpty()) {
        BasicText(
            text = echo,
            style = ImeTypography.preeditStyle(palette.preeditText),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clickable {
                    // Tapping the echoed code cancels the composition, the
                    // same Esc affordance the old action bar exposed.
                    host.dispatch(ImeCommand.Key(KeyActionToken.Plain(ESCAPE_TOKEN)))
                }
                .padding(end = ImeDimens.KeyGap),
        )
        TopBarDivider()
    }
    if (candidatesInKeyboardRow) {
        // The keyboard's digit-row slot carries the strip; the echo owns the bar.
        Spacer(modifier = Modifier.weight(1f))
    } else {
        CandidateStripView(
            state = state,
            config = config,
            host = host,
            modifier = Modifier.weight(1f),
            emptyHint = true,
        )
    }
    TopBarLanguageAndHide(state = state, host = host)
}

/** The panel-open face: a way back to the keyboard plus the persistent pair. */
@Suppress("FunctionName")
@Composable
private fun RowScope.PanelTopBar(
    state: ImeSessionState,
    host: ImeSurfaceHost,
) {
    TopBarChip(
        label = BACK_TO_KEYBOARD_GLYPH,
        onClick = { host.dispatch(ImeCommand.ClosePanel) },
    )
    Spacer(modifier = Modifier.weight(1f))
    TopBarLanguageAndHide(state = state, host = host)
}

/** The language toggle (accent while ASCII/EN is on) and the hide key. */
@Suppress("FunctionName")
@Composable
private fun TopBarLanguageAndHide(
    state: ImeSessionState,
    host: ImeSurfaceHost,
) {
    TopBarChip(
        label = if (state.asciiMode) EN_LABEL else CJK_LABEL,
        highlighted = state.asciiMode,
        onClick = { host.dispatch(ImeCommand.Key(KeyActionToken.Plain(ASCII_SWITCH_TOKEN))) },
    )
    TopBarChip(
        label = HIDE_GLYPH,
        onClick = { host.dispatch(ImeCommand.HideKeyboard) },
    )
}

/** One flat action chip: a function-key box that turns accent when highlighted. */
@Suppress("FunctionName")
@Composable
private fun TopBarChip(
    label: String,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    val palette = LocalImePalette.current
    Box(
        modifier = Modifier
            .padding(horizontal = ImeDimens.KeyGap / 2)
            .height(TopBarChipHeight)
            .widthIn(min = TopBarChipMinWidth)
            .clip(ImeShapes.Key)
            .background(if (highlighted) palette.accent else palette.functionKey)
            .clickable(onClick = onClick)
            .padding(horizontal = ImeDimens.KeyGap),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = ImeTypography.topBarStyle(color = if (highlighted) palette.accentText else palette.keyText),
            maxLines = 1,
        )
    }
}

/** The hairline between the composing code echo and the candidate strip. */
@Suppress("FunctionName")
@Composable
private fun TopBarDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = ImeDimens.KeyGap / 2)
            .width(TopBarHairline)
            .height(TopBarDividerHeight)
            .background(LocalImePalette.current.divider),
    )
}
