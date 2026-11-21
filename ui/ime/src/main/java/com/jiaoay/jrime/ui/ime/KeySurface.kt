/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.ime

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.jiaoay.jrime.data.theme.model.PresetKey
import com.jiaoay.jrime.data.theme.model.TextKeyboard
import com.jiaoay.jrime.engine.GestureConfig
import com.jiaoay.jrime.engine.GestureSample
import com.jiaoay.jrime.engine.ImeCommand
import com.jiaoay.jrime.engine.KeyActionToken
import com.jiaoay.jrime.engine.KeyAppearance
import com.jiaoay.jrime.engine.KeyBinding
import com.jiaoay.jrime.engine.KeyGesture
import com.jiaoay.jrime.engine.KeyGestureResolver
import com.jiaoay.jrime.engine.KeySpec
import com.jiaoay.jrime.engine.SwipeDir
import com.jiaoay.jrime.ui.design.ImeDimens
import com.jiaoay.jrime.ui.design.ImeMotion
import com.jiaoay.jrime.ui.design.ImeShapes
import com.jiaoay.jrime.ui.design.ImeTypography
import com.jiaoay.jrime.ui.design.LocalImePalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.jiaoay.jrime.data.theme.model.KeyActionToken as LegacyKeyToken

/*
 * The key face: one keyboard key's presentation + its gesture handler. The
 * surface owns the rows and the hardcoded chrome geometry; this file owns the
 * data key's behaviour inventory (state-aware click, long press, repeat,
 * swipes, slide-cursor) resolved through KeyBinding/KeyGestureResolver.
 *
 * Everything here is platform-neutral: Compose + the engine's pure gesture
 * model, no Android API and no platform context.
 */

/** The preset placeholder the data uses for "the app decides the enter label". */
private const val ENTER_LABEL_PLACEHOLDER = "enter_labels"

/** The fallback enter glyph when the app provides no label. */
private const val ENTER_LABEL_FALLBACK = "↵"

/** The fallback space label when the schema carries no label. */
private const val SPACE_LABEL_FALLBACK = "空格"

/**
 * One key's resolved behaviour slots. Resolved once per state and shared by the
 * face and the gesture handler so the two can never disagree.
 */
private class KeyBehaviors(
    val click: KeyBinding?,
    val longPress: KeyBinding?,
    val swipes: Map<SwipeDir, KeyBinding>,
    val spec: KeySpec,
)

/**
 * One key: face (surface/label/hint/bubble) and the single pointer handler of
 * the surface. The host receives every action as an [ImeCommand]; feedback is
 * fired once at finger-down.
 */
