/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.action

import com.jiaoay.jrime.data.theme.model.KeyActionToken
import com.jiaoay.jrime.data.theme.model.PresetKey
import com.jiaoay.jrime.engine.KeyboardId
import com.jiaoay.jrime.engine.PanelId

/**
 * Resolves a layout slot (the legacy token strings still used by the board
 * data) into a typed [ImeAction]. This is the single bridge between the old
 * preset table and the model-first surface; once every layout is authored with
 * typed actions the bridge and the preset table go away.
 */
class ActionResolver(
    private val presets: Map<String, PresetKey>,
    private val extraActions: Map<String, ImeAction> = emptyMap(),
) {
    fun resolve(token: KeyActionToken?): ImeAction {
        val raw =
            when (token) {
                null -> return ImeAction.Unsupported("", "empty slot")
                is KeyActionToken.Inline -> return ImeAction.Unsupported("inline", "inline tokens are legacy")
                is KeyActionToken.Plain -> token.token
            }
        if (raw.isEmpty()) return ImeAction.Unsupported("", "empty token")
        extraActions[raw]?.let { return it }
        presets[raw]?.let { preset -> return fromPreset(raw, preset) }
        return fromToken(raw)
    }

    private fun fromPreset(
        name: String,
        preset: PresetKey,
    ): ImeAction {
        preset.send.ifEmpty { null }?.let { send -> semantic(send, preset)?.let { return it } }
        preset.toggle.ifEmpty { null }?.let { return ImeAction.ToggleOption(it) }
        preset.commit.ifEmpty { null }?.let { return ImeAction.Commit(it) }
        preset.command.ifEmpty { null }?.let { return command(it, preset.option) }
        preset.text.ifEmpty { null }?.let { return textMacro(it) }
        return ImeAction.Unsupported(name, "preset has no executable field")
    }

    /** Maps the legacy `send` keysym vocabulary onto typed actions. */
    private fun semantic(
        send: String,
        preset: PresetKey,
    ): ImeAction? = when (send) {
        "SWITCH_CHARSET" -> preset.toggle.takeIf { it.isNotEmpty() }?.let { ImeAction.ToggleOption(it) }
        "Eisu_toggle" -> preset.select.takeIf { it.isNotEmpty() }?.let { ImeAction.SwitchKeyboard(KeyboardId(it)) }
        "FUNCTION" -> command(preset.command, preset.option)
        "SETTINGS" -> ImeAction.OpenSettings
        "BACK" -> ImeAction.HideKeyboard
        "LANGUAGE_SWITCH", "MENU" -> ImeAction.ShowImePicker
        "VOICE_ASSIST" -> ImeAction.VoiceInput
        else -> ImeAction.Key(send)
    }

    private fun command(
        command: String,
        option: String,
    ): ImeAction = when (command) {
        "liquid_keyboard" -> ImeAction.OpenPanel(panelForLiquid(option))

        "clipboard_window" -> ImeAction.OpenPanel(PanelId.Clipboard)

        "back_to_keyboard", "menu_keyboard" -> ImeAction.ClosePanel

        "commit" -> ImeAction.Commit(option)

        "run" -> ImeAction.OpenUrl(option)

        "date" -> ImeAction.Commit(option)

        // callers pass the formatted value
        "select_candidate" -> option.toIntOrNull()?.let { ImeAction.SelectCandidate(it) } ?: ImeAction.Unsupported(command, "bad index")

        "DEPLOY" -> ImeAction.Maintenance(MaintenanceOp.Deploy)

        "UPDATE_CONFIG" -> ImeAction.Maintenance(MaintenanceOp.UpdateConfig)

        "share_text", "clipboard", "apply", "broadcast", "set_color_scheme",
        "switch_hide_key_symbol", "switch_hide_key_hint",
        -> ImeAction.Unsupported(command, "YAML-era command removed")

        else -> ImeAction.Unsupported(command, "unknown command")
    }

    private fun panelForLiquid(option: String): PanelId = when (option.lowercase()) {
        "😀", "emoji" -> PanelId.Emoji
        "剪贴", "clipboard" -> PanelId.Clipboard
        else -> PanelId.Symbols
    }

    /**
     * Preset `text` values are either literal commits or the few YAML-era edit
     * macros that the product still wants. Those macros become explicit typed
     * steps instead of a script string.
     */
    private fun textMacro(text: String): ImeAction = when (text) {
        "{Control+a}{Control+c}" -> sequence(TextEditOp.SelectAll, TextEditOp.Copy)

        "{Control+a}{Control+x}" -> sequence(TextEditOp.SelectAll, TextEditOp.Cut)

        "{Control+a}{BackSpace}" -> sequence(TextEditOp.SelectAll, TextEditOp.Clear)

        else ->
            if (text.contains('{')) {
                ImeAction.Unsupported(text, "legacy text macro")
            } else {
                ImeAction.Commit(text)
            }
    }

    private fun sequence(vararg ops: TextEditOp): ImeAction = ImeAction.Sequence(ops.map { ImeAction.TextEdit(it) })

    /** Non-preset slots: characters and keysym spellings are direct Rime keys. */
    private fun fromToken(token: String): ImeAction = when {
        token.contains('+') -> ImeAction.Key(token)

        token.length == 1 -> ImeAction.Key(token)

        token in KNOWN_KEYS -> ImeAction.Key(token)

        // Multi-character layout literals ("……", "♠︎") are direct commits.
        token.any { it.isLetterOrDigit() } || token.none { it.isLetterOrDigit() } -> ImeAction.Commit(token)

        else -> ImeAction.Unsupported(token, "unknown token")
    }

    private companion object {
        val KNOWN_KEYS =
            setOf(
                "BackSpace", "Delete", "Return", "space", "Tab", "Escape",
                "Up", "Down", "Left", "Right", "Home", "End", "Page_Up", "Page_Down",
                "Shift_L", "Shift_R", "Control_L", "Control_R", "Alt_L", "Alt_R",
                "Caps_Lock", "VoidSymbol",
            )
    }
}
