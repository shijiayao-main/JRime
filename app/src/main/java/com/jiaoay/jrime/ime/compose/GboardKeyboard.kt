/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jiaoay.jrime.data.theme.model.KeyActionToken
import com.jiaoay.jrime.data.theme.model.PresetKey
import com.jiaoay.jrime.data.theme.model.TextKeyboard
import com.jiaoay.jrime.ime.keyboard.KeyAction

/**
 * The Gboard-structured keyboard (see ui-design-spec.md). Geometry follows
 * FlorisBoard's unit grid (TouchModel.kt): a full row spans 10 units, a
 * letter is 1 unit, a shorter letter row is inset by its missing half-widths
 * (the home-row stagger), shift and backspace take 1.5 units each. The letter
 * rows come from the board data; the function chrome (shift, backspace,
 * symbol, aux, comma, space, period, enter) is app-owned geometry -- but each
 * chrome slot still resolves its data key first, so long-press and swipe
 * behaviors survive the redesign (the old YAML data stays a behavior
 * inventory, never a face).
 */
object GboardKeyboard {
    const val ROW_HEIGHT = 58

    /** A full row spans this many units (the FlorisBoard weight grid). */
    internal const val ROW_UNITS = 10f

    /** The shift key's width in units. */
    internal const val SHIFT_UNITS = 1.5f

    /** The backspace key never gets narrower than this. */
    internal const val BACKSPACE_MIN_UNITS = 1.5f
}

/** Centering inset for a letter row shorter than the unit grid (units). */
internal fun rowInsetUnits(keyCount: Int): Float = ((GboardKeyboard.ROW_UNITS - keyCount) / 2f).coerceAtLeast(0f)

/** The backspace width that closes a shift row to the full unit grid. */
internal fun backspaceUnits(letterCount: Int): Float = (GboardKeyboard.ROW_UNITS - GboardKeyboard.SHIFT_UNITS - letterCount)
    .coerceAtLeast(GboardKeyboard.BACKSPACE_MIN_UNITS)

/**
 * Renders the Gboard rows: the data letter rows (a shorter one centered like
 * the classic home-row stagger), the shift-flanked bottom letter row, and the
 * hardcoded function row. Boards that don't fit the letter structure (symbol,
 * number, editor pages) fall back to [KeyboardPanel] at the pinned row height.
 */
@Suppress("FunctionName")
@Composable
fun GboardKeyboard(
    board: TextKeyboard,
    keyAppearance: KeyAppearance,
    presetKeys: Map<String, PresetKey>,
    onDispatch: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
    isAsciiMode: Boolean = false,
) {
    val qwerty = remember(board, presetKeys) { isQwertyBoard(board, presetKeys) }
    if (!qwerty) {
        val rowCount = remember(board) {
            foldRows(board, board.width.takeIf { it > 0f } ?: 10f).size
        }
        KeyboardPanel(
            board = board,
            keyAppearance = keyAppearance,
            presetKeys = presetKeys,
            onDispatch = onDispatch,
            modifier = modifier.height((rowCount * GboardKeyboard.ROW_HEIGHT).dp),
        )
        return
    }
    val rows = remember(board, presetKeys) { gboardLetterRows(board, presetKeys) }
    Column(modifier = modifier.fillMaxWidth()) {
        for (row in rows.dropLast(1)) {
            LetterRow(
                keys = row,
                keyAppearance = keyAppearance,
                presetKeys = presetKeys,
                onDispatch = onDispatch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GboardKeyboard.ROW_HEIGHT.dp),
            )
        }
        rows.lastOrNull()?.let { letters ->
            ShiftRow(
                board = board,
                letters = letters,
                keyAppearance = keyAppearance,
                presetKeys = presetKeys,
                onDispatch = onDispatch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GboardKeyboard.ROW_HEIGHT.dp),
                isAsciiMode = isAsciiMode,
            )
        }
        ChromeRow(
            board = board,
            keyAppearance = keyAppearance,
            presetKeys = presetKeys,
            onDispatch = onDispatch,
            modifier = Modifier
                .fillMaxWidth()
                .height(GboardKeyboard.ROW_HEIGHT.dp),
        )
    }
}

