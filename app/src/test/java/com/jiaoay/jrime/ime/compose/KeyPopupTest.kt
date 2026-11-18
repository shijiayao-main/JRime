/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * The key-press bubble's visibility rule (the preference contract): the
 * show_key_popup preference gates it, the press triggers it, and an empty
 * label suppresses it -- nothing to preview, no empty bubble.
 */
class KeyPopupTest :
    StringSpec({

        "the preference off means no bubble, whatever the press state" {
            keyPopupVisible(popupOnKeyPress = false, pressed = true, label = "A") shouldBe false
            keyPopupVisible(popupOnKeyPress = false, pressed = false, label = "A") shouldBe false
        }

        "the preference on, a press, and a non-empty label show the bubble" {
            keyPopupVisible(popupOnKeyPress = true, pressed = true, label = "A") shouldBe true
            keyPopupVisible(popupOnKeyPress = true, pressed = true, label = "，") shouldBe true
        }

        "an empty label suppresses the bubble even under a press" {
            keyPopupVisible(popupOnKeyPress = true, pressed = true, label = "") shouldBe false
        }

        "no press means no bubble even with the preference on" {
            keyPopupVisible(popupOnKeyPress = true, pressed = false, label = "A") shouldBe false
        }
    })
