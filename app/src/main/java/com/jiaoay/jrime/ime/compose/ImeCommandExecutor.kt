/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import com.jiaoay.jrime.daemon.RimeSession
import com.jiaoay.jrime.daemon.launchOnReady
import com.jiaoay.jrime.data.prefs.AppPrefs
import com.jiaoay.jrime.data.theme.model.KeyActionToken
import com.jiaoay.jrime.data.theme.model.PresetKey
import com.jiaoay.jrime.engine.ImeCommand
import com.jiaoay.jrime.engine.KeyboardId
import com.jiaoay.jrime.engine.PanelId
import com.jiaoay.jrime.ime.core.TrimeInputMethodService
import com.jiaoay.jrime.ime.keyboard.CommonKeyboardActionListener
import com.jiaoay.jrime.ime.keyboard.KeyAction
import com.jiaoay.jrime.ime.keyboard.KeyboardWindow
import com.jiaoay.jrime.ime.symbol.LiquidWindow
import com.jiaoay.jrime.ime.window.BoardWindowManager
import timber.log.Timber

/**
 * Executes the surface's intents on the Android side. This is the one place
 * where the shared UI's [ImeCommand]s become engine/platform calls; the
 * behaviour of a key tap still rides the proven [KeyAction] pipeline, so the
 * M1 surface keeps the exact input semantics of the legacy tree.
 */
class ImeCommandExecutor(
    private val service: TrimeInputMethodService,
    private val rime: RimeSession,
    private val actionListener: CommonKeyboardActionListener,
    private val windowManager: BoardWindowManager,
    private val keyboardWindow: KeyboardWindow,
    private val liquidWindow: LiquidWindow,
    private val presetKeys: Map<String, PresetKey>,
) {
    fun execute(command: ImeCommand) {
        when (command) {
            is ImeCommand.Key -> executeAction(KeyAction(command.token, presetKeys))

            is ImeCommand.SelectCandidate ->
                rime.launchOnReady { it.selectCandidate(command.index, global = true) }

            is ImeCommand.LongPressCandidate ->
                rime.launchOnReady { it.deleteCandidate(command.index, global = true) }

            is ImeCommand.CommitText -> service.commitText(command.text)

            ImeCommand.Backspace -> dispatchToken(KeyActionToken.Plain("BackSpace"))

            ImeCommand.ToggleAscii -> dispatchToken(KeyActionToken.Plain("ascii_switch"))

            is ImeCommand.SetOption ->
                rime.launchOnReady { it.setRuntimeOption(command.option, command.value) }

            is ImeCommand.OpenPanel -> openPanel(command.id)

            ImeCommand.ClosePanel -> windowManager.attachWindow(KeyboardWindow)

            is ImeCommand.SwitchKeyboard -> keyboardWindow.switchKeyboard(command.id.value)

            is ImeCommand.MoveCursor -> moveCursor(command.chars, command.lines)

            is ImeCommand.SetRowHeight ->
                runCatching {
                    AppPrefs.defaultInstance().keyboard.rowHeight.setValue(command.dp)
                }.onFailure { Timber.w(it, "row height write failed") }

            // Palette persistence lands with the settings IA (M2); the state
            // is already carried in the session for the next surface.
            is ImeCommand.SetPaletteMode -> Unit

            ImeCommand.HideKeyboard -> service.requestHideSelf(0)
        }
    }

    /** Runs a pre-built legacy action (e.g. the top bar's clipboard chip). */
    fun executeAction(action: KeyAction) {
        actionListener.listener.onAction(action)
    }

    private fun dispatchToken(token: KeyActionToken) {
        executeAction(KeyAction(token, presetKeys))
    }

    private fun openPanel(id: PanelId) {
        when (id) {
            PanelId.Symbols -> {
                windowManager.attachWindow(liquidWindow)
                liquidWindow.setDataByIndex(0)
            }

            PanelId.Clipboard -> dispatchToken(KeyActionToken.Plain("clipboard_window"))

            PanelId.Emoji -> dispatchToken(KeyActionToken.Plain("liquid_keyboard_emoji"))

            PanelId.Editor -> dispatchToken(KeyActionToken.Plain("Keyboard_editor"))

            PanelId.Phrase -> dispatchToken(KeyActionToken.Plain("clipboard_window"))
        }
    }

    private fun moveCursor(
        chars: Int,
        lines: Int,
    ) {
        repeat(kotlin.math.abs(chars)) {
            dispatchToken(KeyActionToken.Plain(if (chars > 0) "Right" else "Left"))
        }
        repeat(kotlin.math.abs(lines)) {
            dispatchToken(KeyActionToken.Plain(if (lines > 0) "Down" else "Up"))
        }
    }

    companion object {
        /** The keyboard id the surface starts on when the app has none. */
        val DEFAULT_KEYBOARD: KeyboardId = KeyboardId.Qwerty26
    }
}
