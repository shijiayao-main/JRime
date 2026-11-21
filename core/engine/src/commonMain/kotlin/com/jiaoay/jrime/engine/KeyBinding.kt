/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.engine

import com.jiaoay.jrime.data.theme.model.KeyActionToken
import com.jiaoay.jrime.data.theme.model.PresetKey
import com.jiaoay.jrime.data.theme.model.TextKeyboard
import com.jiaoay.jrime.theme.KeyLayouts

/**
 * The surface-level view of one key behaviour slot: the fields the UI needs to
 * render a key (label/hint/flags), plus the underlying behaviour token that the
 * app dispatches.
 *
 * M1 keeps the existing [KeyActionToken] as the behaviour carrier — the input
 * pipeline (Rime key processing, preset resolution, command handling) is
 * proven and is reused unchanged. The UI no longer reads the platform's
 * [com.jiaoay.jrime.ime.keyboard.KeyAction] at all, which is the boundary this
 * binding establishes. A fully typed action model is an M2 migration.
 */
data class KeyBinding(
    val token: KeyActionToken,
    val label: String = "",
    val hint: String = "",
    val preview: String? = null,
    val states: List<String> = emptyList(),
    val shiftLock: String = "",
    val repeatable: Boolean = false,
    val sticky: Boolean = false,
    val slideCursor: Boolean = false,
    val functional: Boolean = false,
    val isEnter: Boolean = false,
    val isSpace: Boolean = false,
) {
    /** The token's plain spelling, for layouts that key off the preset name. */
    val plainToken: String
        get() = (token as? KeyActionToken.Plain)?.token.orEmpty()

    companion object {
        /**
         * Resolves a layout token against the preset table. Returns null for an
         * absent token (a blank plain token means "no action").
         */
        fun resolve(
            token: KeyActionToken?,
            presetKeys: Map<String, PresetKey>,
        ): KeyBinding? {
            val normalized = token?.let { t ->
                if (t is KeyActionToken.Plain && t.token.isEmpty()) null else t
            } ?: return null
            return when (normalized) {
                is KeyActionToken.Plain -> {
                    val preset = presetKeys[normalized.token]
                    KeyBinding(
                        token = normalized,
                        label = preset?.label.orEmpty(),
                        preview = preset?.preview,
                        states = preset?.states.orEmpty(),
                        shiftLock = preset?.shiftLock.orEmpty(),
                        repeatable = preset?.repeatable ?: false,
                        sticky = preset?.sticky ?: false,
                        slideCursor = preset?.slideCursor ?: false,
                        functional = preset != null &&
                            normalized.token != "space" &&
                            normalized.token != "Alt_space",
                        isEnter = preset?.send == "Return",
                        isSpace = normalized.token == "space" || normalized.token == "Alt_space",
                    )
                }

                is KeyActionToken.Inline ->
                    KeyBinding(
                        token = normalized,
                        label = normalized.label.orEmpty(),
                    )
            }
        }
    }
}

/**
 * The keyboard data the surface renders from: the hardcoded layouts plus their
 * preset table. The app builds the single instance and injects it.
 */
class KeyboardCatalog(
    val keyboards: Map<KeyboardId, TextKeyboard>,
    val presetKeys: Map<String, PresetKey>,
) {
    fun board(id: KeyboardId): TextKeyboard = keyboards[id] ?: keyboards.getValue(DEFAULT_KEYBOARD)

    fun find(id: KeyboardId): TextKeyboard? = keyboards[id]

    companion object {
        val DEFAULT_KEYBOARD: KeyboardId = KeyboardId.Qwerty26

        /** Builds the catalog from the shipped hardcoded layouts. */
        fun shipped(): KeyboardCatalog = KeyboardCatalog(
            keyboards = KeyLayouts.keyboards.mapKeys { KeyboardId(it.key) },
            presetKeys = KeyLayouts.presetKeys,
        )
    }
}
