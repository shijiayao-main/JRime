/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jiaoay.jrime.data.theme.model.KeyActionToken
import com.jiaoay.jrime.data.theme.model.TextKeyboard
import com.jiaoay.jrime.data.theme.model.TextKeyboard.TextKey
import com.jiaoay.jrime.data.theme.model.orAbsent
import com.jiaoay.jrime.ime.keyboard.InputFeedbackManager
import com.jiaoay.jrime.ime.keyboard.KeyAction
import com.jiaoay.jrime.ime.keyboard.KeyBehavior
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The full-data keyboard renderer: every key of the board renders with its
 * own face and behavior. The Gboard-structured boards (letters + chrome)
 * render through [GboardKeyboard] instead; this panel takes the boards that
 * don't fit that structure (symbol, number, editor pages).
 *
 * Shared machinery lives here too: [foldRows] (the legacy percentage-weight
 * row folding), [KeyAppearance] (the state flags), [TextKey.resolveToken]
 * (behavior-slot resolution), and [dataKeyGestures] (the gesture handler
 * every data key rides on).
 */
@Suppress("FunctionName")
@Composable
fun KeyboardPanel(
    board: TextKeyboard,
    keyAppearance: KeyAppearance,
    presetKeys: Map<String, com.jiaoay.jrime.data.theme.model.PresetKey>,
    onDispatch: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
    defaultKeyWidth: Float = 10f,
) {
    val rows = remember(board, defaultKeyWidth) { foldRows(board, defaultKeyWidth) }
    Column(modifier = modifier.fillMaxWidth()) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                row.forEach { (key, weight) ->
                    KeyButton(
                        key = key,
                        keyAppearance = keyAppearance,
                        presetKeys = presetKeys,
                        onDispatch = onDispatch,
                        modifier = Modifier.weight(if (weight > 0f) weight else 1f),
                    )
                }
            }
        }
    }
}

/** Percentage weights per row, the legacy MAX_TOTAL_WEIGHT system. */
internal const val MAX_TOTAL_WEIGHT = 100f

/** One row entry: a key with its percentage weight. */
internal data class RowEntry(
    val key: TextKey,
    val weight: Float,
)

internal fun foldRows(
    board: TextKeyboard,
    defaultKeyWidth: Float,
): List<List<RowEntry>> {
    // Mirrors the legacy Keyboard row pass (Keyboard.kt:144-174): weights are
    // percentages of the allowed width (MAX_TOTAL_WEIGHT = 100), a key with
    // zero width and a click action takes the keyboard default width, and a
    // row wraps when the accumulated weight would exceed the full width or
    // the clickable-key column cap.
    val maxColumns = if (board.columns > 0) board.columns else Int.MAX_VALUE
    val rows = mutableListOf<MutableList<RowEntry>>()
    var current = mutableListOf<RowEntry>()
    var totalWeight = 0f
    var column = 0
    for (key in board.keys) {
        val weight = if (key.width == 0f && key.click != null) defaultKeyWidth else key.width
        if (column >= maxColumns || totalWeight + weight > MAX_TOTAL_WEIGHT) {
            if (current.isNotEmpty()) {
                rows += current
            }
            current = mutableListOf()
            totalWeight = 0f
            column = 0
        }
        current += RowEntry(key, weight)
        totalWeight += weight
        if (key.click != null) {
            column++
        }
    }
    if (current.isNotEmpty()) {
        rows += current
    }
    return rows
}

/** The state-aware key appearance flags driving behavior resolution. */
data class KeyAppearance(
    val composing: Boolean = false,
    val hasMenu: Boolean = false,
    val paging: Boolean = false,
)

/**
 * Resolves the token a [behavior] fires for [key]: behavior slots are walked
 * in the [KeyBehavior] order, with the composing/has-menu/paging slots
 * gated on the live input state.
 */
