/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.extension

import com.jiaoay.jrime.action.ImeAction
import com.jiaoay.jrime.data.theme.model.KeyActionToken
import com.jiaoay.jrime.data.theme.model.TextKeyboard
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

    val toolbarActions: List<ExtensionToolbarAction> =
        buildList {
            val ids = mutableSetOf<String>()
            all.forEach { extension ->
                extension.toolbarActions().forEach { entry ->
                    require(entry.id.isNotBlank()) { "extension ${extension.id} registered a blank toolbar id" }
                    require(ids.add(entry.id)) { "toolbar entry '${entry.id}' registered twice (extension ${extension.id})" }
                    add(entry)
                }
            }
        }

    /**
     * Keyboard slots are authored as typed actions; the surface still renders
     * the frozen [TextKeyboard] model, so the bridge generates an internal
     * token per slot/gesture and resolves it through [resolvedActions].
     */
    private val keyboardSlotActions: Map<String, ImeAction> =
        buildMap {
            keyboards.forEach { (keyboardId, keyboard) ->
                keyboard.keys.forEachIndexed { index, slot ->
                    slotActions(slot).forEach { (gesture, action) ->
                        put(slotToken(keyboardId, index, gesture), action)
                    }
                }
            }
        }

    /** Everything [com.jiaoay.jrime.action.ActionResolver] should know about. */
    val resolvedActions: Map<String, ImeAction> = actions + keyboardSlotActions

    /** Extension keyboards adapted to the current surface renderer. */
    val keyboardBoards: Map<KeyboardId, TextKeyboard> =
        keyboards.mapValues { (keyboardId, keyboard) ->
            val keys =
                keyboard.keys.mapIndexed { index, slot ->
                    TextKeyboard.TextKey(
                        label = slot.label,
                        width = slot.width.takeIf { it > 0f } ?: 1f,
                        click = slot.click?.let { KeyActionToken.Plain(slotToken(keyboardId, index, "click")) },
                        longClick = slot.longPress?.let { KeyActionToken.Plain(slotToken(keyboardId, index, "long")) },
                        swipeUp = slot.swipeUp?.let { KeyActionToken.Plain(slotToken(keyboardId, index, "up")) },
                        swipeDown = slot.swipeDown?.let { KeyActionToken.Plain(slotToken(keyboardId, index, "down")) },
                        swipeLeft = slot.swipeLeft?.let { KeyActionToken.Plain(slotToken(keyboardId, index, "left")) },
                        swipeRight = slot.swipeRight?.let { KeyActionToken.Plain(slotToken(keyboardId, index, "right")) },
                    )
                }
            TextKeyboard(
                width = keyboard.columns.toFloat(),
                columns = keyboard.columns,
                keys = keys,
            )
        }

    private fun slotActions(slot: KeySlot): Map<String, ImeAction> = buildMap {
        slot.click?.let { put("click", it) }
        slot.longPress?.let { put("long", it) }
        slot.swipeUp?.let { put("up", it) }
        slot.swipeDown?.let { put("down", it) }
        slot.swipeLeft?.let { put("left", it) }
        slot.swipeRight?.let { put("right", it) }
    }

    private fun slotToken(
        keyboardId: KeyboardId,
        index: Int,
        gesture: String,
    ): String = "__ext__${keyboardId.value}:$index:$gesture"
}
