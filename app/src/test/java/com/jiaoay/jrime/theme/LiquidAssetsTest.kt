/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.theme

import com.jiaoay.jrime.data.theme.model.LiquidKeyboard
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * The liquid content asset (task 1.4): the standalone JSON that replaced the
 * theme YAML's liquid_keyboard section parses into the model the UI reads --
 * the closure's metrics, its fixed key bar, and the full keyboard inventory.
 */
class LiquidAssetsTest :
    FunSpec({
        val text = java.io.File("src/main/assets/shared/liquid.json").readText()
        val liquid = LiquidAssets.parse(text)

        fun keyboard(id: String): LiquidKeyboard.Keyboard = liquid.keyboards.first { it.id == id }

        test("the closure's metrics and fixed key bar port over") {
            liquid.singleWidth shouldBe 48
            liquid.keyHeight shouldBe 42
            liquid.marginX shouldBe 2f
            liquid.fixedKeyBar.position shouldBe LiquidKeyboard.KeyBar.Position.RIGHT
            liquid.fixedKeyBar.keys.first() shouldBe "Liquid_fixed_exit"
            liquid.fixedKeyBar.keys shouldHaveSize 6
        }

        test("the keyboard inventory survives the YAML engine's death") {
            liquid.keyboards shouldHaveSize 55
            val total = liquid.keyboards.sumOf { it.keys.size }
            total shouldBe 7536
            // the dynamic boards carry no inline keys; the single boards do
            keyboard("history").type shouldBe LiquidKeyboard.DataType.HISTORY
            keyboard("history").keys shouldHaveSize 0
            keyboard("emoji_face").type shouldBe LiquidKeyboard.DataType.SINGLE
            keyboard("emoji_face").name shouldBe "😀"
            keyboard("emoji_face").keys.first().text shouldBe "😀"
        }
    })