internal fun TextKey.resolveToken(
    behavior: KeyBehavior,
    appearance: KeyAppearance,
): KeyActionToken? {
    val ordered = when (behavior) {
        KeyBehavior.CLICK ->
            listOfNotNull(
                if (appearance.composing) KeyBehavior.COMPOSING else null,
                if (appearance.hasMenu) KeyBehavior.HAS_MENU else null,
                if (appearance.paging) KeyBehavior.PAGING else null,
                KeyBehavior.CLICK,
            )

        else -> listOf(behavior)
    }
    for (candidate in ordered) {
        val token = when (candidate) {
            KeyBehavior.COMPOSING -> composing
            KeyBehavior.HAS_MENU -> hasMenu
            KeyBehavior.PAGING -> paging
            KeyBehavior.COMBO -> combo
            KeyBehavior.ASCII -> ascii
            KeyBehavior.CLICK -> click
            KeyBehavior.DOUBLE_CLICK -> doubleClick
            KeyBehavior.LAZY_DOUBLE_CLICK -> lazyDoubleClick
            KeyBehavior.SWIPE_UP -> swipeUp
            KeyBehavior.LONG_CLICK -> longClick
            KeyBehavior.SWIPE_DOWN -> swipeDown
            KeyBehavior.SWIPE_LEFT -> swipeLeft
            KeyBehavior.SWIPE_RIGHT -> swipeRight
        }
        // the blank-plain-token contract: orAbsent normalizes "" to no-action
        if (token.orAbsent != null) {
            return token
        }
    }
    return null
}

/** The swipe displacement (dp) past which a gesture counts as a swipe. */
private val SWIPE_THRESHOLD = 40.dp

/**
 * The gesture handler every data key rides on (the behavior contract):
 * press visuals on down, tap resolves the state-aware click slot, long-press
 * fires the long-click slot, release displacement past the threshold fires
 * the swipe slots, and a repeatable click (backspace-style) repeats while
 * held. Shared by the Gboard data keys and this panel's own [KeyButton] so
 * the two renderers can never drift apart behaviorally.
 */
