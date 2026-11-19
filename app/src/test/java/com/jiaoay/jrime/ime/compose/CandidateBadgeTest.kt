/*
 * SPDX-FileCopyrightText: 2026 JRime
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * The candidate badge mirrors the engine's select keys: 1-9 badge the first
 * nine candidates (a long-press on q-p sends that digit, selecting the same
 * candidate); candidates past nine have no select key, so no badge.
 */
class CandidateBadgeTest :
    StringSpec({

        "the first nine candidates badge 1..9" {
            candidateBadgeNumber(0) shouldBe "1"
            candidateBadgeNumber(4) shouldBe "5"
            candidateBadgeNumber(8) shouldBe "9"
        }

        "candidates past nine have no select key" {
            candidateBadgeNumber(9) shouldBe null
            candidateBadgeNumber(20) shouldBe null
        }

        "out-of-range indices never badge" {
            candidateBadgeNumber(-1) shouldBe null
        }
    })
