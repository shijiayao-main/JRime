/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.jiaoay.jrime.data.theme.model.KeyActionToken
import com.jiaoay.jrime.data.theme.model.PresetKey
import com.jiaoay.jrime.data.theme.model.TextKeyboard
import com.jiaoay.jrime.ime.keyboard.InputFeedbackManager
import com.jiaoay.jrime.ime.keyboard.KeyAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/*
 * The Gboard key family (spec §2): GboardKeyFace paints one face --
 * class-colored surface, centered label, small corner hint -- while the two
 * button composables drive it. GboardDataKey keeps a data key's full
 * behavior-slot inventory (state-aware click, long-press, swipes, repeat);
 * GboardChromeKey fires one fixed action for keys the board data doesn't
 * carry. The old YAML data is a behavior inventory, never a face.
 */

/*
 * The pressed-key bubble rides the show_key_popup preference, provided once
 * at the keyboard root (GboardKeyboard) through this local -- one preference
 * listener for the whole tree, never one per key (the per-key listener
 * pattern is the P2 debt the gesture-knob wiring already flagged).
 */

/** Whether the pressed key shows its floating character bubble; provided at GboardKeyboard. */
val LocalPopupOnKeyPress = staticCompositionLocalOf { false }

/** The pressed-key bubble's fixed geometry: height, the gap above the key, min/max width. */
private val KEY_POPUP_HEIGHT = 40.dp

private val KEY_POPUP_GAP = 4.dp

private val KEY_POPUP_MIN_WIDTH = 40.dp

// The width ceiling: room for the rare multi-char label ("www", "ESC") at
// 24sp, never a strip across the keyboard band (a third of a 360dp screen;
// the ClipboardChip's 200dp ceiling is sized for a 24-char preview, too wide
// for a momentary flash above one key).
private val KEY_POPUP_MAX_WIDTH = 120.dp

/**
 * Whether the key-press bubble is visible: the preference gates it, the
 * press triggers it, and an empty label suppresses it -- nothing to preview,
 * no empty bubble.
 */
internal fun keyPopupVisible(
    popupOnKeyPress: Boolean,
    pressed: Boolean,
    label: String,
): Boolean = popupOnKeyPress && pressed && label.isNotEmpty()

