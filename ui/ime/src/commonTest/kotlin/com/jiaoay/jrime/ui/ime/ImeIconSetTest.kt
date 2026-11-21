/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.ime

import com.jiaoay.jrime.ui.design.ImeIcon
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ImeIconSetTest :
    StringSpec({
        "icon keys are unique and stable" {
            val keys = ImeIcon.entries.map { it.key }
            keys.distinct().size shouldBe keys.size
            ImeIcon.entries.all { it.key.isNotBlank() } shouldBe true
        }

        "icon names stay scheme neutral" {
            val forbidden = listOf("flypy", "flyhe", "xhfast", "trime", "tongwen", "rime")
            ImeIcon.entries.forEach { icon ->
                forbidden.forEach { word ->
                    icon.key.contains(word, ignoreCase = true) shouldBe false
                }
            }
        }

        "icons expose the core product surfaces" {
            listOf(
                ImeIcon.Settings,
                ImeIcon.InputRules,
                ImeIcon.Keyboard,
                ImeIcon.NumberPad,
                ImeIcon.Candidates,
                ImeIcon.Appearance,
                ImeIcon.Clipboard,
                ImeIcon.Emoji,
                ImeIcon.Phrase,
                ImeIcon.Hide,
                ImeIcon.Back,
                ImeIcon.ChevronLeft,
                ImeIcon.ChevronRight,
            ).forEach { icon -> icon.key.isNotBlank() shouldBe true }
        }
    })
