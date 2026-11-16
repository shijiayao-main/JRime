/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import com.jiaoay.jrime.data.theme.model.KeyActionToken
import com.jiaoay.jrime.data.theme.model.TextKeyboard
import com.jiaoay.jrime.theme.NativeTheme
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * The top-row contract: the Gboard boards' leading digit row is the slot
 * composing hands to the candidate strip, so it must split off cleanly (and
 * only when it really is digits), stay a pure data row (no Compose-ignored
 * legacy face fields), and the composing echo must strip the soft caret and
 * map the aux separator for display.
 */
class KeyboardTopRowTest :
    StringSpec({

        fun foldedKeyRows(name: String): List<List<TextKeyboard.TextKey>> {
            val board = NativeTheme.theme.presetKeyboards.getValue(name)
            val defaultWidth = board.width.takeIf { it > 0f } ?: 10f
            return foldRows(board, defaultWidth).map { row -> row.map { it.key } }
        }

        fun tokens(row: List<TextKeyboard.TextKey>): List<String?> = row.map { (it.click as? KeyActionToken.Plain)?.token }

        "splitDigitRow lifts the digit row off the Gboard letter rows" {
            val presetKeys = NativeTheme.theme.presetKeys
            for (name in listOf("default", "flyhe36")) {
                val (digitRow, letterRows) =
                    splitDigitRow(gboardLetterRows(NativeTheme.theme.presetKeyboards.getValue(name), presetKeys))
                // the split must lift the row, not drop it (orEmpty still
                // fails the exact-token check below on a null)
                tokens(digitRow.orEmpty()) shouldContainExactly
                    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
                // the letter rows survive in order: qwerty / home / bottom
                letterRows shouldHaveSize 3
                tokens(letterRows[0]) shouldContainExactly
                    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
                tokens(letterRows[1]) shouldContainExactly
                    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
                tokens(letterRows[2]) shouldContainExactly
                    listOf("z", "x", "c", "v", "b", "n", "m")
            }
        }

        "splitDigitRow keeps a board whose first row is not all digits" {
            // the full-data pages open with glyphs ("~", "!", "^"...): a
            // leading glyph row must never be mistaken for the digit slot
            // (t9_symbol opens with digits, but it never takes the qwerty
            // path, so the split is simply never asked about it)
            for (name in listOf("number", "basic_symbol", "math_symbol")) {
                val rows = foldedKeyRows(name)
                val (digitRow, rest) = splitDigitRow(rows)
                digitRow shouldBe null
                rest shouldBe rows
            }
        }

        "splitDigitRow on empty rows yields no digit row" {
            val (digitRow, rest) = splitDigitRow(emptyList())
            digitRow shouldBe null
            rest shouldBe emptyList()
        }

        "the default digit row mirrors the flyhe36 behavior slots" {
            val keys = NativeTheme.theme.presetKeyboards.getValue("default").keys
            val flyhe36 = NativeTheme.theme.presetKeyboards.getValue("flyhe36").keys
            val defaultDigits = keys.take(10)
            val flyheDigits = flyhe36.take(10)
            // the slots were copied verbatim off the flyhe36 template
            defaultDigits.map { it.click } shouldBe flyheDigits.map { it.click }
            defaultDigits.map { it.longClick } shouldBe flyheDigits.map { it.longClick }
            defaultDigits.map { it.swipeUp } shouldBe flyheDigits.map { it.swipeUp }
            defaultDigits.map { it.swipeDown } shouldBe flyheDigits.map { it.swipeDown }
        }

        "the default digit row is a pure data row" {
            val digits = foldedKeyRows("default").first()
            digits shouldHaveSize 10
            tokens(digits) shouldContainExactly
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            // every key long-presses a symbol (the flyhe36 template)
            digits.forEach { key -> key.longClick.shouldNotBeNull() }
            // no Compose-ignored legacy face fields ride along
            digits.forEach { key ->
                key.height shouldBe 0f
                key.keyTextSize shouldBe 0f
                key.keySymbolOffsetX shouldBe 0f
            }
        }

        "compositionEchoText strips the soft caret and maps the aux separator" {
            compositionEchoText("efgh/x") shouldBe "efgh·x"
            compositionEchoText("nihao") shouldBe "nihao"
            // the DISABLE inline mode's soft caret never reaches the strip
            compositionEchoText("abc\u2038") shouldBe "abc"
            compositionEchoText("\u2038") shouldBe ""
            compositionEchoText("") shouldBe ""
            compositionEchoText(null) shouldBe ""
        }
    })
