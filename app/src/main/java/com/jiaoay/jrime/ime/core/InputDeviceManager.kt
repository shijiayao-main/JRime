/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.core

import android.text.InputType
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo

/**
 * Tracks whether the virtual keyboard should show: the Compose panel is
 * the only surface now, so the old candidates-view companion state is gone
 * and this is a plain latch that physical-keyboard events and editor
 * restarts flip.
 */
class InputDeviceManager(
    private val onChange: (Boolean) -> Unit,
) {
    var useVirtualKeyboard = true
        private set(value) {
            if (field == value) return
            field = value
            onChange(value)
        }

    fun setInputView(inputView: InputView) {
        inputView.handleMessages = useVirtualKeyboard
    }

    private var startedInputView = false
    private var isNullInputType = true

    fun evaluateOnStartInputView(
        info: EditorInfo,
        service: TrimeInputMethodService,
    ): Boolean {
        startedInputView = true
        isNullInputType = info.inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_NULL
        // The Compose panel is the only surface: the old DISABLED
        // candidates-mode branch always showed it, and the hard-keyboard
        // heuristics underneath answer false on this device class, leaving
        // the window touchable only in its bottom sliver.
        useVirtualKeyboard = true
        return useVirtualKeyboard
    }

    /**
     * @return should force show Input views
     */
    fun evaluateOnKeyDown(
        e: KeyEvent,
        service: TrimeInputMethodService,
    ): Boolean {
        if (startedInputView) {
            // filter out back/home/volume buttons and combination keys
            return false
        } else {
            // force show InputView when focusing on text input (likely inputType is not TYPE_NULL)
            // and pressing any digit/letter/punctuation key on physical keyboard
            val showInputView = !isNullInputType && e.unicodeChar != 0
            if (showInputView) {
                useVirtualKeyboard = true
            }
            return showInputView
        }
    }

    fun evaluateOnViewClicked(service: TrimeInputMethodService) {
        if (!startedInputView) return
        useVirtualKeyboard = true
    }

    fun evaluateOnUpdateEditorToolType(
        toolType: Int,
    ) {
        if (!startedInputView) return
        if (toolType == MotionEvent.TOOL_TYPE_FINGER || toolType == MotionEvent.TOOL_TYPE_STYLUS) {
            useVirtualKeyboard = true
        }
    }

    fun onFinishInputView() {
        startedInputView = false
    }
}