@Suppress("FunctionName")
@Composable
internal fun KeySurface(
    key: TextKeyboard.TextKey,
    presetKeys: Map<String, PresetKey>,
    appearance: KeyAppearance,
    config: ImeSurfaceConfig,
    schemaLabel: String,
    host: ImeSurfaceHost,
    modifier: Modifier = Modifier,
    labelOverride: String? = null,
    isFunctionOverride: Boolean? = null,
    isEnterOverride: Boolean? = null,
    highlight: Boolean = false,
    dimLabel: Boolean = false,
) {
    // A key with no behaviour slot at all is a layout spacer: it takes its
    // weighted cell but draws no face (the legacy spacer contract).
    if (key.hasNoBehavior()) {
        Box(modifier = modifier)
        return
    }

    val palette = LocalImePalette.current
    val density = LocalDensity.current
    // The face class/label come from the idle click slot (the data face); the
    // dispatched action comes from the state-aware click slot below.
    val faceBinding = remember(key, presetKeys) { KeyBinding.resolve(key.click, presetKeys) }
    val behaviors =
        remember(key, presetKeys, appearance) {
            resolveBehaviors(key, appearance, presetKeys)
        }
    val faceLabel = labelOverride ?: deriveLabel(key, faceBinding, schemaLabel, config.enterLabel)
    val faceHint = remember(key) { key.labelSymbol.ifEmpty { key.hint }.ifEmpty { null } }
    val isEnter = isEnterOverride ?: (faceBinding?.isEnter == true)
    val isFunction = isFunctionOverride ?: (faceBinding?.functional == true)

    var pressed by remember(key) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val gestureConfig = remember(density) { gestureConfigFor(density) }
    val currentHost by rememberUpdatedState(host)
    val currentBehaviors by rememberUpdatedState(behaviors)

    val background =
        when {
            pressed -> palette.keyPressed
            highlight -> palette.accent
            isEnter -> palette.enter
            isFunction -> palette.functionKey
            else -> palette.key
        }
    val foreground =
        when {
            highlight -> palette.accentText
            isEnter -> palette.enterText
            dimLabel -> palette.keySymbol
            else -> palette.keyText
        }
    val singleCharLabel = faceLabel.length == 1 && faceLabel[0].isLetterOrDigit()
    val popupLabel = faceLabel.takeIf { config.popupOnKeyPress && pressed && singleCharLabel }

    Box(
        modifier =
        modifier
            .pointerInput(behaviors.click, gestureConfig) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    val downAtMs = down.uptimeMillis
                    val latest = currentBehaviors
                    // Feedback fires at finger-down (the Gboard feel), once.
                    latest.click?.let { currentHost.onKeyFeedback(it, KeyGesture.Tap) }
                    var repeatCount = 0
                    val repeatJob =
                        // The legacy contract: an explicit long-press slot wins
                        // over hold-repeat, and slide-cursor keys trade the
                        // repeat for the cursor drag.
                        if (latest.spec.repeatable && !latest.spec.slideCursor && latest.spec.longPress == null) {
                            scope.launch {
                                while (isActive) {
                                    val waitMs =
                                        if (repeatCount == 0) {
                                            gestureConfig.longPressMs
                                        } else {
                                            gestureConfig.repeatIntervalMs
                                        }
                                    delay(waitMs)
                                    val heldMs =
                                        gestureConfig.longPressMs +
                                            repeatCount * gestureConfig.repeatIntervalMs
                                    repeatCount++
                                    val sample =
                                        GestureSample(
                                            downAtMs = downAtMs,
                                            upAtMs = downAtMs + heldMs,
                                            repeatCount = repeatCount,
                                        )
                                    if (KeyGestureResolver.resolve(latest.spec, sample, gestureConfig) is KeyGesture.Repeat) {
                                        currentHost.dispatchBinding(latest.click)
                                    }
                                }
                            }
                        } else {
                            null
                        }
                    try {
                        var dxPx = 0f
                        var dyPx = 0f
                        var upAtMs = downAtMs
                        var completed = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            dxPx = change.position.x - down.position.x
                            dyPx = change.position.y - down.position.y
                            if (change.isConsumed) break
                            if (!change.pressed) {
                                upAtMs = change.uptimeMillis
                                completed = true
                                break
                            }
                        }
                        // The hold-repeat owns the gesture once it fired;
                        // otherwise one resolver pass maps the completed
                        // press to its action slot.
                        if (completed && repeatCount == 0) {
                            val sample =
                                GestureSample(
                                    downAtMs = downAtMs,
                                    upAtMs = upAtMs,
                                    dxPx = dxPx,
                                    dyPx = dyPx,
                                )
                            val gesture = KeyGestureResolver.resolve(latest.spec, sample, gestureConfig)
                            when (gesture) {
                                KeyGesture.Tap -> currentHost.dispatchBinding(latest.click)

                                KeyGesture.LongPress ->
                                    currentHost.dispatchBinding(latest.longPress ?: latest.click)

                                is KeyGesture.Swipe -> {
                                    val slot = latest.swipes[gesture.dir]
                                    if (slot != null) {
                                        currentHost.dispatchBinding(slot)
                                    } else if (!latest.spec.slideCursor) {
                                        currentHost.dispatchBinding(latest.click)
                                    }
                                }

                                is KeyGesture.SlideCursor ->
                                    currentHost.dispatch(ImeCommand.MoveCursor(gesture.chars, gesture.lines))

                                is KeyGesture.Repeat -> Unit

                                KeyGesture.None ->
                                    if (!latest.spec.slideCursor) {
                                        currentHost.dispatchBinding(latest.click)
                                    }
                            }
                        }
                    } finally {
                        // A teardown mid-gesture (pointerInput restart, board
                        // switch) must still unpress the face and stop the
                        // repeat job -- never leave a stuck key.
                        repeatJob?.cancel()
                        pressed = false
                    }
                }
            }
            .padding(ImeDimens.KeyGap / 2f)
            .fillMaxSize()
            .background(background, ImeShapes.Key),
    ) {
        BasicText(
            text = faceLabel,
            style =
            if (singleCharLabel) {
                ImeTypography.keyLetterStyle(foreground).copy(textAlign = TextAlign.Center)
            } else {
                ImeTypography.keySymbolStyle(foreground).copy(textAlign = TextAlign.Center)
            },
            modifier = Modifier.align(Alignment.Center),
        )
        if (faceHint != null) {
            BasicText(
                text = faceHint,
                style = ImeTypography.keyHintStyle(palette.hintText).copy(textAlign = TextAlign.Center),
                modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = ImeDimens.KeyGap / 2f, end = ImeDimens.KeyGap / 2f),
            )
        }
        if (popupLabel != null) {
            // A Popup floats above the face (an in-face overlay would clip at
            // the row bound); focusable=false keeps it from stealing touches.
            val popupOffset =
                with(density) {
                    val popupHeightPx = (ImeDimens.MinTouchTarget + ImeDimens.KeyGap).toPx()
                    IntOffset(x = 0, y = -popupHeightPx.toInt())
                }
            Popup(
                alignment = Alignment.TopCenter,
                offset = popupOffset,
                properties = PopupProperties(focusable = false),
            ) {
                Box(
                    modifier =
                    Modifier
                        .widthIn(min = ImeDimens.MinTouchTarget)
                        .height(ImeDimens.MinTouchTarget)
                        .background(palette.popupBackground, ImeShapes.Popup),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = popupLabel,
                        style = ImeTypography.keyLetterStyle(palette.popupText).copy(textAlign = TextAlign.Center),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** The gesture thresholds, derived from the design tokens once per density. */
private fun gestureConfigFor(density: Density): GestureConfig = GestureConfig(
    longPressMs = ImeMotion.LONG_PRESS_TIMEOUT.toLong(),
    repeatIntervalMs = ImeMotion.REPEAT_INTERVAL.toLong(),
    swipeThresholdPx = with(density) { ImeDimens.MinTouchTarget.toPx() },
    slideCursorEnterMs = ImeMotion.LONG_PRESS_ENTER.toLong(),
    // The legacy cursor step was 22dp; half the minimum touch target keeps the
    // slide pace in dp rather than device pixels (no dp literal allowed).
    slideCursorStepPx = with(density) { (ImeDimens.MinTouchTarget / 2).toPx() },
)

/**
 * Resolves the key's behaviour slots against the preset table:
 * step 1 the state-aware click slot, step 2 the long-press slot, step 3 the
 * four swipe slots. The [KeySpec] handed to the resolver carries only the
 * gesture-relevant facts (which slots exist, repeatability, slide-cursor).
 */
private fun resolveBehaviors(
    key: TextKeyboard.TextKey,
    appearance: KeyAppearance,
    presetKeys: Map<String, PresetKey>,
): KeyBehaviors {
    val clickToken = key.resolveClickToken(appearance)
    val click = KeyBinding.resolve(clickToken, presetKeys)
    val longPress = KeyBinding.resolve(key.longClick, presetKeys)
    val swipes =
        buildMap<SwipeDir, KeyBinding> {
            KeyBinding.resolve(key.swipeUp, presetKeys)?.let { put(SwipeDir.Up, it) }
            KeyBinding.resolve(key.swipeDown, presetKeys)?.let { put(SwipeDir.Down, it) }
            KeyBinding.resolve(key.swipeLeft, presetKeys)?.let { put(SwipeDir.Left, it) }
            KeyBinding.resolve(key.swipeRight, presetKeys)?.let { put(SwipeDir.Right, it) }
        }
    val spec =
        KeySpec(
            action = clickToken?.toEngineToken() ?: KeyActionToken.Text(""),
            longPress = longPress?.token?.toEngineToken(),
            swipes = swipes.mapValues { (_, binding) -> binding.token.toEngineToken() },
            repeatable = click?.repeatable == true,
            slideCursor = click?.slideCursor == true,
        )
    return KeyBehaviors(click = click, longPress = longPress, swipes = swipes, spec = spec)
}

/**
 * The state-aware click slot, mirroring the legacy [KeyAppearance] order:
 * aux (while the aux code is active) -> composing -> ascii -> idle click.
 * The has-menu/paging slots are intentionally not wired in M1.
 */
private fun TextKeyboard.TextKey.resolveClickToken(appearance: KeyAppearance): LegacyKeyToken? {
    val ordered =
        listOfNotNull(
            if (appearance.auxActive) aux else null,
            if (appearance.composing) composing else null,
            if (appearance.isAsciiMode) ascii else null,
            click,
        )
    return ordered.firstOrNull { it.isPresent() }
}

/** A blank plain token means "no action" (the librime-blank contract). */
private fun LegacyKeyToken.isPresent(): Boolean = !(this is LegacyKeyToken.Plain && token.isEmpty())

/** Maps a layout token to the engine's gesture-only action carrier. */
private fun LegacyKeyToken.toEngineToken(): KeyActionToken = when (this) {
    is LegacyKeyToken.Plain -> KeyActionToken.Text(token)
    is LegacyKeyToken.Inline -> KeyActionToken.Text(commit ?: text ?: label.orEmpty())
}

/** A key with no behaviour slot at all is a spacer (draws nothing). */
private fun TextKeyboard.TextKey.hasNoBehavior(): Boolean = click == null &&
    doubleClick == null &&
    lazyDoubleClick == null &&
    longClick == null &&
    swipeUp == null &&
    swipeDown == null &&
    swipeLeft == null &&
    swipeRight == null &&
    composing == null &&
    hasMenu == null &&
    paging == null &&
    combo == null &&
    ascii == null &&
    aux == null

/**
 * The label the face shows: the row builder's override, the space schema
 * label, the preset label (with the enter placeholder resolved), the upper-
 * cased single letter/digit, or the plain token as the last resort.
 */
private fun deriveLabel(
    key: TextKeyboard.TextKey,
    binding: KeyBinding?,
    schemaLabel: String,
    enterLabel: String,
): String {
    if (binding?.isSpace == true) {
        return schemaLabel.ifEmpty { SPACE_LABEL_FALLBACK }
    }
    val label = binding?.label.orEmpty()
    if (label == ENTER_LABEL_PLACEHOLDER) {
        return enterLabel.ifEmpty { ENTER_LABEL_FALLBACK }
    }
    if (label.isNotEmpty()) {
        return label
    }
    val plainToken =
        binding?.plainToken.orEmpty().ifEmpty {
            (key.click as? LegacyKeyToken.Plain)?.token.orEmpty()
        }
    return if (plainToken.length == 1 && plainToken[0].isLetterOrDigit()) {
        plainToken.uppercase()
    } else {
        plainToken
    }
}

/** Dispatches a resolved behaviour slot through the host (no-op when absent). */
private fun ImeSurfaceHost.dispatchBinding(binding: KeyBinding?) {
    binding?.let { dispatch(ImeCommand.Key(it.token)) }
}
