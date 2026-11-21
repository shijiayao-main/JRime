/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.engine

import com.jiaoay.jrime.data.theme.model.KeyActionToken
import com.jiaoay.jrime.data.theme.model.PresetKey
import com.jiaoay.jrime.data.theme.model.TextKeyboard

/**
 * Pure layout maths shared by every surface renderer. These are the proven
 * folding rules of the current keyboard (legacy percentage weights), lifted
 * out of the Compose tree so they can be tested without a device and reused by
 * the desktop surface.
 */
object KeyboardLayoutOps {
    /** A full row spans this many units. */
    const val ROW_UNITS: Float = 10f

    /** The shift key's width in units. */
    const val SHIFT_UNITS: Float = 1.5f

    /** The backspace key never gets narrower than this. */
    const val BACKSPACE_MIN_UNITS: Float = 1.5f

    /** Percentage weights per row, the legacy MAX_TOTAL_WEIGHT system. */
    const val MAX_TOTAL_WEIGHT: Float = 100f

    /** Centering inset for a letter row shorter than the unit grid (units). */
    fun rowInsetUnits(keyCount: Int): Float = ((ROW_UNITS - keyCount) / 2f).coerceAtLeast(0f)

    /** The backspace width that closes a shift row to the full unit grid. */
    fun backspaceUnits(letterCount: Int): Float = (ROW_UNITS - SHIFT_UNITS - letterCount)
        .coerceAtLeast(BACKSPACE_MIN_UNITS)

    /**
     * Folds a flat key list into rows, mirroring the legacy row pass: a key
     * without a width takes the keyboard default when it has a click action, a
     * row wraps when the accumulated weight would exceed the full width or the
     * column cap.
     */
    fun foldRows(
        board: TextKeyboard,
        defaultKeyWidth: Float = 10f,
    ): List<List<RowEntry>> {
        val maxColumns = if (board.columns > 0) board.columns else Int.MAX_VALUE
        val rows = mutableListOf<MutableList<RowEntry>>()
        var current = mutableListOf<RowEntry>()
        var totalWeight = 0f
        var column = 0
        for (key in board.keys) {
            val weight = if (key.width == 0f && key.click != null) defaultKeyWidth else key.width
            if (column >= maxColumns || totalWeight + weight > MAX_TOTAL_WEIGHT) {
                if (current.isNotEmpty()) rows += current
                current = mutableListOf()
                totalWeight = 0f
                column = 0
            }
            current += RowEntry(key, weight)
            totalWeight += weight
            if (key.click != null) column++
        }
        if (current.isNotEmpty()) rows += current
        return rows
    }

    /**
     * The letter-class rows of a data board: single letters and digits only,
     * with the function clutter left to the chrome rows; every letter renders
     * at one unit.
     */
    fun gboardLetterRows(
        board: TextKeyboard,
        presetKeys: Map<String, PresetKey>,
    ): List<List<TextKeyboard.TextKey>> = foldRows(board, board.width.takeIf { it > 0f } ?: ROW_UNITS)
        .map { row -> row.map { it.key }.filter { isLetterClassKey(it, presetKeys) } }
        .filter { it.isNotEmpty() }

    /**
     * Splits the leading digit row off the letter rows: a first row whose
     * every key clicks a single digit is the digit row (the composing slot
     * swap hands it to the candidate strip).
     */
    fun splitDigitRow(
        rows: List<List<TextKeyboard.TextKey>>,
    ): Pair<List<TextKeyboard.TextKey>?, List<List<TextKeyboard.TextKey>>> {
        val first = rows.firstOrNull() ?: return null to rows
        val allDigits =
            first.all { key ->
                val token = (key.click as? KeyActionToken.Plain)?.token
                token != null && token.length == 1 && token[0].isDigit()
            }
        return if (allDigits) first to rows.drop(1) else null to rows
    }

    /** Whether the folded rows open with the digit row. */
    fun hasLeadingDigitRow(
        board: TextKeyboard,
        presetKeys: Map<String, PresetKey>,
    ): Boolean = splitDigitRow(gboardLetterRows(board, presetKeys)).first != null

    fun isLetterClassKey(
        key: TextKeyboard.TextKey,
        presetKeys: Map<String, PresetKey>,
    ): Boolean {
        val token = (key.click as? KeyActionToken.Plain)?.token ?: return false
        if (token.length != 1) return false
        if (presetKeys[token] != null) return false
        return token[0].isLetterOrDigit()
    }

    /**
     * Whether a board fits the Gboard structure: at least three letter-class
     * rows and a dominant share of single-character keys (the qwerty-like
     * boards); symbol, number and editor pages fail this and keep their own
     * full-data layout.
     */
    fun isQwertyBoard(
        board: TextKeyboard,
        presetKeys: Map<String, PresetKey>,
    ): Boolean {
        if (gboardLetterRows(board, presetKeys).size < 3) return false
        val clickable = board.keys.count { it.click != null }
        if (clickable == 0) return false
        val letterClass = board.keys.count { isLetterClassKey(it, presetKeys) }
        return letterClass.toFloat() / clickable >= 0.7f
    }

    /** Finds the board's own data key for a preset token (chrome slots keep its gestures). */
    fun TextKeyboard.dataKeyForToken(token: String): TextKeyboard.TextKey? = keys.firstOrNull { it.click == KeyActionToken.Plain(token) }
}

/** One row entry: a key with its percentage weight. */
data class RowEntry(
    val key: TextKeyboard.TextKey,
    val weight: Float,
)

/** The state-aware flags that gate a key's state-dependent behaviour slots. */
data class KeyAppearance(
    val composing: Boolean = false,
    val hasMenu: Boolean = false,
    val paging: Boolean = false,
    val auxActive: Boolean = false,
    val isAsciiMode: Boolean = false,
)
