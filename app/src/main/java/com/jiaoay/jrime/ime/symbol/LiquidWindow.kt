/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.symbol

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.jiaoay.jrime.daemon.RimeSession
import com.jiaoay.jrime.daemon.launchOnReady
import com.jiaoay.jrime.data.SymbolHistory
import com.jiaoay.jrime.data.theme.ThemeScope
import com.jiaoay.jrime.data.theme.model.LiquidKeyboard
import com.jiaoay.jrime.ime.core.TrimeInputMethodService
import com.jiaoay.jrime.ime.keyboard.KeyboardWindow
import com.jiaoay.jrime.ime.window.BoardWindow
import com.jiaoay.jrime.ime.window.BoardWindowManager
import com.jiaoay.jrime.ime.window.ResidentWindow
import org.kodein.di.DI
import org.kodein.di.instance

/**
 * The liquid pages' state machine: which tab is active, its keys, and the
 * per-type tap dispatch. Rendering lives in the Compose LiquidPanel (the tab
 * strip and the key grid); this window only carries data and behavior.
 */
class LiquidWindow(di: DI) :
    BoardWindow.BarBoardWindow(di),
    ResidentWindow {
    override val showTitle = false

    private val service: TrimeInputMethodService by instance()
    private val rime: RimeSession by instance()
    private val scope: ThemeScope by instance()
    private val windowManager: BoardWindowManager by instance()

    private val symbolHistory = SymbolHistory(180)
    var currentDataType: LiquidKeyboard.DataType = LiquidKeyboard.DataType.SINGLE
        private set

    /** The keys of the active tab; the Compose grid renders these. */
    var currentKeys by mutableStateOf(emptyList<LiquidKeyboard.KeyItem>())
        private set

    /** The active tab's index, for the Compose tab strip's highlight. */
    var currentTabIndex by mutableStateOf(-1)
        private set

    /** Every page tab, in order -- the Compose tab strip renders these. */
    val tabs: List<LiquidKeyboard.Tag>
        get() = scope.theme.liquidKeyboard.getTagList()

    companion object : ResidentWindow.Key

    override val key: ResidentWindow.Key
        get() = LiquidWindow

    override fun onCreateView(): View = View(context)

    override fun onCreateBarView(): View? = null

    override fun onAttached() {}

    override fun onDetached() {}

    fun setDataByIndex(i: Int) {
        val tag = scope.theme.liquidKeyboard.getTagList()[i]
        currentDataType = tag.type
        currentTabIndex = i
        when (tag.type) {
            LiquidKeyboard.DataType.HISTORY -> {
                symbolHistory.load()
                currentKeys = symbolHistory.toOrderedList().map { LiquidKeyboard.KeyItem(it) }
            }

            else -> {
                currentKeys = scope.theme.liquidKeyboard.getDataByIndex(i)
            }
        }
    }

    /**
     * One entry point for a key tap on the Compose grid: symbol pages replay
     * through the engine (with the ascii-mode round-trip), plain pages
     * commit and feed the history.
     */
    fun tap(item: LiquidKeyboard.KeyItem) {
        when (currentDataType) {
            LiquidKeyboard.DataType.SYMBOL -> triggerSymbolInput(item.text)

            else -> {
                service.commitText(item.text)
                if (currentDataType != LiquidKeyboard.DataType.HISTORY) {
                    symbolHistory.insert(item.text)
                    symbolHistory.save()
                }
            }
        }
    }

    private fun triggerSymbolInput(symbol: String) {
        rime.launchOnReady {
            val (isAsciiMode, isAsciiPunch) = it.statusCached.run { isAsciiMode to isAsciiPunct }
            if (isAsciiMode) it.setRuntimeOption("ascii_mode", false)
            if (isAsciiPunch) it.setRuntimeOption("ascii_punct", false)
            it.clearComposition()
            it.simulateKeySequence(symbol)
            if (isAsciiPunch) it.setRuntimeOption("ascii_punct", true)
            ContextCompat.getMainExecutor(service).execute {
                windowManager.attachWindow(KeyboardWindow)
            }
        }
    }
}
