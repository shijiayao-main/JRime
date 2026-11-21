/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.ime

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jiaoay.jrime.engine.ImeSessionState

/**
 * The content band host: the registered panel when one is open, otherwise the
 * keyboard. Both surfaces render into the same fixed height, so opening a
 * panel never moves the band.
 */
@Suppress("FunctionName")
@Composable
fun ImePanelHost(
    state: ImeSessionState,
    config: ImeSurfaceConfig,
    host: ImeSurfaceHost,
    contentRows: Int,
    keyboard: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slot =
        state.activePanel?.let { id ->
            config.panelSlots.firstOrNull { it.id == id }
        }
    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .height((contentRows * config.rowHeightDp).dp),
    ) {
        if (slot != null) {
            slot.content(Modifier.fillMaxSize())
        } else {
            keyboard()
        }
    }
}
