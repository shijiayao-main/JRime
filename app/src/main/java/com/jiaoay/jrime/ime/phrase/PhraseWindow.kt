/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.phrase

import android.view.View
import com.jiaoay.jrime.ime.window.BoardWindow
import org.kodein.di.DI

/**
 * The 常用语 panel window: a data-only board window (the Compose surface
 * renders the slot; this object only carries the attach state).
 */
class PhraseWindow(di: DI) : BoardWindow.NoBarBoardWindow(di) {
    override fun onCreateView(): View = View(context)

    override fun onAttached() = Unit

    override fun onDetached() = Unit
}
