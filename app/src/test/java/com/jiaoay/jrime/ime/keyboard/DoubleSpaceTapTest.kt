/*
 * SPDX-FileCopyrightText: 2026 JRime
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.keyboard

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * The double-space rule's tap-gap contract: a deliberate second tap lands in
 * the 120-350ms window; the 80ms key-repeat cadence and slow re-taps never
 * count (the field's trailing char is what actually gates the replacement --
 * this only decides whether the tap qualifies for that check).
 */
class DoubleSpaceTapTest :
    StringSpec({

        "a repeat-cadence gap is not a double tap" {
            isDoubleSpaceTap(80) shouldBe false
            isDoubleSpaceTap(100) shouldBe false
        }

        "the window bounds are inclusive" {
            isDoubleSpaceTap(120) shouldBe true
            isDoubleSpaceTap(350) shouldBe true
            isDoubleSpaceTap(240) shouldBe true
        }

        "outside the window is not a double tap" {
            isDoubleSpaceTap(0) shouldBe false
            isDoubleSpaceTap(119) shouldBe false
            isDoubleSpaceTap(351) shouldBe false
            isDoubleSpaceTap(1_000) shouldBe false
        }
    })
