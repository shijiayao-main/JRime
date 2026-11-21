/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.ime

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jiaoay.jrime.data.theme.model.PresetKey
import com.jiaoay.jrime.data.theme.model.TextKeyboard
import com.jiaoay.jrime.engine.ImeSessionState
import com.jiaoay.jrime.engine.KeyAppearance
import com.jiaoay.jrime.engine.KeyboardCatalog
import com.jiaoay.jrime.engine.KeyboardLayoutOps
import com.jiaoay.jrime.data.theme.model.KeyActionToken as LegacyKeyToken

/*
 * The keyboard surface: the data-driven renderer that replaced the old
 * GboardKeyboard/KeyboardPanel pair. It owns row *structure* only -- the key
 * face and every gesture slot live in KeySurface.
 *
 * The Gboard-structured boards (the qwerty-like ones) render a fixed
 * five-row grid measured in the FlorisBoard unit grid: the data digit row,
 * the two leading letter rows, the shift-flanked last letter row and the
 * hardcoded function row. Every row stretches with `weight(1f)`, so the
 * surface always fills the host band and switching to the composing state
 * (digit row -> candidate strip) never moves the band. Boards that don't fit
 * that structure (number/symbol/editor pages) render all of their folded rows
 * with the row-internal weights the legacy percentage model carried.
 */

/** The function row's edge slots (symbol/comma/period/enter) in units. */
private const val CHROME_EDGE_WEIGHT = 15f

/** The function row's space bar in units. */
private const val CHROME_SPACE_WEIGHT = 40f

/**
 * The shared keyboard surface. Picks the Gboard structure when the catalog's
 * board fits it and the full-data fold otherwise; both paths fill the host
 * band and delegate every key to [KeySurface].
 */
@Suppress("FunctionName")
@Composable
fun KeyboardSurface(
    state: ImeSessionState,
    config: ImeSurfaceConfig,
    catalog: KeyboardCatalog,
    host: ImeSurfaceHost,
    appearance: KeyAppearance,
    modifier: Modifier = Modifier,
    composingContent: (@Composable () -> Unit)? = null,
) {
    val board = remember(catalog, state.keyboardId) { catalog.board(state.keyboardId) }
    val qwerty =
        remember(board, catalog.presetKeys) {
            KeyboardLayoutOps.isQwertyBoard(board, catalog.presetKeys)
        }
    if (qwerty) {
        QwertyKeyboard(
            board = board,
            state = state,
            config = config,
            presetKeys = catalog.presetKeys,
            host = host,
            appearance = appearance,
            composingContent = composingContent,
            modifier = modifier,
        )
    } else {
        FoldedKeyboard(
            board = board,
            state = state,
            config = config,
            presetKeys = catalog.presetKeys,
            host = host,
            appearance = appearance,
            modifier = modifier,
        )
    }
}

/**
 * The Gboard structure: digit row, two letter rows, the shift row and the
 * function row. The digit row hands its pinned slot to the candidate strip
 * while composing (the height never changes).
 */
@Suppress("FunctionName")
@Composable
private fun QwertyKeyboard(
    board: TextKeyboard,
    state: ImeSessionState,
    config: ImeSurfaceConfig,
    presetKeys: Map<String, PresetKey>,
    host: ImeSurfaceHost,
    appearance: KeyAppearance,
    composingContent: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
    val (digitRow, letterRows) =
        remember(board, presetKeys) {
            KeyboardLayoutOps.splitDigitRow(KeyboardLayoutOps.gboardLetterRows(board, presetKeys))
        }
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (appearance.composing && composingContent != null && digitRow != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    composingContent()
                }
            } else if (digitRow != null) {
                LetterRow(
                    keys = digitRow,
                    state = state,
                    config = config,
                    presetKeys = presetKeys,
                    host = host,
                    appearance = appearance,
                )
            }
        }
        letterRows.dropLast(1).forEach { row ->
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                LetterRow(
                    keys = row,
                    state = state,
                    config = config,
                    presetKeys = presetKeys,
                    host = host,
                    appearance = appearance,
                )
            }
        }
        letterRows.lastOrNull()?.let { row ->
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                ShiftRow(
                    board = board,
                    letters = row,
                    state = state,
                    config = config,
                    presetKeys = presetKeys,
                    host = host,
                    appearance = appearance,
                )
            }
        }
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            ChromeRow(
                board = board,
                state = state,
                config = config,
                presetKeys = presetKeys,
                host = host,
                appearance = appearance,
            )
        }
    }
}

