/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.action

import com.jiaoay.jrime.data.theme.model.KeyActionToken
import com.jiaoay.jrime.engine.KeyboardId
import com.jiaoay.jrime.engine.PanelId
import com.jiaoay.jrime.theme.KeyLayouts
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ActionResolverTest :
    StringSpec({
        val resolver = ActionResolver(KeyLayouts.presetKeys)

        fun resolve(token: String) = resolver.resolve(KeyActionToken.Plain(token))

        "core keys resolve to typed actions" {
            resolve("BackSpace") shouldBe ImeAction.Key("BackSpace")
            resolve("space") shouldBe ImeAction.Key("space")
            resolve("a") shouldBe ImeAction.Key("a")
            resolve("Control+c") shouldBe ImeAction.Key("Control+c")
            resolve("Shift_L") shouldBe ImeAction.Key("Shift_L")
        }

        "functional presets map to typed commands" {
            resolve("ascii_switch") shouldBe ImeAction.ToggleOption("ascii_mode")
            resolve("punct_switch") shouldBe ImeAction.ToggleOption("ascii_punct")
            resolve("Keyboard_number") shouldBe ImeAction.SwitchKeyboard(KeyboardId.Number)
            resolve("clipboard_window") shouldBe ImeAction.OpenPanel(PanelId.Clipboard)
            resolve("liquid_keyboard_emoji") shouldBe ImeAction.OpenPanel(PanelId.Emoji)
            resolve("Settings") shouldBe ImeAction.OpenSettings
            resolve("VoiceInput") shouldBe ImeAction.VoiceInput
            resolve("Commit_Caret") shouldBe ImeAction.Commit("^")
            resolve("Newline") shouldBe ImeAction.Commit("\n")
        }

        "legacy edit macros become explicit typed steps" {
            resolve("copy_all") shouldBe
                ImeAction.Sequence(
                    listOf(ImeAction.TextEdit(TextEditOp.SelectAll), ImeAction.TextEdit(TextEditOp.Copy)),
                )
            resolve("cut_all") shouldBe
                ImeAction.Sequence(
                    listOf(ImeAction.TextEdit(TextEditOp.SelectAll), ImeAction.TextEdit(TextEditOp.Cut)),
                )
            resolve("Clear") shouldBe
                ImeAction.Sequence(
                    listOf(ImeAction.TextEdit(TextEditOp.SelectAll), ImeAction.TextEdit(TextEditOp.Clear)),
                )
        }

        "multi-character literals are commits" {
            resolve("……") shouldBe ImeAction.Commit("……")
            resolve("♠︎") shouldBe ImeAction.Commit("♠︎")
        }

        "every shipped layout slot resolves to a typed action" {
            val slots = mutableSetOf<String>()
            KeyLayouts.keyboards.values.forEach { board ->
                board.keys.forEach { key ->
                    listOf(
                        key.click, key.longClick, key.swipeUp, key.swipeDown,
                        key.swipeLeft, key.swipeRight, key.composing, key.ascii, key.aux,
                    ).forEach { token ->
                        (token as? KeyActionToken.Plain)?.token?.let(slots::add)
                    }
                }
            }
            val unsupported = slots.filter { resolve(it) is ImeAction.Unsupported }.toSet()
            println("UNSUPPORTED LAYOUT TOKENS: ${unsupported.sorted()}")
            unsupported shouldBe LEGACY_UNSUPPORTED
        }
    })

/** Documents the YAML-era behaviours scheduled for deletion; keep in sync. */
private val LEGACY_UNSUPPORTED = emptySet<String>()
