/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jiaoay.jrime.data.prefs.AppPrefs
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
 *
 * The top row is data too: idle it renders the digit row (1-9, 0); while
 * composing the slot is handed to the app-provided candidate strip
 * ([composingContent]). Either way it stays one pinned row -- the band never
 * jumps when the state flips.
 */
object GboardKeyboard {
    const val ROW_HEIGHT = 46

    /** A full row spans this many units (the FlorisBoard weight grid). */
    internal const val ROW_UNITS = 10f

    /** The shift key's width in units. */
    internal const val SHIFT_UNITS = 1.5f

    /** The backspace key never gets narrower than this. */
    internal const val BACKSPACE_MIN_UNITS = 1.5f

    /**
     * The content band height in rows every keyboard-window surface renders:
     * digit row + three letter rows + the shift row + the chrome row on the
     * Gboard-structured boards. Panels and fallback boards render this same
     * content height (their rows stretch via weight), so switching surfaces
     * never jumps the band.
     */
    const val KEYBOARD_CONTENT_ROWS = 6
}

/**
 * The row height rides the keyboard_row_height preference (the Trime parity
 * knob): read once per composition, and a settings change updates the band
 * live through the preference listener. Shared by every band surface so the
 * whole keyboard moves together on one preference.
 */
@Composable
internal fun rememberKeyboardRowHeightDp(): Int {
    val keyboardPrefs = remember { AppPrefs.defaultInstance().keyboard }
    var rowHeightDp by remember { mutableIntStateOf(keyboardPrefs.rowHeight.getValue()) }
    DisposableEffect(keyboardPrefs) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == AppPrefs.Keyboard.KEYBOARD_HEIGHT) {
                    rowHeightDp = keyboardPrefs.rowHeight.getValue()
                }
            }
        keyboardPrefs.rowHeight.sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            keyboardPrefs.rowHeight.sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    return rowHeightDp
}

/** Centering inset for a letter row shorter than the unit grid (units). */
internal fun rowInsetUnits(keyCount: Int): Float = ((GboardKeyboard.ROW_UNITS - keyCount) / 2f).coerceAtLeast(0f)

/** The backspace width that closes a shift row to the full unit grid. */
internal fun backspaceUnits(letterCount: Int): Float = (GboardKeyboard.ROW_UNITS - GboardKeyboard.SHIFT_UNITS - letterCount)
    .coerceAtLeast(GboardKeyboard.BACKSPACE_MIN_UNITS)

