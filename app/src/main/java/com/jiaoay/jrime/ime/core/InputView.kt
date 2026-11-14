/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.core

import android.view.ContextThemeWrapper
import android.view.inputmethod.EditorInfo
import com.jiaoay.jrime.core.RimeMessage
import com.jiaoay.jrime.daemon.RimeSession
import com.jiaoay.jrime.data.theme.Theme
import com.jiaoay.jrime.data.theme.ThemeScope
import com.jiaoay.jrime.ime.broadcast.InputBroadcastReceiver
import com.jiaoay.jrime.ime.broadcast.InputBroadcaster
import com.jiaoay.jrime.ime.keyboard.CommonKeyboardActionListener
import com.jiaoay.jrime.ime.keyboard.KeyboardWindow
import com.jiaoay.jrime.ime.symbol.LiquidWindow
import com.jiaoay.jrime.ime.window.BoardWindowManager
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.allInstances
import org.kodein.di.bindInstance
import org.kodein.di.bindSingleton
import org.kodein.di.instance

/**
 * The Compose era's [InputView]: the legacy view tree is gone, and what
 * remains is the machinery the Compose tree still rides on -- the DI
 * container (the board windows, the keyboard machinery, the shared
 * broadcaster), the rime message bridge that fans engine messages out to
 * every receiver, and the lifecycle calls the service forwards here.
 *
 * The pixels come from the [ComposeInputView]'s composition; subclasses that
 * still need a view tree build their own.
 */
@Suppress("ViewConstructor")
abstract class InputView(
    service: TrimeInputMethodService,
    rime: RimeSession,
    scope: ThemeScope,
) : BaseInputView(service, rime, scope),
    DIAware {
    private val broadcaster: InputBroadcaster by instance()
    private val windowManager: BoardWindowManager by instance()
    private val keyboardWindow: KeyboardWindow by instance()
    private val liquidWindow: LiquidWindow by instance()

    private var lastAppearanceState = Triple(false, false, false)

    private fun broadcastKeyAppearanceUpdate() {
        val composing = rime.run { statusCached.isComposing }
        val hasMenu = rime.run { hasMenu }
        val paging = rime.run { paging }
        val current = Triple(composing, hasMenu, paging)
        if (current != lastAppearanceState) {
            lastAppearanceState = current
            broadcaster.onKeyAppearanceUpdate(current.first, current.second, current.third)
        }
    }

    override val di = DI {
        bindInstance<InputView> { this@InputView }
        bindInstance<ContextThemeWrapper> { themedContext }
        bindInstance<ThemeScope> { scope }
        bindInstance<Theme> { scope.theme }
        bindInstance<TrimeInputMethodService> { service }
        bindInstance<RimeSession> { rime }
        bindSingleton { InputBroadcaster() }
        bindSingleton { CommonKeyboardActionListener(di) }
        bindSingleton { BoardWindowManager(di) }
        bindSingleton { KeyboardWindow(di) }
        bindSingleton { LiquidWindow(di) }
    }

    init {
        // MUST call before any operation
        val receivers: List<InputBroadcastReceiver> by allInstances()
        receivers.forEach { broadcaster.addReceiver(it) }

        windowManager.cacheResidentWindow(keyboardWindow)
        windowManager.cacheResidentWindow(liquidWindow)
        // show KeyboardWindow by default
        windowManager.attachWindow(KeyboardWindow)
    }

    /** Releases the built keyboards that are not currently shown. */
    fun trimMemory() {
        keyboardWindow.trimCache()
    }

    /** Restyles after a scheme switch; the Compose tree restyles itself. */
    open fun refreshColors() {}

    fun startInput(
        info: EditorInfo,
        restarting: Boolean = false,
    ) {
        broadcaster.onStartInput(info)
        if (!restarting) {
            windowManager.attachWindow(KeyboardWindow)
        }
    }

    fun updateSelection(
        start: Int,
        end: Int,
    ) {
        broadcaster.onSelectionUpdate(start, end)
    }

    override fun handleRimeMessage(it: RimeMessage<*>) {
        when (it) {
            is RimeMessage.SchemaMessage -> {
                broadcaster.onRimeSchemaUpdated(it.data)

                windowManager.attachWindow(KeyboardWindow)
            }

            is RimeMessage.OptionMessage -> {
                broadcaster.onRimeOptionUpdated(it.data)

                if (it.data.option == "_liquid_keyboard") {
                    windowManager.attachWindow(liquidWindow)
                    liquidWindow.setDataByIndex(0)
                }
            }

            is RimeMessage.CompositionMessage -> {
                broadcaster.onCompositionUpdate(it.data)
            }

            is RimeMessage.BulkCandidatesMessage -> {
                broadcaster.onCandidateListUpdate(it.data)
            }

            else -> {}
        }
        broadcastKeyAppearanceUpdate()
    }
}
