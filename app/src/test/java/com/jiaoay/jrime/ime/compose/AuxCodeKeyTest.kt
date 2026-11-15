/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import android.view.KeyEvent
import com.jiaoay.jrime.core.CompositionProto
import com.jiaoay.jrime.data.theme.model.KeyActionToken
import com.jiaoay.jrime.ime.keyboard.KeyAction
import com.jiaoay.jrime.ime.keyboard.KeyBehavior
import com.jiaoay.jrime.ime.keyboard.KeyCode
import com.jiaoay.jrime.theme.NativeTheme
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * The aux-code mode contract: the composition-tail detector that lights the
 * AUX state, and the period key's AUX slot -- while the active segment takes
 * form codes after the separator, tapping the period selects and commits the
 * second candidate instead of typing a period.
 */
class AuxCodeKeyTest :
    StringSpec({
        // Same platform-call stub as KeyActionTest: the Android key-name
        // lookup throws when not mocked.
        beforeTest { KeyCode.androidKeyNameToCode = { 0 } }
        afterSpec { KeyCode.androidKeyNameToCode = KeyCode.defaultAndroidKeyNameToCode }

        val presetKeys = NativeTheme.theme.presetKeys

        fun periodKey(boardName: String) = NativeTheme.theme.presetKeyboards.getValue(boardName)
            .keys.first { it.click == KeyActionToken.Plain(".") }

        "the period key selects the second candidate while the aux code is active" {
            for (name in listOf("default", "flyhe36")) {
                val period = periodKey(name)
                period.resolveToken(KeyBehavior.CLICK, KeyAppearance(auxActive = true)) shouldBe
                    KeyActionToken.Plain("Control+2")
                // the aux state outranks composing in the click chain
                period.resolveToken(KeyBehavior.CLICK, KeyAppearance(composing = true, auxActive = true)) shouldBe
                    KeyActionToken.Plain("Control+2")
                // idle and plain composing keep the period
                period.resolveToken(KeyBehavior.CLICK, KeyAppearance()) shouldBe
                    KeyActionToken.Plain(".")
                period.resolveToken(KeyBehavior.CLICK, KeyAppearance(composing = true)) shouldBe
                    KeyActionToken.Plain(".")
            }
        }

        "the aux token parses as the number-2 key with Control held" {
            val action = KeyAction(KeyActionToken.Plain("Control+2"), presetKeys)
            action.code shouldBe KeyEvent.KEYCODE_2
            action.modifier shouldBe KeyEvent.META_CTRL_ON
        }

        "the aux-code detector mirrors the cand_selector aux gates" {
            // 2-4 input letters + "/" + 0-2 form letters (or a tone mark);
            // a confirmed segment ends in CJK text, which is the left boundary
            for (text in listOf("efgh/", "efgh/x", "ab/x", "ab/I", "你好efgh/x", "efgh/\u2038")) {
                CompositionProto(text).isAuxActive() shouldBe true
            }
            // plain codes, slash-led recognizer commands, 5+ letter runs and
            // non-form tails never light the aux state
            for (text in listOf("", "efgh", "abcd", "/oh", "/lt…", "abcde/x", "abcdef/x", "ab/c`=")) {
                CompositionProto(text).isAuxActive() shouldBe false
            }
        }
    })