/**
 * Renders the Gboard rows: the data digit row on top (composing hands its
 * slot to [composingContent], the candidate strip), the data letter rows (a
 * shorter one centered like the classic home-row stagger), the shift-flanked
 * bottom letter row, and the hardcoded function row. Boards that don't fit
 * the letter structure (symbol, number, editor pages) fall back to
 * [KeyboardPanel] at the pinned row height.
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
    composingContent: (@Composable () -> Unit)? = null,
) {
    // The row height rides the keyboard_row_height preference (the Trime
    // parity knob), shared with every other band surface.
    val rowHeightDp = rememberKeyboardRowHeightDp()
    // The key-press bubble rides the show_key_popup preference through the
    // same single listener (per-key listeners are the P2 debt the gesture
    // knobs flagged) and flows down through [LocalPopupOnKeyPress] -- the
    // shared face (GboardKeyFace) is the only reader.
    val keyboardPrefs = remember { AppPrefs.defaultInstance().keyboard }
    var popupOnKeyPress by remember { mutableStateOf(keyboardPrefs.popupOnKeyPress.getValue()) }
    DisposableEffect(keyboardPrefs) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == AppPrefs.Keyboard.POPUP_ON_KEY_PRESS) {
                    popupOnKeyPress = keyboardPrefs.popupOnKeyPress.getValue()
                }
            }
        keyboardPrefs.rowHeight.sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            keyboardPrefs.rowHeight.sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    CompositionLocalProvider(LocalPopupOnKeyPress provides popupOnKeyPress) {
        val qwerty = remember(board, presetKeys) { isQwertyBoard(board, presetKeys) }
        if (!qwerty) {
            KeyboardPanel(
                board = board,
                keyAppearance = keyAppearance,
                presetKeys = presetKeys,
                onDispatch = onDispatch,
                // The fallback board renders the same content band as the
                // qwerty path (rows stretch via weight) -- switching boards
                // never jumps the band height.
                modifier = modifier.height((GboardKeyboard.KEYBOARD_CONTENT_ROWS * rowHeightDp).dp),
            )
        } else {
            val (digitRow, letterRows) =
                remember(board, presetKeys) { splitDigitRow(gboardLetterRows(board, presetKeys)) }
            Column(modifier = modifier.fillMaxWidth()) {
                // The top slot: composing shows the candidate strip the app provides,
                // idle the data digit row. Both are one pinned row -- the band height
                // never moves when the state flips (the dataKeyGestures teardown
                // contract keeps a mid-gesture swap from sticking a key face). The
                // swap only happens when the board actually leads with digits
                // (hasLeadingDigitRow): otherwise both states render letter rows only.
                if (keyAppearance.composing && composingContent != null && digitRow != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeightDp.dp),
                    ) {
                        composingContent()
                    }
                } else {
                    digitRow?.let { digits ->
                        LetterRow(
                            keys = digits,
                            keyAppearance = keyAppearance,
                            presetKeys = presetKeys,
                            onDispatch = onDispatch,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(rowHeightDp.dp),
                        )
                    }
                }
                for (row in letterRows.dropLast(1)) {
                    LetterRow(
                        keys = row,
                        keyAppearance = keyAppearance,
                        presetKeys = presetKeys,
                        onDispatch = onDispatch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeightDp.dp),
                    )
                }
                letterRows.lastOrNull()?.let { letters ->
                    ShiftRow(
                        board = board,
                        letters = letters,
                        keyAppearance = keyAppearance,
                        presetKeys = presetKeys,
                        onDispatch = onDispatch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeightDp.dp),
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
                        .height(rowHeightDp.dp),
                )
            }
        }
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

/**
 * Splits the leading digit row off the letter rows: a first row whose every
 * key clicks a single digit is the digit row -- composing hands its slot to
 * the candidate strip, so it must be separable from the letter rows. Boards
 * whose first row is anything else (symbol pages) keep all rows.
 */
internal fun splitDigitRow(
    rows: List<List<TextKeyboard.TextKey>>,
): Pair<List<TextKeyboard.TextKey>?, List<List<TextKeyboard.TextKey>>> {
    val first = rows.firstOrNull() ?: return null to rows
    val allDigits = first.all { key ->
        val token = (key.click as? KeyActionToken.Plain)?.token
        token != null && token.length == 1 && token[0].isDigit()
    }
    return if (allDigits) first to rows.drop(1) else null to rows
}

/**
 * Whether the folded rows open with the digit row -- the shared invariant
 * behind the composing slot swap: the keyboard renders the composing content
 * in the digit slot and the action bar yields its candidates only when this
 * holds (always consumed together with [isQwertyBoard]), so the band height
 * never jumps between the two states on any board.
 */
internal fun hasLeadingDigitRow(
    board: TextKeyboard,
    presetKeys: Map<String, PresetKey>,
): Boolean = splitDigitRow(gboardLetterRows(board, presetKeys)).first != null

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
        // The dedicated aux-code key: composing sends the separator, ASCII
        // keeps the plain shift. The data key has no click slot, so it is
        // located by its ascii slot instead of the usual token lookup.
        val auxKey =
            remember(board) {
                board.keys.firstOrNull { it.ascii == KeyActionToken.Plain("Shift_L") }
            }
        if (auxKey != null) {
            GboardDataKey(
                key = auxKey,
                keyAppearance = keyAppearance,
                presetKeys = presetKeys,
                onDispatch = onDispatch,
                modifier = Modifier.weight(GboardKeyboard.SHIFT_UNITS),
                labelOverride = if (isAsciiMode) "⇧" else "辅助",
                labelSize = if (isAsciiMode) 20f else 18f,
                isFunction = true,
                highlight = isAsciiMode,
            )
        } else {
            ChromeSlot(
                board = board,
                token = "Shift_L",
                label = "⇧",
                labelSize = 20f,
                keyAppearance = keyAppearance,
                presetKeys = presetKeys,
                onDispatch = onDispatch,
                modifier = Modifier.weight(GboardKeyboard.SHIFT_UNITS),
                highlight = isAsciiMode,
            )
        }
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
            labelSize = 20f,
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