/**
 * The full-data fold: every row of the board renders with its own inline
 * weights, each row stretching equally. Used by the number/symbol/editor
 * boards that don't fit the letter structure.
 */
@Suppress("FunctionName")
@Composable
private fun FoldedKeyboard(
    board: TextKeyboard,
    state: ImeSessionState,
    config: ImeSurfaceConfig,
    presetKeys: Map<String, PresetKey>,
    host: ImeSurfaceHost,
    appearance: KeyAppearance,
    modifier: Modifier,
) {
    val rows = remember(board) { KeyboardLayoutOps.foldRows(board) }
    Column(modifier = modifier.fillMaxSize()) {
        rows.forEach { row ->
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                row.forEach { entry ->
                    KeySurface(
                        key = entry.key,
                        presetKeys = presetKeys,
                        appearance = appearance,
                        config = config,
                        schemaLabel = state.schemaLabel,
                        host = host,
                        modifier = Modifier.weight(if (entry.weight > 0f) entry.weight else 1f),
                    )
                }
            }
        }
    }
}

/** A pure letter row, centered when it doesn't span the full unit grid. */
@Suppress("FunctionName")
@Composable
private fun RowScope.LetterRow(
    keys: List<TextKeyboard.TextKey>,
    state: ImeSessionState,
    config: ImeSurfaceConfig,
    presetKeys: Map<String, PresetKey>,
    host: ImeSurfaceHost,
    appearance: KeyAppearance,
) {
    val inset = KeyboardLayoutOps.rowInsetUnits(keys.size)
    if (inset > 0f) {
        Spacer(modifier = Modifier.weight(inset))
    }
    keys.forEach { key ->
        KeySurface(
            key = key,
            presetKeys = presetKeys,
            appearance = appearance,
            config = config,
            schemaLabel = state.schemaLabel,
            host = host,
            modifier = Modifier.weight(1f),
        )
    }
    if (inset > 0f) {
        Spacer(modifier = Modifier.weight(inset))
    }
}

/** The last letter row with its chrome flanks: [⇧/辅助] letters... [⌫]. */
@Suppress("FunctionName")
@Composable
private fun RowScope.ShiftRow(
    board: TextKeyboard,
    letters: List<TextKeyboard.TextKey>,
    state: ImeSessionState,
    config: ImeSurfaceConfig,
    presetKeys: Map<String, PresetKey>,
    host: ImeSurfaceHost,
    appearance: KeyAppearance,
) {
    // The dedicated aux-code key: composing it sends the separator, ASCII it
    // keeps the plain shift. The data key has no click slot, so it is located
    // by its ascii slot instead of the usual token lookup.
    val auxKey =
        remember(board) {
            board.keys.firstOrNull {
                it.click == null && it.ascii == LegacyKeyToken.Plain("Shift_L")
            }
        }
    val shiftKey = remember { TextKeyboard.TextKey(click = LegacyKeyToken.Plain("Shift_L")) }
    val shiftLabel = if (appearance.isAsciiMode) "⇧" else "辅助"
    KeySurface(
        key = auxKey ?: shiftKey,
        presetKeys = presetKeys,
        appearance = appearance,
        config = config,
        schemaLabel = state.schemaLabel,
        host = host,
        modifier = Modifier.weight(KeyboardLayoutOps.SHIFT_UNITS),
        labelOverride = if (auxKey != null) shiftLabel else "⇧",
        isFunctionOverride = true,
        highlight = appearance.isAsciiMode,
    )
    letters.forEach { key ->
        KeySurface(
            key = key,
            presetKeys = presetKeys,
            appearance = appearance,
            config = config,
            schemaLabel = state.schemaLabel,
            host = host,
            modifier = Modifier.weight(1f),
        )
    }
    // The board's own backspace data key keeps its long-press/swipe slots.
    val backspaceKey = remember(board) { board.keyForToken("BackSpace") }
    val fallbackBackspace = remember { TextKeyboard.TextKey(click = LegacyKeyToken.Plain("BackSpace")) }
    KeySurface(
        key = backspaceKey ?: fallbackBackspace,
        presetKeys = presetKeys,
        appearance = appearance,
        config = config,
        schemaLabel = state.schemaLabel,
        host = host,
        modifier = Modifier.weight(KeyboardLayoutOps.backspaceUnits(letters.size)),
    )
}

