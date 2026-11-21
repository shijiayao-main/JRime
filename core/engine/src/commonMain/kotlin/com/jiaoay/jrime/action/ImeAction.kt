/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.action

import com.jiaoay.jrime.engine.KeyboardId
import com.jiaoay.jrime.engine.PanelId

/**
 * The typed behaviour model of the IME surface.
 *
 * This replaces the YAML-era string pipe (`KeyAction` with code/text/command/
 * option/select/toggle/... fields): every key slot is resolved once, at model
 * build time, into one of these sealed actions. The app executes them directly,
 * so adding a feature means adding a typed action (or an extension that
 * registers one) — not inventing another string token plus another `when`
 * branch.
 */
sealed interface ImeAction {
    /** Press a Rime key: a named keysym, a character, or `Modifier+key`. */
    data class Key(
        val name: String,
    ) : ImeAction

    /** Commit literal text (symbol, newline, punctuation choice). */
    data class Commit(
        val text: String,
    ) : ImeAction

    /** Several actions in order (legacy text macros become explicit steps). */
    data class Sequence(
        val steps: List<ImeAction>,
    ) : ImeAction

    data class SwitchKeyboard(
        val keyboard: KeyboardId,
    ) : ImeAction

    data class OpenPanel(
        val panel: PanelId,
    ) : ImeAction

    data object ClosePanel : ImeAction

    data object HideKeyboard : ImeAction

    data object OpenSettings : ImeAction

    /** Show the system input-method picker. */
    data object ShowImePicker : ImeAction

    /** Switch to the preferred system voice input method. */
    data object VoiceInput : ImeAction

    /** Toggle a live Rime runtime option (ascii_mode, ascii_punct, ...). */
    data class ToggleOption(
        val option: String,
    ) : ImeAction

    data class PageCandidates(
        val forward: Boolean,
    ) : ImeAction

    data class SelectCandidate(
        val index: Int,
    ) : ImeAction

    data class DeleteCandidate(
        val index: Int,
    ) : ImeAction

    data class TextEdit(
        val op: TextEditOp,
    ) : ImeAction

    data class Maintenance(
        val op: MaintenanceOp,
    ) : ImeAction

    data class OpenUrl(
        val url: String,
    ) : ImeAction

    /**
     * A YAML-era capability JRime deliberately no longer exposes. Shipped
     * layouts must not contain these; the resolver test pins the (empty)
     * allowlist so new ones cannot slip in.
     */
    data class Unsupported(
        val token: String,
        val reason: String,
    ) : ImeAction
}

enum class TextEditOp {
    Copy,
    Cut,
    Paste,
    Undo,
    Redo,
    SelectAll,
    Clear,
}

enum class MaintenanceOp {
    Deploy,
    UpdateConfig,
}