@Composable
internal fun Modifier.dataKeyGestures(
    key: TextKey,
    keyAppearance: KeyAppearance,
    presetKeys: Map<String, com.jiaoay.jrime.data.theme.model.PresetKey>,
    onDispatch: (KeyAction) -> Unit,
    onPressed: (Boolean) -> Unit,
): Modifier {
    val scope = rememberCoroutineScope()
    val swipeThresholdPx = with(LocalDensity.current) { SWIPE_THRESHOLD.toPx() }
    // Composing-state flips must never restart this handler: a restart
    // cancels the in-flight gesture (the pressed face sticks, the tap is
    // eaten -- the "stuck key" feel) and orphans the repeat job, which then
    // never sees `released` and repeats forever. The live values ride in
    // through updated state instead of pointerInput restart keys.
    val currentAppearance by rememberUpdatedState(keyAppearance)
    val currentPresetKeys by rememberUpdatedState(presetKeys)
    val currentDispatch by rememberUpdatedState(onDispatch)
    val currentPressed by rememberUpdatedState(onPressed)
    return this.pointerInput(key) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            currentPressed(true)
            // The press feedback fires the moment the finger lands (the
            // Gboard feel), against the click slot the tap would dispatch.
            key.resolveToken(KeyBehavior.CLICK, currentAppearance)?.let { token ->
                InputFeedbackManager.run {
                    val code = KeyAction(token, currentPresetKeys).code
                    keyPressSound(code)
                    keyPressSpeak(code)
                }
            }
            val downPos = down.position

            var released = false
            var longFired = false
            val holdJob = scope.launch {
                delay(viewConfiguration.longPressTimeoutMillis)
                val token = key.resolveToken(KeyBehavior.LONG_CLICK, currentAppearance)
                if (token != null) {
                    longFired = true
                    currentDispatch(KeyAction(token, currentPresetKeys))
                } else if (key.resolveToken(KeyBehavior.CLICK, currentAppearance)
                    ?.let { KeyAction(it, currentPresetKeys).isRepeatable } == true
                ) {
                    longFired = true
                    while (!released) {
                        val clickToken = key.resolveToken(KeyBehavior.CLICK, currentAppearance)
                        if (clickToken != null) {
                            currentDispatch(KeyAction(clickToken, currentPresetKeys))
                        }
                        delay(80)
                    }
                }
            }

            try {
                val up = waitForUpOrCancellation()
                if (up != null && !longFired) {
                    val delta = up.position - downPos
                    val threshold = swipeThresholdPx
                    val swipeBehavior = when {
                        delta.y < -threshold && key.swipeUp != null -> KeyBehavior.SWIPE_UP
                        delta.y > threshold && key.swipeDown != null -> KeyBehavior.SWIPE_DOWN
                        delta.x < -threshold && key.swipeLeft != null -> KeyBehavior.SWIPE_LEFT
                        delta.x > threshold && key.swipeRight != null -> KeyBehavior.SWIPE_RIGHT
                        else -> null
                    }
                    val token = if (swipeBehavior != null) {
                        key.resolveToken(swipeBehavior, currentAppearance)
                    } else {
                        key.resolveToken(KeyBehavior.CLICK, currentAppearance)
                    }
                    if (token != null) {
                        currentDispatch(KeyAction(token, currentPresetKeys))
                    }
                }
            } finally {
                // Even when the handler is torn down mid-gesture (a keyboard
                // switch restarts pointerInput), the face must unpress and
                // the repeat job must stop.
                holdJob.cancel()
                released = true
                currentPressed(false)
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun KeyButton(
    key: TextKey,
    keyAppearance: KeyAppearance,
    presetKeys: Map<String, com.jiaoay.jrime.data.theme.model.PresetKey>,
    onDispatch: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pressed by remember(key) { mutableStateOf(false) }

    val token = (key.click as? KeyActionToken.Plain)?.token
    val preset = token?.let(presetKeys::get)
    val isReturn = preset?.send == "Return"
    // the space bar is a content key in Gboard's language, white like letters
    val isFunctional = preset != null && token != "space" && token != "Alt_space"
    val isSpacer = key.click == null && key.longClick == null && key.swipeUp == null &&
        key.swipeDown == null && key.swipeLeft == null && key.swipeRight == null &&
        key.composing == null && key.hasMenu == null && key.paging == null &&
        key.combo == null && key.ascii == null

    if (isSpacer) {
        Box(modifier = modifier)
        return
    }

    val displayLabel = remember(key, presetKeys) { displayLabel(key, presetKeys) }
    // the corner hint shows the key's own hint text; the symbol label is
    // already the main face for these boards (labelSymbol feeds displayLabel)
    val hint = remember(key) { key.hint.ifEmpty { null } }
    val labelSize = remember(displayLabel) { labelSizeFor(displayLabel) }

    GboardKeyFace(
        label = displayLabel,
        hint = hint,
        pressed = pressed,
        isFunction = isFunctional,
        isEnter = isReturn,
        labelSize = labelSize,
        modifier = modifier.dataKeyGestures(
            key = key,
            keyAppearance = keyAppearance,
            presetKeys = presetKeys,
            onDispatch = onDispatch,
            onPressed = { pressed = it },
        ),
    )
}

/** The text a key face shows: explicit label, symbol, the preset's label, or the token. */
internal fun displayLabel(
    key: TextKey,
    presetKeys: Map<String, com.jiaoay.jrime.data.theme.model.PresetKey>,
): String {
    if (key.label.isNotEmpty()) {
        return if (key.label == "enter_labels") ENTER_LABEL else key.label
    }
    if (key.labelSymbol.isNotEmpty()) return key.labelSymbol
    val token = (key.click as? KeyActionToken.Plain)?.token.orEmpty()
    presetKeys[token]?.label?.takeIf { it.isNotEmpty() && it != "enter_labels" }?.let { return it }
    if (presetKeys[token]?.label == "enter_labels") return ENTER_LABEL
    if (token == "space") return "空格"
    return token.uppercase()
}

private const val ENTER_LABEL = "回车"

/** Letter-sized for single glyphs, smaller for words and CJK labels. */
internal fun labelSizeFor(label: String): Float = when {
    label.length == 1 && label[0].isLetterOrDigit() -> 22f
    label.any { it.code > 0x2E80 } -> 20f
    else -> 16f
}
