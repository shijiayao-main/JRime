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
import io.kotest.matchers.shouldBe

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

        "adapts typed keyboard slots to the renderer model and resolver" {
            val keyboardExtension =
                object : JrimeExtension {
                    override val id = "keyboard-ext"

                    override fun keyboards() = listOf(
                        KeyboardExtension(
                            KeyboardId("custom"),
                            keys =
                            listOf(
                                KeySlot(label = "★", click = ImeAction.Commit("★")),
                                KeySlot(label = "语音", longPress = ImeAction.VoiceInput),
                            ),
                        ),
                    )

                    override fun toolbarActions() = listOf(ExtensionToolbarAction(id = "t", label = "Toolbar", action = ImeAction.OpenSettings))
                }
            val registry = ExtensionRegistry(listOf(keyboardExtension))
            val board = registry.keyboardBoards.getValue(KeyboardId("custom"))
            board.keys.size shouldBe 2
            board.keys.first().label shouldBe "★"
            val resolver = com.jiaoay.jrime.action.ActionResolver(emptyMap(), registry.resolvedActions)
            resolver.resolve(board.keys.first().click) shouldBe ImeAction.Commit("★")
            resolver.resolve(board.keys.last().longClick) shouldBe ImeAction.VoiceInput
            registry.toolbarActions.single().id shouldBe "t"
        }

        "rejects duplicate toolbar entry ids" {
            val duplicate =
                object : JrimeExtension {
                    override val id = "toolbar-dup"

                    override fun toolbarActions() = listOf(
                        ExtensionToolbarAction(id = "same", label = "A", action = ImeAction.OpenSettings),
                        ExtensionToolbarAction(id = "same", label = "B", action = ImeAction.OpenSettings),
                    )
                }
            shouldThrow<IllegalArgumentException> { ExtensionRegistry(listOf(duplicate)) }
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
