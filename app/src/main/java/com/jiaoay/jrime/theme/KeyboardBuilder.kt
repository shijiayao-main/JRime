/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.theme

import com.jiaoay.jrime.data.theme.model.KeyActionToken
import com.jiaoay.jrime.data.theme.model.TextKeyboard
import com.jiaoay.jrime.data.theme.model.TextKeyboard.TextKey

/**
 * Type-safe builder producing the runtime [TextKeyboard] model. The DSL is
 * the single source of every keyboard layout; nothing parses anything at
 * runtime, so a mistyped slot is a compile error instead of a runtime
 * diagnostic.
 *
 * Like the model, the key list is FLAT: the renderer folds it into rows by
 * accumulating key widths in column units ([TextKeyboard.columns] slivers
 * per row), which is exactly how the old YAML expressed layouts. Use
 * [spacer] for width-only fillers.
 */
class KeyboardBuilder internal constructor(private val name: String) {
    private val keys = mutableListOf<TextKey>()

    /** Keyboard-level overrides; unset values fall back to [TextKeyboard.DEFAULTS]. */
    var author: String = ""
    var width: Float = 0f
    var height: Float = 0f
    var keyboardHeight: Int = 0
    var keyboardHeightLand: Int = 0
    var autoHeightIndex: Int = -1
    var horizontalGap: Int = 0
    var verticalGap: Int = 0
    var roundCorner: Float = -1f
    var keyBorder: Int = -1
    var columns: Int = 30
    var asciiMode: Int = 1
    var resetAsciiMode: Boolean = false
    var labelTransform: TextKeyboard.LabelTransform = TextKeyboard.LabelTransform.NONE
    var lock: Boolean = false
    var asciiKeyboard: String = ""
    var landscapeKeyboard: String = ""
    var landscapeSplitPercent: Int = 0
    var keyTextOffsetX: Float = 0f
    var keyTextOffsetY: Float = 0f
    var keySymbolOffsetX: Float = 0f
    var keySymbolOffsetY: Float = 0f
    var keyHintOffsetX: Float = 0f
    var keyHintOffsetY: Float = 0f
    var keyPressOffsetX: Float = 0f
    var keyPressOffsetY: Float = 0f

    /**
     * One key: the 13 behavior slots plus the visual fields, all by name.
     * A key whose every behavior slot is empty is a [spacer].
     */
    fun key(
        click: String? = null,
        label: String = "",
        labelSymbol: String = "",
        hint: String = "",
        width: Float = 0f,
        height: Float = 0f,
        keyTextSize: Float = 0f,
        symbolTextSize: Float = 0f,
        popup: List<String> = emptyList(),
        longClick: String? = null,
        doubleClick: String? = null,
        lazyDoubleClick: String? = null,
        swipeUp: String? = null,
        swipeDown: String? = null,
        swipeLeft: String? = null,
        swipeRight: String? = null,
        composing: String? = null,
        hasMenu: String? = null,
        paging: String? = null,
        combo: String? = null,
        ascii: String? = null,
        keyTextOffsetY: Float = 0f,
        keySymbolOffsetX: Float = 0f,
        keyBackColor: String = "",
        keyTextColor: String = "",
        keySymbolColor: String = "",
        hilitedKeyBackColor: String = "",
        hilitedKeyTextColor: String = "",
    ) {
        val slots = listOf(click, doubleClick, lazyDoubleClick, longClick, swipeUp, swipeDown, swipeLeft, swipeRight, composing, hasMenu, paging, combo, ascii)
        if (slots.all { it.isNullOrEmpty() }) {
            spacer(width)
            return
        }
        keys += TextKey.DEFAULTS.copy(
            // the renderer derives display labels from the click token; the
            // model keeps label exactly what the layout states
            label = label,
            labelSymbol = labelSymbol,
            hint = hint,
            width = width,
            height = height,
            keyTextSize = keyTextSize,
            symbolTextSize = symbolTextSize,
            popup = popup,
            keyTextOffsetY = keyTextOffsetY,
            keySymbolOffsetX = keySymbolOffsetX,
            keyBackColor = keyBackColor,
            keyTextColor = keyTextColor,
            keySymbolColor = keySymbolColor,
            hilitedKeyBackColor = hilitedKeyBackColor,
            hilitedKeyTextColor = hilitedKeyTextColor,
            click = token(click),
            doubleClick = token(doubleClick),
            lazyDoubleClick = token(lazyDoubleClick),
            longClick = token(longClick),
            swipeUp = token(swipeUp),
            swipeDown = token(swipeDown),
            swipeLeft = token(swipeLeft),
            swipeRight = token(swipeRight),
            composing = token(composing),
            hasMenu = token(hasMenu),
            paging = token(paging),
            combo = token(combo),
            ascii = token(ascii),
        )
    }

    /** A width-only filler, the YAML `keys: [{width: 2}]` equivalent. */
    fun spacer(width: Float) {
        keys += TextKey.DEFAULTS.copy(width = width)
    }

    internal fun build(defaults: TextKeyboard): TextKeyboard = defaults.copy(
        name = name,
        author = author,
        width = width,
        height = height,
        keyboardHeight = keyboardHeight,
        keyboardHeightLand = keyboardHeightLand,
        autoHeightIndex = autoHeightIndex,
        horizontalGap = horizontalGap,
        verticalGap = verticalGap,
        roundCorner = roundCorner,
        keyBorder = keyBorder,
        columns = columns,
        asciiMode = asciiMode,
        resetAsciiMode = resetAsciiMode,
        labelTransform = labelTransform,
        lock = lock,
        asciiKeyboard = asciiKeyboard,
        landscapeKeyboard = landscapeKeyboard,
        landscapeSplitPercent = landscapeSplitPercent,
        keyTextOffsetX = keyTextOffsetX,
        keyTextOffsetY = keyTextOffsetY,
        keySymbolOffsetX = keySymbolOffsetX,
        keySymbolOffsetY = keySymbolOffsetY,
        keyHintOffsetX = keyHintOffsetX,
        keyHintOffsetY = keyHintOffsetY,
        keyPressOffsetX = keyPressOffsetX,
        keyPressOffsetY = keyPressOffsetY,
        keys = keys.toList(),
    )

    private fun token(action: String?): KeyActionToken? = action?.takeIf { it.isNotEmpty() }?.let(KeyActionToken::Plain)
}

/** Entry point of the layout DSL: `keyboard("default") { key("q"); spacer(2f) }`. */
fun keyboard(name: String, block: KeyboardBuilder.() -> Unit): KeyboardBuilder = KeyboardBuilder(name).apply(block)
