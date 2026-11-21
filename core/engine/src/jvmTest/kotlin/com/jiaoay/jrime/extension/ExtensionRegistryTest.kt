/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.extension

import com.jiaoay.jrime.action.ImeAction
import com.jiaoay.jrime.engine.KeyboardId
import com.jiaoay.jrime.engine.PanelId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.maps.shouldContainKey

class ExtensionRegistryTest :
    StringSpec({
        val base =
            object : JrimeExtension {
                override val id = "core"

                override fun actions() = mapOf("core_action" to ImeAction.Commit("x"))

                override fun panels() = listOf(PanelDescriptor(PanelId.Clipboard, "Clipboard", "📋"))
            }
        val extra =
            object : JrimeExtension {
                override val id = "extra"

                override fun actions() = mapOf("extra_action" to ImeAction.VoiceInput)

                override fun keyboards() = listOf(KeyboardExtension(KeyboardId("custom"), keys = emptyList()))

                override fun inputRules() = listOf(InputRuleDescriptor(option = "my_switch", title = "My switch"))
            }

        "merges actions, keyboards, panels and rules by model" {
            val registry = ExtensionRegistry(listOf(base, extra))
            registry.actions shouldContainKey "core_action"
            registry.actions shouldContainKey "extra_action"
            registry.keyboards shouldContainKey KeyboardId("custom")
            registry.panels shouldContainKey PanelId.Clipboard
            registry.inputRules.map { it.option } shouldContain "my_switch"
        }

        "rejects duplicate extension ids" {
            shouldThrow<IllegalArgumentException> { ExtensionRegistry(listOf(base, base)) }
        }

        "rejects duplicate action tokens across extensions" {
            val clash =
                object : JrimeExtension {
                    override val id = "clash"

                    override fun actions() = mapOf("core_action" to ImeAction.Commit("y"))
                }
            shouldThrow<IllegalArgumentException> { ExtensionRegistry(listOf(base, clash)) }
        }

        "rejects panel id collisions" {
            val clash =
                object : JrimeExtension {
                    override val id = "clash-panel"

                    override fun panels() = listOf(PanelDescriptor(PanelId.Clipboard, "Other", "X"))
                }
            shouldThrow<IllegalArgumentException> { ExtensionRegistry(listOf(base, clash)) }
        }
    })
