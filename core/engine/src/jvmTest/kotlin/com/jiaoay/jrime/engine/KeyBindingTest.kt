/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.engine

import com.jiaoay.jrime.data.theme.model.KeyActionToken
import com.jiaoay.jrime.data.theme.model.PresetKey
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class KeyBindingTest :
    StringSpec({
        val presets =
            mapOf(
                "space" to PresetKey(preview = "{schema}", repeatable = true, slideCursor = true, send = "space"),
                "BackSpace" to PresetKey(label = "⇦", repeatable = true, send = "BackSpace"),
                "Commit_Caret" to PresetKey(label = "^", commit = "^"),
                "ascii_switch" to PresetKey(toggle = "ascii_mode", states = listOf("中", "EN"), send = "SWITCH_CHARSET"),
                "Keyboard_number" to PresetKey(select = "number", label = "123", send = "Eisu_toggle"),
                "clipboard_window" to PresetKey(command = "clipboard_window", label = "剪贴", send = "FUNCTION"),
                "Return" to PresetKey(label = "enter_labels", repeatable = true, send = "Return"),
            )

        "a preset token carries label, flags and the raw token" {
            val binding = KeyBinding.resolve(KeyActionToken.Plain("BackSpace"), presets)!!
            binding.label shouldBe "⇦"
            binding.repeatable shouldBe true
            binding.slideCursor shouldBe false
            binding.plainToken shouldBe "BackSpace"
            binding.isEnter shouldBe false
        }

        "the space key is flagged as space and keeps its preview" {
            val binding = KeyBinding.resolve(KeyActionToken.Plain("space"), presets)!!
            binding.isSpace shouldBe true
            binding.slideCursor shouldBe true
            binding.preview shouldBe "{schema}"
            binding.functional shouldBe false
        }

        "an enter preset is flagged as enter" {
            KeyBinding.resolve(KeyActionToken.Plain("Return"), presets)!!.isEnter shouldBe true
        }

        "a command preset keeps its states and is functional" {
            val binding = KeyBinding.resolve(KeyActionToken.Plain("ascii_switch"), presets)!!
            binding.states shouldBe listOf("中", "EN")
            binding.functional shouldBe true
        }

        "a plain token without a preset still resolves" {
            val binding = KeyBinding.resolve(KeyActionToken.Plain("q"), presets)!!
            binding.plainToken shouldBe "q"
            binding.label shouldBe ""
            binding.functional shouldBe false
        }

        "an inline token resolves to a label" {
            val binding = KeyBinding.resolve(KeyActionToken.Inline(commit = "，", label = "，"), presets)!!
            binding.label shouldBe "，"
            binding.plainToken shouldBe ""
        }

        "a blank token is absent" {
            KeyBinding.resolve(KeyActionToken.Plain(""), presets) shouldBe null
            KeyBinding.resolve(null, presets) shouldBe null
        }
    })
