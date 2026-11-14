/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.data.theme.model

data class ToolBar(
    val primaryButton: Button? = null,
    val buttons: List<Button> = emptyList(),
    val buttonSpacing: Int = 18,
    val buttonFont: MaybeStringList = MaybeStringList.Empty,
    val backStyle: String = "ic@arrow-left",
) {
    data class Button(
        val background: Background = Background(),
        val foreground: Foreground = Foreground(),
        val action: String = "",
        val longPressAction: String = "",
        val size: List<Int> = emptyList(),
    ) {
        data class Background(
            val type: Type = Type.RECTANGLE,
            val cornerRadius: Float = 10f,
            val normal: String = "",
            val highlight: String = "",
            val verticalInset: Int = 4,
            val horizontalInset: Int = 4,
        ) {
            enum class Type {
                RECTANGLE,
                CIRCLE,
            }
        }
        data class Foreground(
            val style: String = "",
            val optionStyles: List<String> = emptyList(),
            val normal: String = "",
            val highlight: String = "",
            val fontSize: Float = 18f,
            val padding: Int = 4,
        )
    }
}
