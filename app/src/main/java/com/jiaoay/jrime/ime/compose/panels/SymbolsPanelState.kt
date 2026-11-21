/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose.panels

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

/**
 * Compose-native symbol panel state (P3): the active tab, its keys, and the
 * per-type tap dispatch, without a `BoardWindow` host. Rendering stays in the
 * shared `LiquidPanel` composable.
 */
class SymbolsPanelState(
    private val service: TrimeInputMethodService,
    private val rime: RimeSession,
    private val scope: ThemeScope,
    private val onClosePanel: () -> Unit,
) {
    private val symbolHistory = SymbolHistory(180)
    var currentDataType: LiquidKeyboard.DataType = LiquidKeyboard.DataType.SINGLE
        private set

    var currentKeys by mutableStateOf(emptyList<LiquidKeyboard.KeyItem>())
        private set

    var currentTabIndex by mutableStateOf(-1)
        private set

    val tabs: List<LiquidKeyboard.Tag>
        get() = scope.theme.liquidKeyboard.getTagList()

    fun setDataByIndex(i: Int) {
        val tag = tabs[i]
        currentDataType = tag.type
        currentTabIndex = i
        when (tag.type) {
            LiquidKeyboard.DataType.HISTORY -> {
                symbolHistory.load()
                currentKeys = symbolHistory.toOrderedList().map { LiquidKeyboard.KeyItem(it) }
            }

            else -> currentKeys = scope.theme.liquidKeyboard.getDataByIndex(i)
        }
    }

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
            ContextCompat.getMainExecutor(service).execute { onClosePanel() }
        }
    }
}
