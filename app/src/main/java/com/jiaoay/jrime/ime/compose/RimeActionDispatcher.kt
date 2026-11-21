/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.jiaoay.jrime.R
import com.jiaoay.jrime.action.ImeAction
import com.jiaoay.jrime.action.MaintenanceOp
import com.jiaoay.jrime.action.TextEditOp
import com.jiaoay.jrime.core.KeyModifiers
import com.jiaoay.jrime.daemon.RimeSession
import com.jiaoay.jrime.daemon.launchOnReady
import com.jiaoay.jrime.data.prefs.AppPrefs
import com.jiaoay.jrime.ime.core.TrimeInputMethodService
import com.jiaoay.jrime.ime.keyboard.KeyCode
import com.jiaoay.jrime.util.AppUtils
import com.jiaoay.jrime.util.InputMethodUtils
import com.jiaoay.jrime.util.toast
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Executes the typed [ImeAction] model on Android.
 *
 * This replaces the YAML-era `CommonKeyboardActionListener` string interpreter
 * on the Compose surface: the key's behaviour is resolved once by
 * `ActionResolver`, and every branch here is a typed model case. Panels and
 * keyboards still ride the legacy resident-window host until the P3 panel
 * migration; the callbacks make that seam explicit instead of hiding it in a
 * command string.
 */
