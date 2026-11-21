/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.extension

import com.jiaoay.jrime.action.ActionResolver
import com.jiaoay.jrime.action.ImeAction
import com.jiaoay.jrime.engine.KeyboardCatalog
import com.jiaoay.jrime.engine.KeyboardId
import com.jiaoay.jrime.extensions.sample.SampleExtension
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/** Pins the model-first path end to end: extension -> catalog -> resolver. */
class SampleExtensionWiringTest :
    StringSpec({
        val registry = ExtensionRegistry(listOf(SampleExtension))

        "the sample keyboard is merged into the shipped catalog" {
            val catalog = KeyboardCatalog.shipped(registry)
            catalog.find(KeyboardId(SampleExtension.SAMPLE_KEYBOARD)).shouldNotBeNull()
            catalog.find(KeyboardId.Qwerty26).shouldNotBeNull()
        }

        "typed extension slots resolve without any legacy token" {
            val catalog = KeyboardCatalog.shipped(registry)
            val resolver = ActionResolver(catalog.presetKeys, registry.resolvedActions)
            val board = catalog.find(KeyboardId(SampleExtension.SAMPLE_KEYBOARD))!!
            resolver.resolve(board.keys.first().click) shouldBe ImeAction.Commit("★")
        }

        "extension toolbar entries carry typed actions" {
            val entry = registry.toolbarActions.single()
            entry.label shouldBe "扩展示例"
            entry.action shouldBe ImeAction.SwitchKeyboard(KeyboardId(SampleExtension.SAMPLE_KEYBOARD))
        }
    })
