/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.engine

import com.jiaoay.jrime.data.theme.model.KeyActionToken
import com.jiaoay.jrime.theme.KeyLayouts
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** The chrome-row long-press behaviours introduced for JRime. */
class KeyLayoutsPresetTest :
    StringSpec({
        val presets = KeyLayouts.presetKeys

        "voice input preset switches to the system voice IME" {
            presets.getValue("VoiceInput").send shouldBe "VOICE_ASSIST"
        }

        "newline preset commits a literal newline" {
            presets.getValue("Newline").commit shouldBe "\n"
        }

        "enumeration comma preset commits the Chinese comma" {
            presets.getValue("EnumerationComma").commit shouldBe "、"
        }

        "the new presets resolve to bindings the surface can dispatch" {
            KeyBinding.resolve(KeyActionToken.Plain("VoiceInput"), presets) shouldNotBe null
            KeyBinding.resolve(KeyActionToken.Plain("Newline"), presets) shouldNotBe null
            KeyBinding.resolve(KeyActionToken.Plain("EnumerationComma"), presets) shouldNotBe null
        }

        "the default board leaves the chrome long-presses to the surface" {
            val board = KeyLayouts.keyboards.getValue("default")
            board.keys.first { it.click == KeyActionToken.Plain("space") }.longClick shouldBe null
            board.keys.first { it.click == KeyActionToken.Plain("Return") }.longClick shouldBe null
        }
    })
