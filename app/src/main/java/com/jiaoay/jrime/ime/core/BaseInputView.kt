/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.core

import android.view.ContextThemeWrapper
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.jiaoay.jrime.core.RimeMessage
import com.jiaoay.jrime.daemon.RimeSession
import com.jiaoay.jrime.data.theme.Theme
import com.jiaoay.jrime.data.theme.ThemeScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import splitties.views.dsl.core.withTheme

/**
 * The input view's message-pump base: owns the rime message subscription
 * switch ([handleMessages]) and the themed context the board windows build
 * their (placeholder) views with. Everything visual lived in the legacy
 * subclasses that the Compose migration deleted.
 */
abstract class BaseInputView(
    val service: TrimeInputMethodService,
    val rime: RimeSession,
    val scope: ThemeScope,
) : ConstraintLayout(service) {
    /** The theme config; geometry is theme-scoped, colors go through [scope.colors]. */
    val theme: Theme
        get() = scope.theme

    protected abstract fun handleRimeMessage(it: RimeMessage<*>)

    private var messageHandlerJob: Job? = null

    private fun setupRimeMessageHandler() {
        messageHandlerJob =
            service.lifecycleScope.launch {
                rime.run { messageFlow }.collect {
                    handleRimeMessage(it)
                }
            }
    }

    var handleMessages = false
        set(value) {
            field = value
            if (field) {
                if (messageHandlerJob == null) {
                    setupRimeMessageHandler()
                }
            } else {
                messageHandlerJob?.cancel()
                messageHandlerJob = null
            }
        }

    val themedContext: ContextThemeWrapper = context.withTheme(android.R.style.Theme_DeviceDefault_Settings)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // on API 35+, we must call requestApplyInsets() manually after replacing views,
        // otherwise View#onApplyWindowInsets won't be called. ¯\_(ツ)_/¯
        requestApplyInsets()
    }
}
