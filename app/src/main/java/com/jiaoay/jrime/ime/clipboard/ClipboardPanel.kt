/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.clipboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.jiaoay.jrime.ime.compose.GboardKeyboard
import com.jiaoay.jrime.ime.compose.ImeColors
import com.jiaoay.jrime.ime.compose.LocalImeColors
import com.jiaoay.jrime.ime.compose.panels.ClipboardPanelState
import com.jiaoay.jrime.ime.compose.rememberKeyboardRowHeightDp

/**
 * The Compose clipboard panel: a tab strip (history / favorites) above a
 * scrolling card list. A tap pastes; a long-press opens the item menu
 * (pin, favorite, edit, share, delete). The whole panel lives inside the
 * keyboard band's bound (4 rows high), the liquid-panel pattern.
 */
@OptIn(ExperimentalFoundationApi::class)
@Suppress("FunctionName")
@Composable
fun ClipboardPanel(
    state: ClipboardPanelState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalImeColors.current
    // The clipboard band renders the same content height as the keyboard
    // (KEYBOARD_CONTENT_ROWS x the row-height preference) so switching
    // surfaces never jumps the band.
    val rowHeightDp = rememberKeyboardRowHeightDp()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height((GboardKeyboard.KEYBOARD_CONTENT_ROWS * rowHeightDp).dp)
            .background(colors.keyboardBackground),
    ) {
        ClipboardTabStrip(state, colors)
        val items = if (state.currentTab == 0) state.clippings else state.favorites
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = if (state.currentTab == 0) "剪贴板为空" else "收藏为空",
                    style = TextStyle(fontSize = 15.sp, color = colors.keySymbol),
                )
            }
        } else {
            ClipboardList(state, colors, items)
        }
    }
}

/** The two tabs and the clear button, on one strip. */
@Suppress("FunctionName")
@Composable
private fun ClipboardTabStrip(
    state: ClipboardPanelState,
    colors: ImeColors,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabChip("剪贴板", state.currentTab == 0, colors) { state.selectTab(0) }
        TabChip("收藏", state.currentTab == 1, colors) { state.selectTab(1) }
        Box(modifier = Modifier.weight(1f))
        MenuChip("清空", colors) { state.clearCurrentTab() }
    }
}

@Suppress("FunctionName")
@Composable
private fun TabChip(
    label: String,
    selected: Boolean,
    colors: ImeColors,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(
                if (selected) colors.activeToggle else colors.functionKey,
                RoundedCornerShape(6.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        BasicText(
            text = label,
            style =
            TextStyle(
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = if (selected) colors.activeToggleLabel else colors.keyText,
            ),
        )
    }
}

@Suppress("FunctionName")
@Composable
private fun MenuChip(
    label: String,
    colors: ImeColors,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(colors.functionKey, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        BasicText(text = label, style = TextStyle(fontSize = 13.sp, color = colors.keySymbol))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Suppress("FunctionName")
@Composable
private fun ClipboardList(
    state: ClipboardPanelState,
    colors: ImeColors,
    items: List<ClipboardItem>,
) {
    var menuFor by remember { mutableStateOf<ClipboardItem?>(null) }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(items) { _, item ->
            ClipCard(
                item = item,
                colors = colors,
                onClick = { state.paste(item) },
                onLongPress = { menuFor = item },
            )
        }
    }
    menuFor?.let { item ->
        ClipItemMenu(
            item = item,
            state = state,
            colors = colors,
            onDismiss = { menuFor = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Suppress("FunctionName")
@Composable
private fun ClipCard(
    item: ClipboardItem,
    colors: ImeColors,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .background(colors.keyBackground, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column {
            BasicText(
                text = item.text,
                style = TextStyle(fontSize = 15.sp, color = colors.keyText),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            if (item.pinned) {
                BasicText(
                    text = "已置顶",
                    style = TextStyle(fontSize = 10.sp, color = colors.keySymbol),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/** The long-press action sheet, a plain popup over the panel. */
@Suppress("FunctionName")
@Composable
private fun ClipItemMenu(
    item: ClipboardItem,
    state: ClipboardPanelState,
    colors: ImeColors,
    onDismiss: () -> Unit,
) {
    val entries =
        buildList {
            if (state.currentTab == 0) {
                if (item.pinned) {
                    add("取消置顶" to { state.unpin(item) })
                } else {
                    add("置顶" to { state.pin(item) })
                }
                add("收藏" to { state.favorite(item) })
            }
            if (state.currentTab == 0) {
                add("编辑" to { state.editClipping(item) })
            } else {
                add("编辑" to { state.editFavorite(item) })
            }
            add("分享" to { state.share(item) })
            if (state.currentTab == 0) {
                add("删除" to { state.deleteClipping(item) })
            } else {
                add("删除" to { state.deleteFavorite(item) })
            }
        }
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .background(colors.keyBackground, RoundedCornerShape(10.dp))
                .padding(vertical = 4.dp),
        ) {
            entries.forEachIndexed { index, (label, action) ->
                Box(
                    modifier = Modifier
                        .clickable {
                            action()
                            onDismiss()
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    BasicText(
                        text = label,
                        style =
                        TextStyle(
                            fontSize = 15.sp,
                            color = if (label == "删除") Color(0xFFD93025) else colors.keyText,
                        ),
                    )
                }
            }
        }
    }
}
