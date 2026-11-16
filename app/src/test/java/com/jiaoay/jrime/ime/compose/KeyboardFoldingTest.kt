/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import com.jiaoay.jrime.data.theme.model.KeyActionToken
import com.jiaoay.jrime.ime.keyboard.KeyBehavior
import com.jiaoay.jrime.theme.NativeTheme
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * The layout dump of the Compose keyboard folding (the geometry the master
 * sees): rows fold exactly like the legacy Keyboard renderer -- percentage
 * weights, the board's width as the default key width, a wrap at 100 -- then
 * the Gboard structure maps the letter-class rows onto the FlorisBoard unit
 * grid (centered short rows, shift/backspace flanks).
 */
class KeyboardFoldingTest :
    StringSpec({

        fun dump(name: String) {
            val board = NativeTheme.theme.presetKeyboards.getValue(name)
            val defaultWidth = board.width.takeIf { it > 0f } ?: 10f
            val rows = foldRows(board, defaultWidth)
            println("== $name (defaultKeyWidth=$defaultWidth, ${rows.size} rows) ==")
            for ((index, row) in rows.withIndex()) {
                val keys =
                    row.joinToString(" ") { entry ->
                        val label =
                            entry.key.label.ifEmpty {
                                (entry.key.click as? KeyActionToken.Plain)?.token.orEmpty()
                            }
                        "$label:${entry.weight}"
                    }
                val total = row.sumOf { it.weight.toDouble() }
                println("row $index (weight $total): $keys")
            }
        }

        fun fold(name: String): Pair<List<List<RowEntry>>, Float> {
            val board = NativeTheme.theme.presetKeyboards.getValue(name)
            val defaultWidth = board.width.takeIf { it > 0f } ?: 10f
            return foldRows(board, defaultWidth) to defaultWidth
        }

        "layout dump: every keyboard prints its folded geometry" {
            for (name in listOf("default", "flyhe36", "number", "editor", "basic_symbol", "math_symbol", "t9_symbol")) {
                dump(name)
            }
        }

        "the default keyboard folds into the five classic rows (the digit row on top)" {
            val (rows, _) = fold("default")
            rows shouldHaveSize 5

            val digits = rows[0]
            digits shouldHaveSize 10
            digits.sumOf { it.weight.toDouble() } shouldBe 100.0
            digits.map { (it.key.click as KeyActionToken.Plain).token } shouldBe
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            digits.forEach { it.weight shouldBe 10f }

            val first = rows[1]
            first shouldHaveSize 10
            first.sumOf { it.weight.toDouble() } shouldBe 100.0
            first.map { (it.key.click as KeyActionToken.Plain).token } shouldBe
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
            first.forEach { it.weight shouldBe 10f }

            val home = rows[2]
            // leading spacer(2) + a(12) s(11) d f g h j(10) k(11) l(12) + trailing spacer(2)
            home.sumOf { it.weight.toDouble() } shouldBe 100.0
            home.count { it.key.click != null } shouldBe 9

            val bottom = rows[3]
            // Shift(14) + z x c v b n m (7x10) + BackSpace(16) = 100
            bottom.sumOf { it.weight.toDouble() } shouldBe 100.0
            // the aux key in the shift slot has no click slot (it resolves
            // through composing/ascii), so eight click-bearing keys remain
            bottom.count { it.key.click != null } shouldBe 8

            val functionRow = rows[4]
            functionRow.sumOf { it.weight.toDouble() } shouldBe 100.0
            // the comma is a plain comma again; the dedicated aux-code key
            // (in the shift slot) carries the separator while composing
            val comma = functionRow.first { it.key.click == KeyActionToken.Plain(",") }
            comma.weight shouldBe 15f
            comma.key.composing shouldBe null
        }

        "no row exceeds the max total weight" {
            for (name in listOf("default", "flyhe36", "number", "editor", "basic_symbol", "math_symbol", "t9_symbol")) {
                val (rows, _) = fold(name)
                for (row in rows) {
                    val total = row.sumOf { it.weight.toDouble() }
                    (total <= 100.0 + 1e-6) shouldBe true
                }
            }
        }

        "preset-backed keys render their preset labels" {
            val theme = NativeTheme.theme
            val board = NativeTheme.theme.presetKeyboards.getValue("default")
            val basicSymbol = board.keys.first { (it.click as? KeyActionToken.Plain)?.token == "Keyboard_basic_symbol" }
            displayLabel(basicSymbol, theme.presetKeys) shouldBe "符号"
            val backspace = board.keys.first { (it.click as? KeyActionToken.Plain)?.token == "BackSpace" }
            displayLabel(backspace, theme.presetKeys) shouldBe "⇦"
            val ret = board.keys.first { (it.click as? KeyActionToken.Plain)?.token == "Return" }
            displayLabel(ret, theme.presetKeys) shouldBe "回车"
        }

        "the dedicated aux key resolves to the separator while composing" {
            val board = NativeTheme.theme.presetKeyboards.getValue("default")
            val aux = board.keys.first { it.ascii == KeyActionToken.Plain("Shift_L") }
            aux.resolveToken(
                KeyBehavior.CLICK,
                KeyAppearance(composing = true),
            ) shouldBe KeyActionToken.Plain("/")
            // ASCII keeps the plain shift through the ascii slot
            aux.resolveToken(
                KeyBehavior.CLICK,
                KeyAppearance(isAsciiMode = true),
            ) shouldBe KeyActionToken.Plain("Shift_L")
            // idle CJK: no click slot, so the key stays inert
            aux.resolveToken(
                KeyBehavior.CLICK,
                KeyAppearance(),
            ) shouldBe null
        }

        "the comma key carries no hidden slots (the YAML leftovers are gone)" {
            for (name in listOf("default", "flyhe36")) {
                val comma = NativeTheme.theme.presetKeyboards.getValue(name)
                    .keys.first { it.click == KeyActionToken.Plain(",") }
                // the number-board long-press/swipe-up entrances moved to the
                // action bar's "123" chip; the aux job moved to the dedicated
                // key in the shift slot, so the comma is a plain comma again
                comma.longClick shouldBe null
                comma.swipeUp shouldBe null
                comma.hint shouldBe ""
                comma.composing shouldBe null
            }
        }

        "gboardLetterRows strips every non-letter-class key from the data board" {
            val presetKeys = NativeTheme.theme.presetKeys
            val letters = gboardLetterRows(NativeTheme.theme.presetKeyboards.getValue("default"), presetKeys)
            letters shouldHaveSize 4
            // every key face is a single letter or digit, nothing else
            for (row in letters) {
                for (key in row) {
                    val token = (key.click as? KeyActionToken.Plain)?.token.orEmpty()
                    token.length shouldBe 1
                    token[0].isLetterOrDigit() shouldBe true
                }
            }
            letters[0].map { (it.click as? KeyActionToken.Plain)?.token } shouldBe
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            letters[1].map { (it.click as? KeyActionToken.Plain)?.token } shouldBe
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
            letters[2].map { (it.click as? KeyActionToken.Plain)?.token } shouldBe
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
            letters[3].map { (it.click as? KeyActionToken.Plain)?.token } shouldBe
                listOf("z", "x", "c", "v", "b", "n", "m")

            // the 36-key board keeps its digit row: letters and digits both count
            val flyhe36 = gboardLetterRows(NativeTheme.theme.presetKeyboards.getValue("flyhe36"), presetKeys)
            flyhe36 shouldHaveSize 4
            flyhe36[0].map { (it.click as? KeyActionToken.Plain)?.token } shouldBe
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        }

        "isQwertyBoard routes only the qwerty boards to the Gboard structure" {
            val presetKeys = NativeTheme.theme.presetKeys
            for (name in listOf("default", "flyhe36")) {
                isQwertyBoard(NativeTheme.theme.presetKeyboards.getValue(name), presetKeys) shouldBe true
            }
            for (name in listOf("number", "editor", "basic_symbol", "math_symbol", "t9_symbol")) {
                isQwertyBoard(NativeTheme.theme.presetKeyboards.getValue(name), presetKeys) shouldBe false
            }
        }

        "the Gboard row geometry follows the FlorisBoard unit grid" {
            // a full 10-unit row spans the width; shorter rows center by their
            // missing half-widths (the home-row stagger)
            rowInsetUnits(10) shouldBe 0f
            rowInsetUnits(9) shouldBe 0.5f
            rowInsetUnits(8) shouldBe 1f
            rowInsetUnits(7) shouldBe 1.5f
            // the shift row closes to the grid: 1.5 + letters + backspace = 10
            backspaceUnits(7) shouldBe 1.5f
            backspaceUnits(6) shouldBe 2.5f
            // but the backspace never collapses below its floor; an oversized
            // row just lets the Row weights normalize the overflow
            backspaceUnits(8) shouldBe 1.5f
            backspaceUnits(9) shouldBe 1.5f
        }

        "the editor keyboard folds 20-percent keys with a wide space" {
            val (rows, _) = fold("editor")
            rows shouldHaveSize 4
            rows.forEach { row ->
                row.filter { it.key.click != null }.forEach { entry ->
                    entry.weight shouldBe if (entry.key.click == KeyActionToken.Plain("Alt_space")) 40f else 20f
                }
                row.sumOf { it.weight.toDouble() } shouldBe 100.0
            }
        }
    })
