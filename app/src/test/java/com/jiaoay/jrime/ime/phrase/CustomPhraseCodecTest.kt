/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.phrase

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CustomPhraseCodecTest :
    StringSpec({
        val sample =
            """
            # Rime table
            # coding: utf-8
            ## 置顶字词
            啊	a	1
            不	b	2
            好	h	2
            """.trimIndent() + "\n"

        "the header comments are preserved and entries parsed" {
            val file = CustomPhraseCodec.parse(sample)
            file.header shouldBe
                listOf(
                    "# Rime table",
                    "# coding: utf-8",
                    "## 置顶字词",
                )
            file.phrases shouldBe
                listOf(
                    CustomPhrase("啊", "a", 1),
                    CustomPhrase("不", "b", 2),
                    CustomPhrase("好", "h", 2),
                )
        }

        "a missing weight defaults to 1 and malformed lines are dropped" {
            val file =
                CustomPhraseCodec.parse(
                    """
                    # header
                    词	ci
                    broken-line
                    	empty-text
                    """.trimIndent() + "\n",
                )
            file.phrases shouldBe listOf(CustomPhrase("词", "ci", 1))
        }

        "render round-trips through parse" {
            val file = CustomPhraseCodec.parse(sample)
            CustomPhraseCodec.parse(CustomPhraseCodec.render(file)) shouldBe file
        }
    })
