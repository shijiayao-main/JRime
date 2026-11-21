/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.engine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class KeyGestureResolverTest :
    StringSpec({
        val config = GestureConfig(longPressMs = 500, swipeThresholdPx = 32f, slideCursorEnterMs = 350)

        fun letter(
            longPress: KeyActionToken? = null,
            swipes: Map<SwipeDir, KeyActionToken> = emptyMap(),
        ) = KeySpec(action = KeyActionToken.Text("a"), longPress = longPress, swipes = swipes)

        "a short press without travel is a tap" {
            KeyGestureResolver.resolve(
                letter(),
                GestureSample(downAtMs = 0, upAtMs = 100),
                config,
            ) shouldBe KeyGesture.Tap
        }

        "held past the timeout with a long-press slot is a long press" {
            KeyGestureResolver.resolve(
                letter(longPress = KeyActionToken.Text("A")),
                GestureSample(downAtMs = 0, upAtMs = 600),
                config,
            ) shouldBe KeyGesture.LongPress
        }

        "held past the timeout without a long-press slot falls back to tap" {
            KeyGestureResolver.resolve(
                letter(),
                GestureSample(downAtMs = 0, upAtMs = 600),
                config,
            ) shouldBe KeyGesture.Tap
        }

        "a repeat tick on a repeatable key reports the count" {
            val key = KeySpec(action = KeyActionToken.Backspace, repeatable = true)
            KeyGestureResolver.resolve(
                key,
                GestureSample(downAtMs = 0, upAtMs = 600, repeatCount = 3),
                config,
            ) shouldBe KeyGesture.Repeat(3)
        }

        "a repeat tick on a non-repeatable key stays a long press or tap" {
            KeyGestureResolver.resolve(
                letter(),
                GestureSample(downAtMs = 0, upAtMs = 100, repeatCount = 3),
                config,
            ) shouldBe KeyGesture.Tap
        }

        "travel past the threshold with a declared slot is a directional swipe" {
            KeyGestureResolver.resolve(
                letter(swipes = mapOf(SwipeDir.Up to KeyActionToken.Text("!"))),
                GestureSample(downAtMs = 0, upAtMs = 120, dxPx = 2f, dyPx = -60f),
                config,
            ) shouldBe KeyGesture.Swipe(SwipeDir.Up)
        }

        "travel past the threshold without a declared slot is no action" {
            KeyGestureResolver.resolve(
                letter(),
                GestureSample(downAtMs = 0, upAtMs = 120, dxPx = 0f, dyPx = -60f),
                config,
            ) shouldBe KeyGesture.None
        }

        "the dominant axis wins when both travel" {
            KeyGestureResolver.dominantDirection(10f, -40f) shouldBe SwipeDir.Up
            KeyGestureResolver.dominantDirection(-40f, 10f) shouldBe SwipeDir.Left
            KeyGestureResolver.dominantDirection(40f, 10f) shouldBe SwipeDir.Right
            KeyGestureResolver.dominantDirection(10f, 40f) shouldBe SwipeDir.Down
        }

        "holding the space key and dragging slides the cursor" {
            val space = KeySpec(action = KeyActionToken.Text(" "), slideCursor = true)
            KeyGestureResolver.resolve(
                space,
                GestureSample(downAtMs = 0, upAtMs = 500, dxPx = 66f, dyPx = 0f),
                config,
            ) shouldBe KeyGesture.SlideCursor(chars = 3, lines = 0)
        }

        "a fast horizontal drag on the space key still slides with zero movement" {
            val space = KeySpec(action = KeyActionToken.Text(" "), slideCursor = true)
            KeyGestureResolver.resolve(
                space,
                GestureSample(downAtMs = 0, upAtMs = 100, dxPx = 40f, dyPx = 0f),
                config,
            ) shouldBe KeyGesture.SlideCursor(chars = 0, lines = 0)
        }

        "a vertical drag on the space key maps to a line move" {
            val space = KeySpec(action = KeyActionToken.Text(" "), slideCursor = true)
            KeyGestureResolver.resolve(
                space,
                GestureSample(downAtMs = 0, upAtMs = 500, dxPx = 0f, dyPx = 60f),
                config,
            ) shouldBe KeyGesture.SlideCursor(chars = 0, lines = 1)
        }

        "a drag with no meaningful movement is a tap" {
            KeyGestureResolver.resolve(
                letter(swipes = mapOf(SwipeDir.Up to KeyActionToken.Text("!"))),
                GestureSample(downAtMs = 0, upAtMs = 100, dxPx = 3f, dyPx = 3f),
                config,
            ) shouldBe KeyGesture.Tap
        }
    })
