/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.theme

import com.jiaoay.jrime.data.theme.Theme
import com.jiaoay.jrime.data.theme.model.GeneralStyle
import com.jiaoay.jrime.data.theme.model.MaybeStringList
import com.jiaoay.jrime.data.theme.model.Preedit
import com.jiaoay.jrime.data.theme.model.TextKeyboard
import com.jiaoay.jrime.data.theme.model.ToolBar
import com.jiaoay.jrime.data.theme.model.Window

/**
 * The one and only theme: hardcoded, no parsing, no switching. The [Theme]
 * model stays the consumer contract -- every piece of the IME UI keeps
 * reading the same data class it always did; this object is simply its new
 * producer. The liquid keyboard content comes from the standalone JSON
 * asset through [LiquidAssets]; everything else is code.
 *
 * Layouts and keys landed with task 1.2; the preedit/window/liquid/toolBar
 * surfaces land here with the source flip (task 1.4); everything is
 * calibrated against real Gboard screenshots during acceptance (task 1.5).
 */
object NativeTheme {
    /** Scheme ids: the light default and the dark scheme its chain links to. */
    const val DEFAULT_SCHEME = "default"
    const val DARK_SCHEME = "gboard_dark"

    val theme: Theme by lazy { build() }

    private fun build(): Theme = Theme(
        name = "native",
        style = gboardStyle(),
        preedit = preedit(),
        window = window(),
        liquidKeyboard = LiquidAssets.load(),
        presetKeys = presetKeys(),
        presetKeyboards = presetKeyboards(),
        presetColorSchemes =
        mapOf(
            DEFAULT_SCHEME to GboardColors.Light,
            DARK_SCHEME to GboardColors.Dark,
        ),
        fallbackColors = GboardColors.fallback,
        toolBar = toolBar(),
    )

    /**
     * Gboard visual parameters. Rounded keys with even gaps, no borders, no
     * shadows -- everything else stays at the model's neutral defaults until
     * the screenshot calibration pass pins sizes and offsets.
     */
    private fun gboardStyle(): GeneralStyle = GeneralStyle(
        roundCorner = 8f,
        horizontalGap = 4,
        verticalGap = 4,
        keyBorder = 0,
        shadowRadius = 0f,
    )

    /** The composing underline's geometry, ported from the legacy base theme. */
    private fun preedit(): Preedit = Preedit(
        horizontalPadding = 8,
        topStartRadius = 4f,
        topEndRadius = 4f,
        foreground = Preedit.Foreground(fontSize = 16f),
    )

    /** The floating candidate window's geometry, ported from the legacy base theme. */
    private fun window(): Window = Window(
        insets = Window.Padding(vertical = 4, horizontal = 4),
        itemPadding = Window.Padding(vertical = 0, horizontal = 4),
        foreground =
        Window.Foreground(
            labelFontSize = 20f,
            textFontSize = 20f,
            commentFontSize = 16f,
        ),
    )

    /**
     * The toolbar of the liquid pages, ported from the legacy closure: the
     * primary menu button plus the right-aligned button row (hide, number,
     * emoji, clipboard, editor, redo, undo).
     */
    private fun toolBar(): ToolBar = ToolBar(
        primaryButton =
        ToolBar.Button(
            background =
            ToolBar.Button.Background(
                cornerRadius = 8f,
                verticalInset = 4,
                horizontalInset = 0,
            ),
            foreground =
            ToolBar.Button.Foreground(
                style = "\uE141",
                fontSize = 18f,
                padding = 10,
            ),
            action = "menu_keyboard",
        ),
        buttons =
        listOf(
            toolBarButton(style = "ic@keyboard_close", action = "Hide"),
            toolBarButton(style = "\uE010", action = "Keyboard_number", fontSize = 20f, padding = 10),
            toolBarButton(style = "ic@emoticon", action = "liquid_keyboard_emoji"),
            toolBarButton(style = "ic@clipboard", action = "clipboard_window"),
            toolBarButton(style = "\uE1C1", action = "Keyboard_editor"),
            toolBarButton(style = "ic@redo", action = "redo"),
            toolBarButton(style = "ic@undo", action = "undo"),
        ),
        buttonSpacing = 5,
        buttonFont = MaybeStringList.Scalar("iconfont.ttf"),
    )

    private fun toolBarButton(
        style: String,
        action: String,
        fontSize: Float = 18f,
        padding: Int = 4,
    ): ToolBar.Button = ToolBar.Button(
        foreground =
        ToolBar.Button.Foreground(
            style = style,
            fontSize = fontSize,
            padding = padding,
        ),
        action = action,
    )

    /** Named key actions: the 71-key closure, ported with task 1.2. */
    private fun presetKeys(): Map<String, com.jiaoay.jrime.data.theme.model.PresetKey> = KeyLayouts.presetKeys

    /** The seven keyboards of the closure, ported with task 1.2. */
    private fun presetKeyboards(): Map<String, TextKeyboard> = KeyLayouts.keyboards
}
