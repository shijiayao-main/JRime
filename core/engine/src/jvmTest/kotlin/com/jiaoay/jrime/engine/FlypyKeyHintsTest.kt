/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.engine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FlypyKeyHintsTest :
    StringSpec({
        "the rhyme keys carry the schema's final mapping" {
            FlypyKeyHints.hintFor('q') shouldBe "iu"
            FlypyKeyHints.hintFor('w') shouldBe "ei"
            FlypyKeyHints.hintFor('r') shouldBe "uan"
            FlypyKeyHints.hintFor('t') shouldBe "ue"
            FlypyKeyHints.hintFor('y') shouldBe "un"
            FlypyKeyHints.hintFor('b') shouldBe "in"
            FlypyKeyHints.hintFor('n') shouldBe "iao"
            FlypyKeyHints.hintFor('m') shouldBe "ian"
        }

        "case does not matter" {
            FlypyKeyHints.hintFor('Q') shouldBe "iu"
        }

        "pure vowel keys carry no redundant hint" {
            FlypyKeyHints.hintFor('a') shouldBe null
            FlypyKeyHints.hintFor('e') shouldBe null
            FlypyKeyHints.hintFor('i') shouldBe null
            FlypyKeyHints.hintFor('u') shouldBe null
        }
    })
