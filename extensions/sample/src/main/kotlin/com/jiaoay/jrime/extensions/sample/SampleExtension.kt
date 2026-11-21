/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.extensions.sample

import com.jiaoay.jrime.action.ImeAction
import com.jiaoay.jrime.engine.KeyboardId
import com.jiaoay.jrime.engine.PanelId
import com.jiaoay.jrime.extension.ExtensionToolbarAction
import com.jiaoay.jrime.extension.InputRuleDescriptor
import com.jiaoay.jrime.extension.JrimeExtension
import com.jiaoay.jrime.extension.KeySlot
import com.jiaoay.jrime.extension.KeyboardExtension
import com.jiaoay.jrime.engine.PanelId as Panel

/**
 * A complete, copyable extension: typed actions, a keyboard board, a toolbar
 * entry and an input-rule descriptor. Everything is Kotlin — no YAML, no
 * token strings, no theme files. See `doc/extension-guide.md`.
 */
object SampleExtension : JrimeExtension {
    const val SAMPLE_KEYBOARD = "sample.symbols"

    override val id: String = "sample"

    override fun actions(): Map<String, ImeAction> = mapOf(
        "SampleInsertSignature" to ImeAction.Commit("—— JRime · 专注好用的输入法"),
        "SampleOpenSymbols" to ImeAction.OpenPanel(Panel.Symbols),
    )

    override fun keyboards(): List<KeyboardExtension> = listOf(
        KeyboardExtension(
            id = KeyboardId(SAMPLE_KEYBOARD),
            columns = 10,
            keys =
            listOf(
                symbol("★"),
                symbol("☆"),
                symbol("✓"),
                symbol("♥"),
                symbol("→"),
                symbol("←"),
                symbol("↑"),
                symbol("↓"),
                symbol("·"),
                symbol("——"),
                KeySlot(
                    label = "签名",
                    width = 4f,
                    click = ImeAction.Commit("—— JRime · 专注好用的输入法"),
                ),
                KeySlot(
                    label = "返回键盘",
                    width = 4f,
                    click = ImeAction.SwitchKeyboard(KeyboardId.Qwerty26),
                ),
                KeySlot(
                    label = "符号面板",
                    width = 2f,
                    click = ImeAction.OpenPanel(PanelId.Symbols),
                ),
            ),
        ),
    )

    override fun toolbarActions(): List<ExtensionToolbarAction> = listOf(
        ExtensionToolbarAction(
            id = "sample.board",
            label = "扩展示例",
            icon = "Add",
            action = ImeAction.SwitchKeyboard(KeyboardId(SAMPLE_KEYBOARD)),
        ),
    )

    override fun inputRules(): List<InputRuleDescriptor> = listOf(
        InputRuleDescriptor(
            option = "sample_wide_punct",
            title = "示例规则（宿主可渲染）",
            labels = listOf("关闭", "开启"),
        ),
    )

    private fun symbol(text: String): KeySlot = KeySlot(label = text, width = 1f, click = ImeAction.Commit(text))
}
