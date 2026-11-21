/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.engine

import kotlin.math.abs

/** Thresholds for interpreting a key press. All durations in ms, distances in px. */
data class GestureConfig(
    val longPressMs: Long = 500,
    val repeatIntervalMs: Long = 80,
    val swipeThresholdPx: Float = 32f,
    /** Space must be held this long before a drag turns into cursor sliding. */
    val slideCursorEnterMs: Long = 350,
    /** Cursor slide step, in chars per step. */
    val slideCursorStepPx: Float = 22f,
) {
    init {
        require(longPressMs > 0) { "longPressMs must be positive" }
        require(repeatIntervalMs > 0) { "repeatIntervalMs must be positive" }
        require(swipeThresholdPx > 0) { "swipeThresholdPx must be positive" }
    }

    companion object {
        val Default = GestureConfig()
    }
}

/** A completed key press, as seen by the surface. */
data class GestureSample(
    val downAtMs: Long,
    val upAtMs: Long,
    val dxPx: Float = 0f,
    val dyPx: Float = 0f,
    val repeatCount: Int = 0,
) {
    val durationMs: Long get() = upAtMs - downAtMs
    val maxTravelPx: Float get() = maxOf(abs(dxPx), abs(dyPx))
}

/** The interpreted gesture of a key press. */
sealed interface KeyGesture {
    data object Tap : KeyGesture

    data object LongPress : KeyGesture

    /** Long-press auto-repeat fired [count] times. */
    data class Repeat(val count: Int) : KeyGesture

    data class Swipe(val dir: SwipeDir) : KeyGesture

    /** Space held and dragged: move the field cursor. */
    data class SlideCursor(val chars: Int, val lines: Int) : KeyGesture

    /** No action (the key has no mapping for the interpreted gesture). */
    data object None : KeyGesture
}

/**
 * The single gesture interpreter of the surface. Pure and stateless: the
 * platform feeds it the pointer summary, and the layout says which slots exist.
 *
 * Resolution order:
 * 1. held past [GestureConfig.slideCursorEnterMs] on a slide-cursor key and
 *    moved -> [KeyGesture.SlideCursor];
 * 2. travelled past [GestureConfig.swipeThresholdPx] -> [KeyGesture.Swipe]
 *    when the key declares that direction, otherwise a slide-cursor key still
 *    slides (the space key's old behaviour), else [KeyGesture.None];
 * 3. repeats -> [KeyGesture.Repeat];
 * 4. held past [GestureConfig.longPressMs] -> [KeyGesture.LongPress] (only when
 *    the key declares a long-press action);
 * 5. otherwise [KeyGesture.Tap].
 */
object KeyGestureResolver {
    fun resolve(
        key: KeySpec,
        sample: GestureSample,
        config: GestureConfig = GestureConfig.Default,
    ): KeyGesture {
        if (key.slideCursor && sample.durationMs >= config.slideCursorEnterMs) {
            if (sample.maxTravelPx >= config.swipeThresholdPx) {
                val chars = (sample.dxPx / config.slideCursorStepPx).toInt()
                val lines = if (abs(sample.dyPx) >= config.swipeThresholdPx) {
                    if (sample.dyPx < 0) -1 else 1
                } else {
                    0
                }
                if (chars != 0 || lines != 0) return KeyGesture.SlideCursor(chars, lines)
            }
        }

        if (sample.maxTravelPx >= config.swipeThresholdPx) {
            val dir = dominantDirection(sample.dxPx, sample.dyPx)
            if (key.swipes.containsKey(dir)) return KeyGesture.Swipe(dir)
            if (key.slideCursor) return KeyGesture.SlideCursor(0, 0)
            return KeyGesture.None
        }

        if (sample.repeatCount > 0 && key.repeatable) {
            return KeyGesture.Repeat(sample.repeatCount)
        }

        if (sample.durationMs >= config.longPressMs && key.longPress != null) {
            return KeyGesture.LongPress
        }

        return KeyGesture.Tap
    }

    fun dominantDirection(dx: Float, dy: Float): SwipeDir = if (abs(dx) >= abs(dy)) {
        if (dx < 0) SwipeDir.Left else SwipeDir.Right
    } else {
        if (dy < 0) SwipeDir.Up else SwipeDir.Down
    }
}