/**
 * The letter-class rows of the data board: single letters and digits only,
 * with the old theme's function clutter (spacers, shift, backspace) left to
 * the chrome rows. The legacy staggered widths drop out here too -- every
 * letter renders at 1 unit.
 */
internal fun gboardLetterRows(
    board: TextKeyboard,
    presetKeys: Map<String, PresetKey>,
): List<List<TextKeyboard.TextKey>> = foldRows(board, board.width.takeIf { it > 0f } ?: 10f)
    .map { row -> row.map { it.key }.filter { isLetterClassKey(it, presetKeys) } }
    .filter { it.isNotEmpty() }

private fun isLetterClassKey(
    key: TextKeyboard.TextKey,
    presetKeys: Map<String, PresetKey>,
): Boolean {
    val token = (key.click as? KeyActionToken.Plain)?.token ?: return false
    if (token.length != 1) return false
    if (presetKeys[token] != null) return false
    return token[0].isLetterOrDigit()
}

/**
 * Whether a board fits the Gboard structure: at least three letter-class rows
 * and a dominant share of single-character keys -- the qwerty-like boards
 * (default, flyhe36). Symbol, number, and editor pages fail this and keep
 * their own full-data layout.
 */
internal fun isQwertyBoard(
    board: TextKeyboard,
    presetKeys: Map<String, PresetKey>,
): Boolean {
    if (gboardLetterRows(board, presetKeys).size < 3) return false
    val clickable = board.keys.count { it.click != null }
    if (clickable == 0) return false
    val letterClass = board.keys.count { isLetterClassKey(it, presetKeys) }
    return letterClass.toFloat() / clickable >= 0.7f
}

/** A pure letter row, centered when it doesn't span the full unit grid. */
@Suppress("FunctionName")
@Composable
private fun LetterRow(
    keys: List<TextKeyboard.TextKey>,
    keyAppearance: KeyAppearance,
    presetKeys: Map<String, PresetKey>,
    onDispatch: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        val inset = rowInsetUnits(keys.size)
        if (inset > 0f) {
            Spacer(modifier = Modifier.weight(inset))
        }
        keys.forEach { key ->
            GboardDataKey(
                key = key,
                keyAppearance = keyAppearance,
                presetKeys = presetKeys,
                onDispatch = onDispatch,
                modifier = Modifier.weight(1f),
            )
        }
        if (inset > 0f) {
            Spacer(modifier = Modifier.weight(inset))
        }
    }
}

/** The last letter row with its chrome flanks: [⇧] letters... [⌫]. */
@Suppress("FunctionName")
@Composable
private fun ShiftRow(
    board: TextKeyboard,
    letters: List<TextKeyboard.TextKey>,
    keyAppearance: KeyAppearance,
    presetKeys: Map<String, PresetKey>,
    onDispatch: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
    isAsciiMode: Boolean = false,
) {
    Row(modifier = modifier) {
        ChromeSlot(
            board = board,
            token = "Shift_L",
            label = "⇧",
            labelSize = 20f,
            keyAppearance = keyAppearance,
            presetKeys = presetKeys,
            onDispatch = onDispatch,
            modifier = Modifier.weight(GboardKeyboard.SHIFT_UNITS),
            // The shift key doubles as the CJK/EN toggle's on-key feedback:
            // lit while ascii_mode is on, matching the action-bar chip.
            highlight = isAsciiMode,
        )
        letters.forEach { key ->
            GboardDataKey(
                key = key,
                keyAppearance = keyAppearance,
                presetKeys = presetKeys,
                onDispatch = onDispatch,
                modifier = Modifier.weight(1f),
            )
        }
        ChromeSlot(
            board = board,
            token = "BackSpace",
            label = "⌫",
            labelSize = 20f,
            keyAppearance = keyAppearance,
            presetKeys = presetKeys,
            onDispatch = onDispatch,
            modifier = Modifier.weight(backspaceUnits(letters.size)),
        )
    }
}

