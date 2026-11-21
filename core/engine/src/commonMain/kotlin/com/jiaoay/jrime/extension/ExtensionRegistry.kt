/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.extension

import com.jiaoay.jrime.action.ImeAction
import com.jiaoay.jrime.engine.KeyboardId
import com.jiaoay.jrime.engine.PanelId

/**
 * Merges the shipped app extension with any additional [JrimeExtension]s.
 * Conflicts are reported at construction time instead of silently shadowing:
 * model-first means the composition is explicit and testable.
 */
class ExtensionRegistry(
    extensions: List<JrimeExtension>,
) {
    init {
        val duplicateIds = extensions.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicateIds.isEmpty()) { "duplicate extension ids: $duplicateIds" }
    }

    private val all = extensions

    val actions: Map<String, ImeAction> =
        buildMap {
            all.forEach { extension ->
                extension.actions().forEach { (token, action) ->
                    require(token.isNotBlank()) { "extension ${extension.id} registered a blank action token" }
                    require(put(token, action) == null) {
                        "action '$token' registered twice (extension ${extension.id})"
                    }
                }
            }
        }

    val keyboards: Map<KeyboardId, KeyboardExtension> =
        buildMap {
            all.forEach { extension ->
                extension.keyboards().forEach { keyboard ->
                    require(put(keyboard.id, keyboard) == null) {
                        "keyboard '${keyboard.id}' registered twice (extension ${extension.id})"
                    }
                }
            }
        }

    val panels: Map<PanelId, PanelDescriptor> =
        buildMap {
            all.forEach { extension ->
                extension.panels().forEach { panel ->
                    require(put(panel.id, panel) == null) {
                        "panel '${panel.id}' registered twice (extension ${extension.id})"
                    }
                }
            }
        }

    val inputRules: List<InputRuleDescriptor> = all.flatMap { it.inputRules() }
}
