/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * The clipboard chip's pure rules: the one-line preview truncation and the
 * visibility gate (the clipboard_suggestion/timeout preference pair, the
 * clip's existence and sensitivity, the expiry window).
 */
class ClipboardChipTest :
    StringSpec({

        "a preview past the character limit truncates with an ellipsis" {
            clipboardChipPreview("abcdefghijklmnopqrstuvwxyz") shouldBe "abcdefghijklmnopqrstuvwx…"
        }

        "a preview within the limit stays verbatim" {
            val exact = "abcdefghijklmnopqrstuvwx" // the limit itself, 24 characters
            clipboardChipPreview(exact) shouldBe exact
            clipboardChipPreview("hello") shouldBe "hello"
        }

        "the suggestion preference off means no chip" {
            clipboardChipVisible(
                suggestionEnabled = false,
                hasEntity = true,
                sensitive = false,
                timeoutSeconds = 0,
                elapsedMillis = 0L,
            ) shouldBe false
        }

        "no clipboard entity means no chip" {
            clipboardChipVisible(
                suggestionEnabled = true,
                hasEntity = false,
                sensitive = false,
                timeoutSeconds = 0,
                elapsedMillis = 0L,
            ) shouldBe false
        }

        "a sensitive clip never shows the chip" {
            clipboardChipVisible(
                suggestionEnabled = true,
                hasEntity = true,
                sensitive = true,
                timeoutSeconds = 0,
                elapsedMillis = 0L,
            ) shouldBe false
        }

        "a zero timeout never expires the chip" {
            clipboardChipVisible(
                suggestionEnabled = true,
                hasEntity = true,
                sensitive = false,
                timeoutSeconds = 0,
                elapsedMillis = Long.MAX_VALUE / 2,
            ) shouldBe true
        }

        "a clip past the timeout window hides the chip" {
            clipboardChipVisible(
                suggestionEnabled = true,
                hasEntity = true,
                sensitive = false,
                timeoutSeconds = 20,
                elapsedMillis = 20_001L,
            ) shouldBe false
        }

        "a clip within the timeout window shows the chip" {
            clipboardChipVisible(
                suggestionEnabled = true,
                hasEntity = true,
                sensitive = false,
                timeoutSeconds = 20,
                elapsedMillis = 20_000L,
            ) shouldBe true
        }
    })