/**
 * The hardcoded function row: symbol, comma, space, period, enter. The
 * geometry lives here (app-owned), but every slot still resolves the board's
 * own data key so long-press/swipe behaviours survive the redesign.
 */
@Suppress("FunctionName")
@Composable
private fun RowScope.ChromeRow(
    board: TextKeyboard,
    state: ImeSessionState,
    config: ImeSurfaceConfig,
    presetKeys: Map<String, PresetKey>,
    host: ImeSurfaceHost,
    appearance: KeyAppearance,
) {
    ChromeSlot(
        board = board,
        token = "Keyboard_basic_symbol",
        weight = CHROME_EDGE_WEIGHT,
        state = state,
        config = config,
        presetKeys = presetKeys,
        host = host,
        appearance = appearance,
    )
    ChromeSlot(
        board = board,
        token = ",",
        weight = CHROME_EDGE_WEIGHT,
        state = state,
        config = config,
        presetKeys = presetKeys,
        host = host,
        appearance = appearance,
        isFunctionOverride = true,
    )
    ChromeSlot(
        board = board,
        token = "space",
        weight = CHROME_SPACE_WEIGHT,
        state = state,
        config = config,
        presetKeys = presetKeys,
        host = host,
        appearance = appearance,
        isFunctionOverride = false,
        dimLabel = true,
    )
    ChromeSlot(
        board = board,
        token = ".",
        weight = CHROME_EDGE_WEIGHT,
        state = state,
        config = config,
        presetKeys = presetKeys,
        host = host,
        appearance = appearance,
        isFunctionOverride = true,
    )
    ChromeSlot(
        board = board,
        token = "Return",
        weight = CHROME_EDGE_WEIGHT,
        state = state,
        config = config,
        presetKeys = presetKeys,
        host = host,
        appearance = appearance,
    )
}

/**
 * One function-row slot: resolves the board's own data key for [token]
 * (keeping its gesture slots) and renders it with the hardcoded face class;
 * falls back to a synthetic chrome key when the board lacks the data.
 */
@Suppress("FunctionName")
@Composable
private fun RowScope.ChromeSlot(
    board: TextKeyboard,
    token: String,
    weight: Float,
    state: ImeSessionState,
    config: ImeSurfaceConfig,
    presetKeys: Map<String, PresetKey>,
    host: ImeSurfaceHost,
    appearance: KeyAppearance,
    isFunctionOverride: Boolean? = null,
    isEnterOverride: Boolean? = null,
    dimLabel: Boolean = false,
) {
    val dataKey = remember(board, token) { board.keyForToken(token) }
    val fallbackKey = remember(token) { TextKeyboard.TextKey(click = LegacyKeyToken.Plain(token)) }
    KeySurface(
        key = dataKey ?: fallbackKey,
        presetKeys = presetKeys,
        appearance = appearance,
        config = config,
        schemaLabel = state.schemaLabel,
        host = host,
        modifier = Modifier.weight(weight),
        isFunctionOverride = isFunctionOverride,
        isEnterOverride = isEnterOverride,
        dimLabel = dimLabel,
    )
}

/** Finds the board's own data key for a plain preset token. */
private fun TextKeyboard.keyForToken(token: String): TextKeyboard.TextKey? = keys.firstOrNull { it.click == LegacyKeyToken.Plain(token) }
