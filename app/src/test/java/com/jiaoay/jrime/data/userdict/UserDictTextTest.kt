/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.data.userdict

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class UserDictTextTest :
    StringSpec({
        val exported =
            "ni\t你\tc=3 d=0.5 t=1700000000\n" +
                "hao\t好\tc=-1 d=0 t=0\n" +
                "# comment line\n" +
                "bad-line-without-tab\n"

        "parses code phrase and value" {
            val entries = UserDictText.parse(exported)
            entries.size shouldBe 2
            entries[0].code shouldBe "ni"
            entries[0].phrase shouldBe "你"
            entries[0].commits shouldBe 3
            entries[0].deleted shouldBe false
            entries[1].phrase shouldBe "好"
            entries[1].commits shouldBe -1
            entries[1].deleted shouldBe true
        }

        "render round-trips entries" {
            val entries = UserDictText.parse(exported)
            UserDictText.parse(UserDictText.render(entries)) shouldBe entries
        }

        "bump increases commits and restores positive" {
            val entry = UserDictEntry("ni", "你", "c=3 d=0.5 t=1")
            UserDictText.bump(entry).commits shouldBe 4
            UserDictText.bump(entry.copy(value = "c=-2 d=0 t=0")).commits shouldBe 1
        }

        "markDeleted uses negative commits" {
            val entry = UserDictEntry("ni", "你", "c=3 d=0.5 t=1")
            UserDictText.markDeleted(entry).commits shouldBe -3
            UserDictText.markDeleted(entry.copy(value = "c=0 d=0 t=0")).commits shouldBe -1
        }

        "add appends a new entry" {
            val entries = UserDictText.parse(exported)
            val added = UserDictText.add(entries, "zhong", "中")
            added.size shouldBe 3
            added.last().commits shouldBe 1
            added.last().code shouldBe "zhong"
        }

        "add bumps an existing identical entry" {
            val entries = UserDictText.parse(exported)
            val bumped = UserDictText.add(entries, "ni", "你")
            bumped.size shouldBe 2
            bumped.first().commits shouldBe 4
        }
    })
