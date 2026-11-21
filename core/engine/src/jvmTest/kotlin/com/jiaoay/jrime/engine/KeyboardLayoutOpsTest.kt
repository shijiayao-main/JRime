/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.engine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class KeyboardLayoutOpsTest :
    StringSpec({
        val catalog = KeyboardCatalog.shipped()
        val default = catalog.board(KeyboardId.Qwerty26)

        "the shipped default board folds into the expected row count" {
            val rows = KeyboardLayoutOps.foldRows(default)
            rows.size shouldBe 5
            rows.first().size shouldBe 10
        }

        "the default board is qwerty-structured with a leading digit row" {
            KeyboardLayoutOps.isQwertyBoard(default, catalog.presetKeys) shouldBe true
            KeyboardLayoutOps.hasLeadingDigitRow(default, catalog.presetKeys) shouldBe true
        }

        "the default board has four letter-class rows after the digit row" {
            val letterRows = KeyboardLayoutOps.gboardLetterRows(default, catalog.presetKeys)
            letterRows.size shouldBe 4
            val (digits, letters) = KeyboardLayoutOps.splitDigitRow(letterRows)
            (digits != null) shouldBe true
            letters.size shouldBe 3
            letters[0].size shouldBe 10
            letters[1].size shouldBe 9
            letters[2].size shouldBe 7
        }

        "non-qwerty boards fall into the full-data path" {
            val number = catalog.board(KeyboardId.Number)
            KeyboardLayoutOps.isQwertyBoard(number, catalog.presetKeys) shouldBe false
        }

        "the shift row geometry closes to the full grid" {
            KeyboardLayoutOps.rowInsetUnits(7) shouldBe 1.5f
            KeyboardLayoutOps.rowInsetUnits(10) shouldBe 0f
            KeyboardLayoutOps.backspaceUnits(7) shouldBe 1.5f
        }

        "every shipped keyboard folds into non-empty rows" {
            catalog.keyboards.forEach { (id, board) ->
                val rows = KeyboardLayoutOps.foldRows(board)
                (rows.isNotEmpty()) shouldBe true
                rows.all { it.isNotEmpty() } shouldBe true
            }
        }
    })
