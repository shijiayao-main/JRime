/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiaoay.jrime.ime.compose.panels.SymbolsPanelState
import com.jiaoay.jrime.ime.compose.rememberKeyboardRowHeightDp
import com.jiaoay.jrime.ui.design.ImePalette
import com.jiaoay.jrime.ui.design.LocalImePalette

/**
 * The liquid symbol pages: a horizontally scrolling tab strip above the key
 * grid of the active tab. Taps dispatch through [LiquidWindow.tap] (symbol
 * pages replay through the engine, plain pages commit); the whole panel
 * lives inside the keyboard band's bound, the grid scrolling below.
 */
@Suppress("FunctionName")
@Composable
fun LiquidPanel(
    state: SymbolsPanelState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalImePalette.current
    // The liquid band renders the same content height as the keyboard
    // (KEYBOARD_CONTENT_ROWS x the row-height preference) so switching
    // surfaces never jumps the band.
    val rowHeightDp = rememberKeyboardRowHeightDp()
    Column(
        modifier = modifier
            .fillMaxWidth()
            // Bounded to the keyboard's height: the wrap column must stay the
            // keyboard's size (an unbounded fillMaxSize stretches it to the
            // full window and pushes the action bar off the top of the band).
            .height((com.jiaoay.jrime.engine.ImeMetrics.CONTENT_ROWS * rowHeightDp).dp)
            .background(colors.liquidBackground),
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(state.tabs) { index, tag ->
                LiquidTabChip(
                    label = tag.label,
                    selected = index == state.currentTabIndex,
                    colors = colors,
                ) { state.setDataByIndex(index) }
            }
        }
        val symbols = state.currentKeys
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 44.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(symbols) { item ->
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .background(colors.key, RoundedCornerShape(6.dp))
                        .clickable { state.tap(item) },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(text = item.text, style = TextStyle(fontSize = 20.sp, color = colors.keyText))
                }
            }
        }
    }
}

/** One page tab of the liquid strip; the active one lights up. */
@Suppress("FunctionName")
@Composable
private fun LiquidTabChip(
    label: String,
    selected: Boolean,
    colors: ImePalette,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(
                if (selected) colors.accent else colors.functionKey,
                RoundedCornerShape(6.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        BasicText(
            text = label.trim(),
            style =
            TextStyle(
                fontSize = 12.sp,
                color = if (selected) colors.accentText else colors.keyText,
            ),
        )
    }
}
