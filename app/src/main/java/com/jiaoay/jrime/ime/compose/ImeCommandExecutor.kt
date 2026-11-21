/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import com.jiaoay.jrime.action.ActionResolver
import com.jiaoay.jrime.action.ImeAction
import com.jiaoay.jrime.daemon.RimeSession
import com.jiaoay.jrime.daemon.launchOnReady
import com.jiaoay.jrime.data.prefs.AppPrefs
import com.jiaoay.jrime.data.theme.model.KeyActionToken
import com.jiaoay.jrime.data.theme.model.PresetKey
import com.jiaoay.jrime.engine.CandidateActionKind
import com.jiaoay.jrime.engine.ImeCommand
import com.jiaoay.jrime.engine.KeyboardId
import com.jiaoay.jrime.engine.PanelId
import com.jiaoay.jrime.ime.core.TrimeInputMethodService
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
    private val presetKeys: Map<String, PresetKey>,
    private val extraActions: Map<String, ImeAction> = emptyMap(),
    /** Whether a Shift tap is allowed to toggle Chinese/English. */
    private val shiftTogglesLanguage: () -> Boolean = { true },
) {
    private val resolver = ActionResolver(presetKeys, extraActions)

    private val dispatcher = RimeActionDispatcher(service = service, rime = rime)
    fun execute(command: ImeCommand) {
        when (command) {
            is ImeCommand.Key -> {
                val shiftBlocked =
                    !shiftTogglesLanguage() &&
                        command.token == KeyActionToken.Plain("Shift_L")
                if (!shiftBlocked) dispatcher.dispatch(resolver.resolve(command.token))
            }

            is ImeCommand.Typed -> dispatcher.dispatch(command.action)

            is ImeCommand.ChangeCandidatePage,
            is ImeCommand.SetCandidatePageSize,
            is ImeCommand.SetAccent,
            -> Unit

            is ImeCommand.CandidateAction -> handleCandidateAction(command)

            is ImeCommand.SelectCandidate ->
                rime.launchOnReady { it.selectCandidate(command.index, global = true) }

            is ImeCommand.LongPressCandidate ->
                rime.launchOnReady { it.deleteCandidate(command.index, global = true) }

            is ImeCommand.CommitText -> service.commitText(command.text)

            ImeCommand.Backspace -> dispatcher.dispatch(ImeAction.Key("BackSpace"))

            ImeCommand.ToggleAscii -> dispatcher.dispatch(ImeAction.ToggleOption("ascii_mode"))

            is ImeCommand.SetOption ->
                rime.launchOnReady { it.setRuntimeOption(command.option, command.value) }

            is ImeCommand.SetSimplified ->
                rime.launchOnReady { api ->
                    api.setRuntimeOption("zh_simp", command.simplified)
                    api.setRuntimeOption("zh_trad", !command.simplified)
                }

            // Surface-only state: the controller already reduced the command.
            is ImeCommand.OpenPanel, ImeCommand.ClosePanel, is ImeCommand.SwitchKeyboard -> Unit

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

    private fun handleCandidateAction(command: ImeCommand.CandidateAction) {
        when (command.action) {
            CandidateActionKind.Pin -> dispatcher.dispatch(resolver.resolve(KeyActionToken.Plain("PinCandFunc")))

            CandidateActionKind.Delete ->
                rime.launchOnReady { it.deleteCandidate(command.index, global = true) }

            CandidateActionKind.Copy -> {
                val clipboard = service.getSystemService(android.content.ClipboardManager::class.java)
                clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("candidate", command.text))
            }

            // The encoding lookup lands with the M3 hint batch.
            CandidateActionKind.QueryCode -> Unit
        }
    }

    private fun moveCursor(
        chars: Int,
        lines: Int,
    ) {
        repeat(kotlin.math.abs(chars)) {
            dispatcher.dispatch(ImeAction.Key(if (chars > 0) "Right" else "Left"))
        }
        repeat(kotlin.math.abs(lines)) {
            dispatcher.dispatch(ImeAction.Key(if (lines > 0) "Down" else "Up"))
        }
    }

    companion object {
        /** The keyboard id the surface starts on when the app has none. */
        val DEFAULT_KEYBOARD: KeyboardId = KeyboardId.Qwerty26
    }
}