class RimeActionDispatcher(
    private val service: TrimeInputMethodService,
    private val rime: RimeSession,
) {
    private val prefs = AppPrefs.defaultInstance()
    private var lastIdleSpaceAt = 0L

    fun dispatch(action: ImeAction) {
        when (action) {
            is ImeAction.Key -> pressKey(action.name)

            is ImeAction.Commit -> service.commitText(action.text)

            is ImeAction.Sequence -> action.steps.forEach(::dispatch)

            // Panel and keyboard switches are surface state only; the
            // controller reduces those commands before the dispatcher runs.
            is ImeAction.SwitchKeyboard, is ImeAction.OpenPanel, ImeAction.ClosePanel -> Unit

            ImeAction.HideKeyboard -> service.requestHideSelf(0)

            ImeAction.OpenSettings -> AppUtils.launchMainActivity(service)

            ImeAction.ShowImePicker ->
                service.getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()

            ImeAction.VoiceInput -> switchToVoiceInputMethod()

            is ImeAction.ToggleOption -> toggleOption(action.option)

            is ImeAction.PageCandidates ->
                rime.launchOnReady { api ->
                    api.changeCandidatePage(backward = !action.forward)
                }

            is ImeAction.SelectCandidate ->
                rime.launchOnReady { api -> api.selectCandidate(action.index, global = true) }

            is ImeAction.DeleteCandidate ->
                rime.launchOnReady { api -> api.deleteCandidate(action.index, global = true) }

            is ImeAction.TextEdit -> textEdit(action.op)

            is ImeAction.Maintenance -> maintenance(action.op)

            is ImeAction.OpenUrl -> openUrl(action.url)

            is ImeAction.Unsupported ->
                Timber.w("unhandled action: %s (%s)", action.token, action.reason)
        }
    }

    // -- keys --------------------------------------------------------------
    private fun pressKey(name: String) {
        val (code, modifier) = KeyCode.parse(name)
        if (code == 0) {
            // A layout literal the key map does not know: commit it verbatim.
            if (name.isNotBlank()) service.commitText(name)
            return
        }
        pressKey(code, modifier)
    }

    private fun pressKey(
        code: Int,
        modifier: Int,
    ) {
        if (code == KeyEvent.KEYCODE_SPACE &&
            modifier == 0 &&
            prefs.keyboard.doubleSpacePeriod.getValue() &&
            !rime.run { statusCached }.isComposing
        ) {
            val now = SystemClock.uptimeMillis()
            val gap = now - lastIdleSpaceAt
            lastIdleSpaceAt = now
            if (isDoubleSpaceWindow(gap)) {
                service.currentInputConnection?.let { ic ->
                    if (ic.getTextBeforeCursor(1, 0) == " ") {
                        ic.deleteSurroundingText(1, 0)
                        service.commitText("。")
                        return
                    }
                }
            }
        }

        forwardKey(code, modifier)
    }

    /** The idle double-space window (same cadence as the old input pipeline). */
    private fun isDoubleSpaceWindow(gapMs: Long): Boolean = gapMs in 120L..350L

    private fun isNavigationKey(code: Int): Boolean = code in KeyEvent.KEYCODE_DPAD_UP..KeyEvent.KEYCODE_DPAD_RIGHT ||
        code == KeyEvent.KEYCODE_MOVE_HOME ||
        code == KeyEvent.KEYCODE_MOVE_END

    private fun forwardKey(
        keyEventCode: Int,
        metaState: Int,
    ) {
        val value =
            if (metaState and KeyEvent.META_SHIFT_ON != 0 &&
                keyEventCode in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z
            ) {
                'A'.code + (keyEventCode - KeyEvent.KEYCODE_A)
            } else {
                val name = KeyCode.codeToKeyName(keyEventCode) ?: "VoidSymbol"
                com.jiaoay.jrime.core.RimeKeyEvent.getKeycodeByName(name)
            }
        val m =
            if (keyEventCode in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_EQUALS) {
                metaState or KeyEvent.META_NUM_LOCK_ON
            } else {
                metaState
            }
        val modifiers = KeyModifiers.fromMetaState(m).modifiers
        service.postRimeJob {
            if (service.hookKeyboard(keyEventCode, m)) return@postRimeJob
            if (processKey(value, modifiers, isVirtual = true)) return@postRimeJob
            if (AppUtils.launchKeyCategory(service, keyEventCode)) return@postRimeJob
            if (isNavigationKey(keyEventCode)) {
                val downTime = SystemClock.uptimeMillis()
                service.currentInputConnection?.let { ic ->
                    ic.sendKeyEvent(KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyEventCode, 0, m))
                    ic.sendKeyEvent(KeyEvent(downTime, downTime + 50, KeyEvent.ACTION_UP, keyEventCode, 0, m))
                }
                return@postRimeJob
            }
            if (keyEventCode == KeyEvent.KEYCODE_BACK) service.requestHideSelf(0)
        }
    }

    // -- options -----------------------------------------------------------
    private fun toggleOption(option: String) {
        rime.launchOnReady { api ->
            val enabled = api.getRuntimeOption(option)
            val composing = api.statusCached.isComposing
            api.setRuntimeOption(option, !enabled)
            if (option == "ascii_mode" && composing) {
                api.getRawInput().takeIf { it.isNotEmpty() }?.let {
                    service.commitText(it)
                    api.clearComposition()
                }
            }
        }
    }

    // -- text editing ------------------------------------------------------
    private fun textEdit(op: TextEditOp) {
        val ic = service.currentInputConnection ?: return
        when (op) {
            TextEditOp.Copy -> ic.performContextMenuAction(android.R.id.copy)

            TextEditOp.Cut -> ic.performContextMenuAction(android.R.id.cut)

            TextEditOp.Paste -> ic.performContextMenuAction(android.R.id.paste)

            TextEditOp.SelectAll -> ic.performContextMenuAction(android.R.id.selectAll)

            TextEditOp.Undo -> ic.performContextMenuAction(android.R.id.undo)

            TextEditOp.Redo -> ic.performContextMenuAction(android.R.id.redo)

            TextEditOp.Clear -> {
                ic.performContextMenuAction(android.R.id.selectAll)
                val now = SystemClock.uptimeMillis()
                ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0))
                ic.sendKeyEvent(KeyEvent(now, now + 30, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL, 0))
            }
        }
    }

    // -- platform actions --------------------------------------------------
    private fun switchToVoiceInputMethod() {
        val pkgName = prefs.general.preferredVoiceInput.getValue()
        val voice =
            if (pkgName.isNotEmpty()) {
                InputMethodUtils.voiceInputMethods().find { it.first.packageName == pkgName }?.let {
                    it.first.id to it.second
                } ?: InputMethodUtils.firstVoiceInput()
            } else {
                InputMethodUtils.firstVoiceInput()
            }
        if (voice != null) {
            InputMethodUtils.switchInputMethod(service, voice.first, voice.second)
        } else {
            service.toast(R.string.no_voice_input_installed)
        }
    }

    private fun maintenance(op: MaintenanceOp) {
        when (op) {
            MaintenanceOp.Deploy ->
                rime.launchOnReady { api ->
                    Timber.i("try to start maintenance via typed action")
                    api.deploy()
                }

            MaintenanceOp.UpdateConfig ->
                rime.launchOnReady { api ->
                    api.updateConfig()
                    service.lifecycleScope.launch {
                        Toast.makeText(service, R.string.done, Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun openUrl(url: String) {
        if (url.isBlank()) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY
        runCatching { service.startActivity(intent) }
            .onFailure { Timber.w(it, "cannot open %s", url) }
    }
}
