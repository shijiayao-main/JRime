/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.theme

import com.jiaoay.jrime.data.theme.model.KeyActionToken
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class NativeThemeTest :
    StringSpec({
        "the theme builds with both Gboard color schemes" {
            val theme = NativeTheme.theme
            theme.name shouldBe "native"
            theme.presetColorSchemes.keys shouldBe setOf(NativeTheme.DEFAULT_SCHEME, NativeTheme.DARK_SCHEME)
        }

        "the light scheme chains to the dark scheme for day/night following" {
            val light = NativeTheme.theme.presetColorSchemes.getValue(NativeTheme.DEFAULT_SCHEME)
            light.getValue("dark_scheme") shouldBe NativeTheme.DARK_SCHEME
        }

        "the Gboard style sets rounded, borderless, gap-even keys" {
            val style = NativeTheme.theme.style
            style.roundCorner shouldBe 8f
            style.keyBorder shouldBe 0
            style.horizontalGap shouldBe 4
            style.verticalGap shouldBe 4
            style.shadowRadius shouldBe 0f
        }

        "the keyboard DSL produces model keys with behavior slots" {
            val board = keyboard("probe") {
                key("q", longClick = "`", hint = "~")
                spacer(2f)
            }.build(com.jiaoay.jrime.data.theme.model.TextKeyboard.DEFAULTS)

            board.name shouldBe "probe"
            board.keys.size shouldBe 2
            val key = board.keys.first()
            key.label shouldBe ""
            key.click shouldBe KeyActionToken.Plain("q")
            key.longClick shouldBe KeyActionToken.Plain("`")
            key.hint shouldBe "~"
            board.keys[1].width shouldBe 2f
            board.keys[1].click shouldBe null
        }

        "a key with only gesture slots is a key, not a spacer" {
            val board = keyboard("probe") {
                key(swipeDown = "BackToPreviousSyllable")
            }.build(com.jiaoay.jrime.data.theme.model.TextKeyboard.DEFAULTS)

            board.keys.single().swipeDown shouldBe KeyActionToken.Plain("BackToPreviousSyllable")
        }
    })