/** One Gboard key face: surface by class, 8dp corners, centered label, corner hint. */
@Suppress("FunctionName")
@Composable
fun GboardKeyFace(
    label: String,
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    isFunction: Boolean = false,
    isEnter: Boolean = false,
    dimLabel: Boolean = false,
    highlight: Boolean = false,
    labelSize: Float = 19f,
    hint: String? = null,
) {
    val style = gboardStyle()
    val background =
        when {
            pressed -> style.pressed
            highlight -> style.highlight
            isEnter -> style.enter
            isFunction -> style.function
            else -> style.letter
        }
    val foreground =
        when {
            highlight -> style.highlightText
            isEnter -> style.enterText
            dimLabel -> style.textVariant
            else -> style.text
        }
    // The character bubble is a composition-layer child of the face: it
    // appears and disappears with the pressed recomposition, and never
    // touches a pointerInput restart key (the stuck-key contract stays).
    val popupShown = keyPopupVisible(LocalPopupOnKeyPress.current, pressed, label)
    Box(
        modifier = modifier
            .padding(2.dp)
            .fillMaxSize()
            .background(background, RoundedCornerShape(8.dp)),
    ) {
        BasicText(
            text = label,
            style =
            TextStyle(
                fontSize = labelSize.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = foreground,
            ),
            modifier = Modifier.align(Alignment.Center),
        )
        if (hint != null) {
            BasicText(
                text = hint,
                style = style.hintStyle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 5.dp),
            )
        }
        if (popupShown) {
            // A Popup floats above the face (the ClipboardPanel long-press
            // menu precedent): an in-face overlay would clip at the keyboard
            // band's row bound. focusable=false keeps it from stealing the
            // touch; no animation beyond the simplest appear/disappear.
            Popup(
                alignment = Alignment.TopCenter,
                offset = with(LocalDensity.current) {
                    IntOffset(0, -(KEY_POPUP_HEIGHT + KEY_POPUP_GAP).toPx().roundToInt())
                },
                properties = PopupProperties(focusable = false),
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = KEY_POPUP_MIN_WIDTH, max = KEY_POPUP_MAX_WIDTH)
                        .height(KEY_POPUP_HEIGHT)
                        .background(style.pressed, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = label,
                        style =
                        TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            color = style.text,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * A data key in the Gboard structure: the face is Gboard's, the behavior is
 * the key's own slot inventory -- long-press and swipe gestures keep working
 * exactly as the data prescribes (spec §3). The face defaults to the letter
 * look; row builders override label/size/class for the chrome-class keys
 * (shift, backspace, space, enter...) whose gestures still come from data.
 */
@Suppress("FunctionName")
@Composable
fun GboardDataKey(
    key: TextKeyboard.TextKey,
    keyAppearance: KeyAppearance,
    presetKeys: Map<String, PresetKey>,
    onDispatch: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
    labelOverride: String? = null,
    labelSize: Float? = null,
    hintOverride: String? = null,
    isFunction: Boolean? = null,
    isEnter: Boolean? = null,
    dimLabel: Boolean = false,
    highlight: Boolean = false,
) {
    var pressed by remember(key) { mutableStateOf(false) }

    val token = (key.click as? KeyActionToken.Plain)?.token.orEmpty()
    val preset = token.ifEmpty { null }?.let(presetKeys::get)
    val derivedEnter = preset?.send == "Return"
    val derivedFunction = preset != null && token != "space" && token != "Alt_space"

    val faceLabel =
        labelOverride
            ?: token.takeIf { it.length == 1 && it[0].isLetterOrDigit() }?.uppercase()
            ?: displayLabel(key, presetKeys)
    val faceHint =
        hintOverride
            ?: key.labelSymbol.ifEmpty { key.hint }.ifEmpty { null }
    val faceSize =
        labelSize
            ?: when {
                faceLabel.length == 1 && faceLabel[0].isLetterOrDigit() -> 22f
                faceLabel.any { it.code > 0x2E80 } -> 20f
                else -> 16f
            }

    GboardKeyFace(
        label = faceLabel,
        hint = faceHint,
        pressed = pressed,
        isFunction = isFunction ?: derivedFunction,
        isEnter = isEnter ?: derivedEnter,
        dimLabel = dimLabel,
        highlight = highlight,
        labelSize = faceSize,
        modifier = modifier.dataKeyGestures(
            key = key,
            keyAppearance = keyAppearance,
            presetKeys = presetKeys,
            onDispatch = onDispatch,
            onPressed = { pressed = it },
        ),
    )
}

/**
 * A chrome key the board data doesn't carry: fires [action] on tap; a
 * repeatable action (backspace-style) repeats after the long-press timeout.
 * The timing rides the same preference-backed knobs the data keys use (the
 * long-press timeout, the repeat interval) so the fallback can never drift
 * from the [dataKeyGestures] pace. Used only as the fallback when a
 * Gboard-structured row misses its data key.
 */
@Suppress("FunctionName")
@Composable
fun GboardChromeKey(
    label: String,
    action: String,
    presetKeys: Map<String, PresetKey>,
    onDispatch: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    labelSize: Float = 18f,
    isFunction: Boolean = true,
    isEnter: Boolean = false,
) {
    var pressed by remember(action) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentThresholds by rememberUpdatedState(rememberGestureThresholds())

    fun fire() {
        onDispatch(KeyAction(KeyActionToken.Plain(action), presetKeys))
    }

    GboardKeyFace(
        label = label,
        hint = hint,
        pressed = pressed,
        isFunction = !isEnter && isFunction,
        isEnter = isEnter,
        labelSize = labelSize,
        modifier = modifier
            .pointerInput(action) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    // The press feedback fires on the down (the Gboard feel),
                    // not on the tap's dispatch.
                    InputFeedbackManager.run {
                        val code = KeyAction(KeyActionToken.Plain(action), presetKeys).code
                        keyPressSound(code)
                        keyPressSpeak(code)
                    }
                    val repeatable = KeyAction(KeyActionToken.Plain(action), presetKeys).isRepeatable
                    val repeatJob =
                        if (repeatable) {
                            scope.launch {
                                // 0 means follow the system long-press timeout
                                // (the accessibility setting respect included),
                                // never a 0ms hold.
                                val longPressTimeoutMs =
                                    effectiveLongPressTimeoutMs(currentThresholds.longPressTimeoutMs)
                                        ?: viewConfiguration.longPressTimeoutMillis
                                delay(longPressTimeoutMs.toLong())
                                while (isActive) {
                                    fire()
                                    delay(effectiveRepeatIntervalMs(currentThresholds.repeatIntervalMs).toLong())
                                }
                            }
                        } else {
                            null
                        }
                    try {
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            fire()
                        }
                    } finally {
                        // A teardown mid-gesture (pointerInput restart) must
                        // still unpress the face and stop the repeat job.
                        repeatJob?.cancel()
                        pressed = false
                    }
                }
            },
    )
}
