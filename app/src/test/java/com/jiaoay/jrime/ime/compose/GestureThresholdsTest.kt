/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * The effective-value mapping the gesture handlers ride (the preference
 * contract): the special 0 semantics of the two knobs -- swipe travel 0 is
 * swipe detection off (never a 0dp threshold), long-press timeout 0 follows
 * the system setting (never a 0ms hold) -- and the clamping of out-of-range
 * values back into each slider's reachable range.
 */
class GestureThresholdsTest :
    StringSpec({

        "swipe travel 0 means swipe detection off, never a 0dp threshold" {
            effectiveSwipeTravelDp(0) shouldBe null
            effectiveSwipeTravelDp(-10) shouldBe null
        }

        "swipe travel clamps into the slider's reachable positive range" {
            effectiveSwipeTravelDp(40) shouldBe 40
            effectiveSwipeTravelDp(10) shouldBe 10
            effectiveSwipeTravelDp(400) shouldBe 400
            effectiveSwipeTravelDp(500) shouldBe 400
            effectiveSwipeTravelDp(5) shouldBe 10
            effectiveSwipeTravelDp(1) shouldBe 10
        }

        "long-press timeout 0 means follow the system setting, never a 0ms hold" {
            effectiveLongPressTimeoutMs(0) shouldBe null
            effectiveLongPressTimeoutMs(-50) shouldBe null
        }

        "long-press timeout clamps explicit values into the configured range" {
            effectiveLongPressTimeoutMs(300) shouldBe 300
            effectiveLongPressTimeoutMs(100) shouldBe 100
            effectiveLongPressTimeoutMs(1000) shouldBe 1000
            effectiveLongPressTimeoutMs(50) shouldBe 100
            effectiveLongPressTimeoutMs(2000) shouldBe 1000
        }

        "repeat interval clamps into the configured range" {
            effectiveRepeatIntervalMs(80) shouldBe 80
            effectiveRepeatIntervalMs(10) shouldBe 10
            effectiveRepeatIntervalMs(100) shouldBe 100
            effectiveRepeatIntervalMs(5) shouldBe 10
            effectiveRepeatIntervalMs(0) shouldBe 10
            effectiveRepeatIntervalMs(150) shouldBe 100
        }
    })