/**
 * The hardcoded function row: [符号] [，·辅助] [空格·中文] [。] [↵].
 * The comma doubles as the aux-code separator: while composing, its click
 * slot resolves to "/" (the data's composing slot), idle it stays "，"
 * (the master's call -- one key, both jobs, a freed slot for wider keys).
 */
@Suppress("FunctionName")
@Composable
private fun ChromeRow(
    board: TextKeyboard,
    keyAppearance: KeyAppearance,
    presetKeys: Map<String, PresetKey>,
    onDispatch: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        ChromeSlot(
            board = board,
            token = "Keyboard_basic_symbol",
            label = "符号",
            labelSize = 14f,
            keyAppearance = keyAppearance,
            presetKeys = presetKeys,
            onDispatch = onDispatch,
            modifier = Modifier.weight(15f),
        )
        ChromeSlot(
            board = board,
            token = ",",
            label = "，",
            labelSize = 20f,
            keyAppearance = keyAppearance,
            presetKeys = presetKeys,
            onDispatch = onDispatch,
            modifier = Modifier.weight(15f),
            isFunction = true,
        )
        ChromeSlot(
            board = board,
            token = "space",
            label = "中文",
            labelSize = 12f,
            keyAppearance = keyAppearance,
            presetKeys = presetKeys,
            onDispatch = onDispatch,
            modifier = Modifier.weight(40f),
            isFunction = false,
            dimLabel = true,
        )
        ChromeSlot(
            board = board,
            token = ".",
            // While the aux code is active the key's face follows its job:
            // the tap picks the second candidate, so the label says so.
            label = if (keyAppearance.auxActive) "次选" else "。",
            labelSize = 20f,
            keyAppearance = keyAppearance,
            presetKeys = presetKeys,
            onDispatch = onDispatch,
            modifier = Modifier.weight(15f),
            isFunction = true,
        )
        ChromeSlot(
            board = board,
            token = "Return",
            label = "↵",
            labelSize = 22f,
            keyAppearance = keyAppearance,
            presetKeys = presetKeys,
            onDispatch = onDispatch,
            modifier = Modifier.weight(15f),
        )
    }
}

private fun TextKeyboard.dataKeyForToken(token: String): TextKeyboard.TextKey? = keys.firstOrNull { it.click == KeyActionToken.Plain(token) }

/**
 * One chrome slot: resolves the board's own data key for [token] (keeping
 * its long-press and swipe behaviors) and renders it with the hardcoded
 * face; falls back to a plain chrome key when the board lacks the data.
 */
@Suppress("FunctionName")
@Composable
private fun ChromeSlot(
    board: TextKeyboard,
    token: String,
    label: String,
    labelSize: Float,
    keyAppearance: KeyAppearance,
    presetKeys: Map<String, PresetKey>,
    onDispatch: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    isFunction: Boolean? = null,
    isEnter: Boolean? = null,
    dimLabel: Boolean = false,
    highlight: Boolean = false,
) {
    val key = remember(board, token) { board.dataKeyForToken(token) }
    if (key != null) {
        GboardDataKey(
            key = key,
            keyAppearance = keyAppearance,
            presetKeys = presetKeys,
            onDispatch = onDispatch,
            modifier = modifier,
            labelOverride = label,
            labelSize = labelSize,
            hintOverride = hint,
            isFunction = isFunction,
            isEnter = isEnter,
            highlight = highlight,
            dimLabel = dimLabel,
        )
    } else {
        GboardChromeKey(
            label = label,
            action = token,
            presetKeys = presetKeys,
            onDispatch = onDispatch,
            modifier = modifier,
            hint = hint,
            labelSize = labelSize,
            isFunction = isFunction ?: true,
            isEnter = isEnter ?: false,
        )
    }
}
