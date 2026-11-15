/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.theme

import com.jiaoay.jrime.data.theme.model.PresetKey
import com.jiaoay.jrime.data.theme.model.TextKeyboard

/**
 * The hardcoded keyboard layouts and key actions.
 *
 * Generated once from the flyhe.trime.yaml closure by
 * openspec/changes/native-theme/tools/gen_layouts.py (task 1.2), reviewed
 * against the 4.1 closure analysis, and frozen: this file is now the
 * single source of truth and is edited by hand from here on.
 */
object KeyLayouts {
    val keyboards: Map<String, TextKeyboard> = mapOf(
        "default" to keyboard("default") {
            author = "Boomker"
            width = 10f
            height = 35f
            verticalGap = 5
            asciiMode = 0
            resetAsciiMode = true
            keyTextOffsetY = -.6f
            keySymbolOffsetY = 0f
            keyHintOffsetY = 0f
            // Q 行长按=数字（Gboard 契约：角标只显示长按输出的字符）
            key(click = "q", longClick = "1", swipeDown = "~", hint = "1")
            key(click = "w", longClick = "2", swipeDown = "BackToPreviousSyllable", hint = "2")
            key(click = "e", longClick = "3", swipeDown = "Liquid_fixed_left", hint = "3")
            key(click = "r", longClick = "4", swipeDown = "Liquid_fixed_right", hint = "4")
            key(click = "t", longClick = "5", swipeDown = "Liquid_fixed_tab", hint = "5")
            key(click = "y", longClick = "6", hint = "6")
            key(click = "u", longClick = "7", swipeUp = "_", swipeDown = "U", hint = "7")
            key(click = "i", longClick = "8", swipeUp = "=", swipeDown = "I", hint = "8")
            key(click = "o", longClick = "9", swipeUp = "/", swipeDown = "/", hint = "9")
            key(click = "p", longClick = "0", swipeUp = "|", swipeDown = "\\", hint = "0")
            spacer(2f)
            // A/Z 行键面只留字母：低频的编辑/词库/标点功能收进长按与滑动槽作
            // 盲操作（操作栏已承担功能入口），不再用图标/角标扰乱视线
            key(click = "a", longClick = "select_all", width = 12f)
            key(click = "s", longClick = "Keyboard_symbol", swipeDown = "Liquid_fixed_down", width = 11f)
            key(click = "d", longClick = "Date_Lua", swipeUp = "Date_Lua", swipeDown = "DelCandFunc")
            key(click = "f", longClick = "Keyboard_editor", swipeUp = "Keyboard_editor", swipeDown = "Keyboard_emoji")
            key(click = "g", longClick = "Google", swipeUp = "Google", swipeDown = "ResetCandWeight")
            key(click = "h", longClick = "History_Lua", swipeUp = "History_Lua", swipeDown = "HideCandFunc")
            key(click = "j", longClick = ";", swipeDown = ":")
            key(click = "k", longClick = "{", swipeUp = "{", swipeDown = "[", width = 11f)
            key(click = "l", longClick = "}", swipeUp = "}", swipeDown = "]", width = 12f)
            spacer(2f)
            // The dedicated aux-code key (the master's call): while composing
            // it sends the separator the cand_selector contract parses; idle
            // in CJK it does nothing, and in ASCII it stays the plain shift.
            // The comma is back to being just a comma.
            key(composing = "/", ascii = "Shift_L", width = 14f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "z", longClick = "undo")
            key(click = "x", longClick = "cut")
            key(click = "c", longClick = "copy")
            key(click = "v", longClick = "paste")
            key(click = "b", longClick = "clipboard_window")
            key(click = "n", longClick = "'", swipeDown = "N")
            key(click = "m", longClick = "\"", swipeDown = "M")
            key(click = "BackSpace", swipeDown = "Delete", width = 16f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "Keyboard_basic_symbol", width = 15f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            // 逗号键组合态发 '/' 作辅助码分隔符：'/' 是方案层的刻意约定（speller
            // alphabet 含 '/' 不含 ','，逗号会被标点处理直接上屏）；长按/上滑切
            // 数字键盘的隐藏交互已移除（入口改到操作栏菜单行 "123"）。角标标出
            // 该键组合态的辅助码语义（主人点名要的功能提示）
            key(click = ",", label = "，", keyTextColor = "one_color", width = 15f)
            key(click = "space", swipeUp = "Up", swipeDown = "Keyboard_flyhe36", swipeLeft = "Left", swipeRight = "Right", width = 40f)
            // 句号键辅助码态（输入串尾部如 "efgh/x"）单击改为选定次选上屏；其余状态仍为句号
            key(click = ".", aux = "Control+2", longClick = "?", swipeUp = "?", swipeDown = "Commit_Dot", label = "。", hint = "?", keyTextColor = "one_color", width = 15f)
            key(click = "Return", width = 15f, keyTextSize = 24f, keyTextOffsetY = -2f, keyBackColor = "enter_bg_color", keyTextColor = "enter_text_color", hilitedKeyBackColor = "enter_pressed_bg_color", hilitedKeyTextColor = "enter_pressed_text_color")
        },
        "flyhe36" to keyboard("flyhe36") {
            author = "Boomker"
            width = 10f
            asciiMode = 0
            resetAsciiMode = true
            keyTextOffsetY = -.6f
            keySymbolOffsetY = 0f
            keyHintOffsetY = 0f
            key(click = "1", longClick = "!", swipeUp = "!", height = 35f, keySymbolOffsetX = 5f)
            key(click = "2", longClick = "@", swipeUp = "@", keySymbolOffsetX = 5f)
            key(click = "3", longClick = "#", swipeUp = "#", keySymbolOffsetX = 5f)
            key(click = "4", longClick = "$", swipeUp = "$", keySymbolOffsetX = 5f)
            key(click = "5", longClick = "%", swipeUp = "%", keySymbolOffsetX = 5f)
            key(click = "6", longClick = "Commit_Caret", swipeUp = "Commit_Caret", swipeDown = "……", keySymbolOffsetX = 5f)
            key(click = "7", longClick = "&", swipeUp = "&", swipeDown = "-", keySymbolOffsetX = 5f)
            key(click = "8", longClick = "*", swipeUp = "*", swipeDown = "+", keySymbolOffsetX = 5f)
            key(click = "9", longClick = "(", swipeUp = "(", swipeDown = "<", keySymbolOffsetX = 5f)
            key(click = "0", longClick = ")", swipeUp = ")", swipeDown = ">", keySymbolOffsetX = 5f)
            // Q 行长按=数字（Gboard 契约：角标只显示长按输出的字符）
            key(click = "q", longClick = "1", swipeDown = "~", hint = "1")
            key(click = "w", longClick = "2", swipeDown = "BackToPreviousSyllable", hint = "2")
            key(click = "e", longClick = "3", swipeDown = "Liquid_fixed_left", hint = "3")
            key(click = "r", longClick = "4", swipeDown = "Liquid_fixed_right", hint = "4")
            key(click = "t", longClick = "5", swipeDown = "Liquid_fixed_tab", hint = "5")
            key(click = "y", longClick = "6", hint = "6")
            key(click = "u", longClick = "7", swipeUp = "_", swipeDown = "U", hint = "7")
            key(click = "i", longClick = "8", swipeUp = "=", swipeDown = "I", hint = "8")
            key(click = "o", longClick = "9", swipeUp = "/", swipeDown = "/", hint = "9")
            key(click = "p", longClick = "0", swipeUp = "|", swipeDown = "\\", hint = "0")
            spacer(2f)
            // A/Z 行键面只留字母：低频的编辑/词库/标点功能收进长按与滑动槽作
            // 盲操作（操作栏已承担功能入口），不再用图标/角标扰乱视线
            key(click = "a", longClick = "select_all", width = 12f)
            key(click = "s", longClick = "Keyboard_symbol", swipeDown = "Liquid_fixed_down", width = 11f)
            key(click = "d", longClick = "Date_Lua", swipeUp = "Date_Lua", swipeDown = "DelCandFunc")
            key(click = "f", longClick = "Keyboard_editor", swipeUp = "Keyboard_editor", swipeDown = "Keyboard_emoji")
            key(click = "g", longClick = "Google", swipeUp = "Google", swipeDown = "ResetCandWeight")
            key(click = "h", longClick = "History_Lua", swipeUp = "History_Lua", swipeDown = "HideCandFunc")
            key(click = "j", longClick = ";", swipeDown = ":")
            key(click = "k", longClick = "{", swipeUp = "{", swipeDown = "[", width = 11f)
            key(click = "l", longClick = "}", swipeUp = "}", swipeDown = "]", width = 12f)
            spacer(2f)
            // The dedicated aux-code key (the master's call): while composing
            // it sends the separator the cand_selector contract parses; idle
            // in CJK it does nothing, and in ASCII it stays the plain shift.
            // The comma is back to being just a comma.
            key(composing = "/", ascii = "Shift_L", width = 14f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "z", longClick = "undo")
            key(click = "x", longClick = "cut")
            key(click = "c", longClick = "copy")
            key(click = "v", longClick = "paste")
            key(click = "b", longClick = "clipboard_window")
            key(click = "n", longClick = "'", swipeDown = "N")
            key(click = "m", longClick = "\"", swipeDown = "M")
            key(click = "BackSpace", swipeDown = "Delete", width = 16f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "Keyboard_basic_symbol", width = 15f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = ",", label = "，", keyTextColor = "one_color", width = 15f)
            key(click = "space", swipeUp = "Up", swipeDown = "Keyboard_default", swipeLeft = "Left", swipeRight = "Right", width = 40f)
            key(click = ".", aux = "Control+2", longClick = "?", swipeUp = "?", swipeDown = "Commit_Dot", label = "。", hint = "?", keyTextColor = "one_color", width = 15f)
            key(click = "Return", width = 15f, keyTextSize = 24f, keyTextOffsetY = -2f, keyBackColor = "enter_bg_color", keyTextColor = "enter_text_color", hilitedKeyBackColor = "enter_pressed_bg_color", hilitedKeyTextColor = "enter_pressed_text_color")
        },
        "number" to keyboard("number") {
            author = "Boomker"
            width = 11.5f
            height = 47f
            asciiMode = 0
            resetAsciiMode = true
            key(click = "!", longClick = "=", swipeUp = "=")
            key(click = "^", longClick = "Commit_Dollar", swipeUp = "Commit_Dollar")
            key(click = "(", longClick = "<", swipeUp = "<", label = "(")
            key(click = "1", width = 18f, keyTextSize = 24f)
            key(click = "2", width = 18f, keyTextSize = 24f)
            key(click = "3", width = 18f, keyTextSize = 24f)
            key(click = "BackSpace", swipeDown = "Delete", keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "+")
            key(click = "-")
            key(click = "Left", keyTextSize = 24f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "4", width = 18f, keyTextSize = 24f)
            key(click = "5", width = 18f, keyTextSize = 24f, keyTextColor = "on_key_text_color")
            key(click = "6", width = 18f, keyTextSize = 24f)
            key(click = "Right", keyTextSize = 24f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "*", longClick = "×", swipeUp = "×")
            key(click = "/", longClick = "÷", swipeUp = "÷")
            key(click = ")", longClick = ">", swipeUp = ">")
            key(click = "7", width = 18f, keyTextSize = 24f)
            key(click = "8", width = 18f, keyTextSize = 24f)
            key(click = "9", width = 18f, keyTextSize = 24f)
            key(click = "Alt_space")
            key(click = "Keyboard_default", longClick = "Number_Lua", swipeDown = "Lunar_Lua", hint = "农历", width = 17.25f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "Keyboard_math_symbol", longClick = "Calc_Lua", swipeDown = "Date_Lua", hint = "日期", width = 17.25f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "0", swipeDown = "Escape", width = 36f, keyTextSize = 24f, keySymbolColor = "on_key_text_color")
            key(click = ".", width = 18f, keyTextSize = 24f)
            key(click = "Return", keyTextSize = 24f, keyTextOffsetY = -2f, keyBackColor = "enter_bg_color", keyTextColor = "enter_text_color", hilitedKeyBackColor = "enter_pressed_bg_color", hilitedKeyTextColor = "enter_pressed_text_color")
        },
        "editor" to keyboard("editor") {
            author = "Boomker"
            width = 20f
            height = 47f
            asciiMode = 0
            keyTextOffsetY = -.4f
            keySymbolOffsetY = 1.6f
            key(click = "cut", longClick = "cut_all", labelSymbol = "✁", keyTextSize = 20f)
            key(click = "Home", longClick = "Page_Up", keyTextSize = 20f)
            key(click = "Up", keyTextSize = 24f)
            key(click = "Delete", keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "BackSpace", keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "copy", longClick = "copy_all", labelSymbol = "❐", keyTextSize = 20f)
            key(click = "Left", keyTextSize = 24f)
            key(click = "Shift_L", label = "选择", keyTextSize = 20f)
            key(click = "Right", keyTextSize = 24f)
            key(click = "clipboard_window", labelSymbol = "⏍", keyTextSize = 20f)
            key(click = "paste", labelSymbol = "▣", keyTextSize = 20f)
            key(click = "End", longClick = "Page_Down", keyTextSize = 20f)
            key(click = "Down", keyTextSize = 24f)
            key(click = "Alt_space", width = 40f, keyTextSize = 24f)
            key(click = "Keyboard_default", keyTextSize = 20f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "Clear", label = "❌", hint = "清空", keySymbolColor = "on_key_text_color")
            key(click = "undo", labelSymbol = "↶", keyTextSize = 20f)
            key(click = "redo", labelSymbol = "↷", keyTextSize = 20f)
            key(click = "Return", keyTextSize = 24f, keyTextOffsetY = -2f, keyBackColor = "enter_bg_color", keyTextColor = "enter_text_color", hilitedKeyBackColor = "enter_pressed_bg_color", hilitedKeyTextColor = "enter_pressed_text_color")
        },
        "basic_symbol" to keyboard("basic_symbol") {
            author = "Windy_D"
            width = 10f
            height = 35f
            verticalGap = 5
            asciiMode = 1
            resetAsciiMode = true
            keySymbolOffsetY = 0f
            keyHintOffsetY = 0f
            key(click = "~")
            key(click = "↑", longClick = "⇑", swipeLeft = "↖", swipeRight = "↗", hint = "↖   ↗")
            key(click = "№", swipeLeft = "₹", swipeRight = "₱", hint = "₹   ₱")
            key(click = "♪")
            key(click = "¶", longClick = "§")
            key(click = "°", swipeLeft = "′", swipeRight = "″", hint = "′   ″")
            key(click = "∅", longClick = "ⁿ")
            key(click = "±", longClick = "∓")
            key(click = "≠", longClick = "∞", swipeLeft = "≡", swipeRight = "≈", hint = "≡   ≈")
            key(click = "√", longClick = "∛", swipeDown = "∜", hint = "∜", keyTextSize = 16f)
            key(click = "←", longClick = "⇐")
            key(click = "↓", longClick = "⇓", swipeLeft = "↙", swipeRight = "↘", hint = "↙   ↘")
            key(click = "→", longClick = "⇒")
            key(click = "•", swipeLeft = "„", swipeRight = "‚", hint = "„   ‚")
            key(click = "★", swipeLeft = "‡", swipeRight = "†", hint = "‡   †", keyTextSize = 16f)
            key(click = "–", longClick = "—")
            key(click = "−")
            key(click = "Ω", longClick = "℧")
            key(click = "μ")
            key(click = "π", longClick = "Π")
            key(click = "Keyboard_math_symbol", width = 14f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "™", longClick = "℅", swipeLeft = "©", swipeRight = "®", hint = "©   ®")
            key(click = "♠︎", longClick = "♥︎", swipeLeft = "♦︎", swipeRight = "♣︎", hint = "♦︎   ♣︎", keyTextSize = 16f)
            key(click = "⩽", longClick = "≤")
            key(click = "⩾", longClick = "≥")
            key(click = "✓")
            key(click = "‰", longClick = "‱")
            key(click = "¡")
            key(click = "BackSpace", swipeDown = "Delete", width = 16f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "Keyboard_default", longClick = "Settings", width = 14f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "Keyboard_number", longClick = "Flyhe_readme", width = 12.5f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "‹", longClick = "«")
            key(click = "Alt_space", swipeUp = "Up", swipeDown = "Down", swipeLeft = "Left", swipeRight = "Right", width = 27f)
            key(click = "›", longClick = "»")
            key(click = ".", longClick = "…")
            key(click = "Return", width = 15f, keyTextSize = 24f, keyTextOffsetY = -2f, keyBackColor = "enter_bg_color", keyTextColor = "enter_text_color", hilitedKeyBackColor = "enter_pressed_bg_color", hilitedKeyTextColor = "enter_pressed_text_color")
        },
        "math_symbol" to keyboard("math_symbol") {
            author = "Windy_D"
            width = 10f
            height = 35f
            verticalGap = 5
            asciiMode = 1
            resetAsciiMode = true
            keySymbolOffsetY = 0f
            keyHintOffsetY = 0f
            key(click = "∅", longClick = "∁")
            key(click = "∃", longClick = "∄", swipeDown = "∀", hint = "∀")
            key(click = "∈", longClick = "∉", swipeLeft = "∋", swipeRight = "∌", hint = "∋   ∌", keyTextSize = 16f)
            key(click = "Σ", swipeLeft = "Π", swipeRight = "∐", hint = "Π   ∐", keyTextSize = 22f)
            key(click = "∫", longClick = "∬")
            key(click = "∮", longClick = "∯")
            key(click = "Δ", longClick = "∂")
            key(click = "ℝ", longClick = "ℚ")
            key(click = "ℕ", longClick = "ℤ")
            key(click = "ℂ", longClick = "ℍ")
            key(click = "∧", longClick = "∨")
            key(click = "∩", longClick = "∪", swipeLeft = "⋂", swipeRight = "⋃", hint = "⋂   ⋃")
            key(click = "⊂", longClick = "⊆", swipeDown = "⊈", hint = "⊈", keyTextSize = 16f)
            key(click = "⊊", longClick = "⫋", keyTextSize = 16f)
            key(click = "∣", longClick = "∤", swipeLeft = "∥", swipeRight = "⟂", hint = "∥   ⟂")
            key(click = "∗")
            key(click = "⊕", longClick = "⊖", swipeDown = "⊙", hint = "⊙")
            key(click = "∠", longClick = "∢")
            key(click = "∎")
            key(click = "↊", longClick = "↋")
            key(click = "Keyboard_t9_symbol", width = 14f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "∵", longClick = "∴")
            key(click = "⎜", longClick = "⎛", swipeUp = "⎛", swipeDown = "⎝", hint = "⎝", keyTextSize = 16f)
            key(click = "⎟", longClick = "⎞", swipeUp = "⎞", swipeDown = "⎠", hint = "⎠", keyTextSize = 16f)
            key(click = "⋯", longClick = "⋮", swipeLeft = "⋰", swipeRight = "⋱", hint = "⋰   ⋱")
            key(click = "≝", longClick = "≟", swipeDown = "∝", hint = "∝")
            key(click = "∽", longClick = "∼", swipeDown = "≌", hint = "≌")
            key(click = "⇔", longClick = "⇌", hint = "   ")
            key(click = "BackSpace", swipeDown = "Delete", width = 16f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "Keyboard_default", longClick = "Settings", width = 14f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "Keyboard_number", longClick = "Flyhe_readme", width = 12.5f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "≪", keyTextSize = 16f)
            key(click = "Alt_space", swipeUp = "Up", swipeDown = "Down", swipeLeft = "Left", swipeRight = "Right", width = 27f)
            key(click = "≫", keyTextSize = 16f)
            key(click = ".", longClick = "…")
            key(click = "Return", width = 15f, keyTextSize = 24f, keyTextOffsetY = -2f, keyBackColor = "enter_bg_color", keyTextColor = "enter_text_color", hilitedKeyBackColor = "enter_pressed_bg_color", hilitedKeyTextColor = "enter_pressed_text_color")
        },
        "t9_symbol" to keyboard("t9_symbol") {
            author = "Boomker"
            width = 10f
            height = 35f
            verticalGap = 5
            asciiMode = 1
            resetAsciiMode = true
            keySymbolOffsetY = 0f
            keyHintOffsetY = 0f
            key(click = "1", longClick = "!", swipeUp = "!")
            key(click = "2", longClick = "@", swipeUp = "@", swipeLeft = "©", swipeRight = "®", hint = "©   ®")
            key(click = "3", longClick = "#", swipeUp = "#", swipeLeft = "℃", swipeRight = "℉", hint = "℃   ℉")
            key(click = "4", longClick = "$", swipeUp = "$", swipeLeft = "¢", swipeRight = "€", hint = "¢   €")
            key(click = "5", longClick = "%", swipeUp = "%", swipeLeft = "£", swipeRight = "₿", hint = "£   ₿")
            key(click = "6", longClick = "Commit_Caret", swipeUp = "Commit_Caret", swipeDown = "……")
            key(click = "7", longClick = "&", swipeUp = "&", swipeDown = "_", hint = "_")
            key(click = "8", longClick = "*", swipeUp = "*", hint = "-")
            key(click = "9", longClick = "(", swipeUp = "(", swipeDown = "+", hint = "+")
            key(click = "0", longClick = ")", swipeUp = ")", swipeDown = "/", hint = "/")
            key(click = "`")
            key(click = "~", longClick = "≈")
            key(click = "=", longClick = "≠")
            key(click = "[", longClick = "「", swipeUp = "「", swipeDown = "【", hint = "【")
            key(click = "]", longClick = "」", swipeUp = "」", swipeDown = "】", hint = "】")
            key(click = "{", longClick = "『", swipeUp = "『", swipeDown = "〖", hint = "〖")
            key(click = "}", longClick = "』", swipeUp = "』", swipeDown = "〗", hint = "〗")
            key(click = "\\")
            key(click = "/")
            key(click = "|")
            key(click = "Keyboard_basic_symbol", width = 14f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = ";")
            key(click = ":")
            key(click = "'")
            key(click = "\"")
            key(click = "<", longClick = "《", swipeUp = "《", swipeDown = "〈", hint = "〈")
            key(click = ">", longClick = "》", swipeUp = "》", swipeDown = "〉", hint = "〉")
            key(click = "?")
            key(click = "BackSpace", swipeDown = "Delete", width = 16f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "Keyboard_default", longClick = "Settings", width = 16f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "Keyboard_number", longClick = "Flyhe_readme", width = 14f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "Left", width = 12f, keyTextSize = 24f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "Alt_space", swipeUp = "Up", swipeDown = "Down", swipeLeft = "Left", swipeRight = "Right", width = 30f)
            key(click = "Right", width = 12f, keyTextSize = 24f, keyBackColor = "off_key_back_color", keyTextColor = "off_key_text_color", hilitedKeyBackColor = "hilited_off_key_back_color", hilitedKeyTextColor = "hilited_off_key_text_color")
            key(click = "Return", width = 15f, keyTextSize = 24f, keyTextOffsetY = -2f, keyBackColor = "enter_bg_color", keyTextColor = "enter_text_color", hilitedKeyBackColor = "enter_pressed_bg_color", hilitedKeyTextColor = "enter_pressed_text_color")
        },
    ).mapValues { (_, builder) ->
        builder.build(TextKeyboard.DEFAULTS)
    }

    val presetKeys: Map<String, PresetKey> = mapOf(
        "Alt_space" to PresetKey(label = "空格", repeatable = true, send = "space"),
        "BackSpace" to PresetKey(label = "⇦", repeatable = true, send = "BackSpace"),
        "BackToPreviousSyllable" to PresetKey(label = "刪音节", send = "Control+BackSpace"),
        "Calc_Lua" to PresetKey(label = "计算器", text = "cC"),
        "Clear" to PresetKey(label = "清除", text = "{Control+a}{BackSpace}"),
        "Commit_Caret" to PresetKey(label = "^", commit = "^"),
        "Commit_Dollar" to PresetKey(label = "$", commit = "$"),
        "Commit_Dot" to PresetKey(label = ".", commit = "."),
        "Date_Lua" to PresetKey(label = "日期", text = "orq"),
        "DelCandFunc" to PresetKey(label = "删词", send = "Control+d"),
        "Delete" to PresetKey(label = "⇨", repeatable = true, send = "Delete"),
        "Down" to PresetKey(label = "↓", repeatable = true, send = "Down"),
        "EasyEnMode" to PresetKey(label = "英译", send = "Control+g"),
        "End" to PresetKey(label = "行尾", send = "End"),
        "Escape" to PresetKey(label = "Esc", send = "Escape"),
        "Flyhe_readme" to PresetKey(command = "run", option = "https://github.com/boomker/rime-fast-xhup", label = "关于"),
        "Google" to PresetKey(command = "run", option = "https://www.google.com/search?q=%s", label = "谷歌"),
        "Hide" to PresetKey(label = "隐藏", send = "BACK"),
        "HideCandFunc" to PresetKey(label = "藏词", send = "Control+x"),
        "History_Lua" to PresetKey(label = "历史", text = "hH"),
        "Home" to PresetKey(label = "行首", send = "Home"),
        "IME_switch" to PresetKey(label = "⌨", send = "LANGUAGE_SWITCH"),
        "Keyboard_basic_symbol" to PresetKey(select = "basic_symbol", label = "符号", send = "Eisu_toggle"),
        "Keyboard_default" to PresetKey(select = ".default", label = "返回", send = "Eisu_toggle"),
        "Keyboard_editor" to PresetKey(select = "editor", label = "编辑", send = "Eisu_toggle"),
        "Keyboard_emoji" to PresetKey(command = "liquid_keyboard", option = "😀", label = "☻", send = "FUNCTION"),
        "Keyboard_flyhe36" to PresetKey(select = "flyhe36", label = "36键中", send = "Eisu_toggle"),
        "Keyboard_math_symbol" to PresetKey(select = "math_symbol", label = "符号", send = "Eisu_toggle"),
        "Keyboard_number" to PresetKey(select = "number", label = "123", send = "Eisu_toggle"),
        "Keyboard_symbol" to PresetKey(command = "liquid_keyboard", option = "标点", label = "符号", send = "FUNCTION"),
        "Keyboard_t9_symbol" to PresetKey(select = "t9_symbol", label = "符号", send = "Eisu_toggle"),
        "Left" to PresetKey(label = "←", repeatable = true, send = "Left"),
        "Liquid_fixed_backspace" to PresetKey(label = " ⇦  ", repeatable = true, send = "BackSpace"),
        "Liquid_fixed_down" to PresetKey(label = "↓", send = "Down"),
        "Liquid_fixed_exit" to PresetKey(command = "liquid_keyboard", option = "-1", label = " ↶ ", send = "FUNCTION"),
        "Liquid_fixed_left" to PresetKey(label = " ←  ", send = "Left"),
        "Liquid_fixed_return" to PresetKey(label = " ↲  ", repeatable = true, send = "Return"),
        "Liquid_fixed_right" to PresetKey(label = " →  ", send = "Right"),
        "Liquid_fixed_space" to PresetKey(label = " ⎵  ", repeatable = true, send = "space"),
        "Liquid_fixed_tab" to PresetKey(label = " ⇥ ", send = "Tab"),
        "Liquid_fixed_up" to PresetKey(label = "↑", send = "Up"),
        "Lunar_Lua" to PresetKey(label = "农历", text = "nL"),
        "Mandarin_rl" to PresetKey(label = "反查", text = "rL"),
        "Menu" to PresetKey(label = "🌐︎", send = "MENU"),
        "Number_Lua" to PresetKey(label = "中文数", text = "nN"),
        "Page_Down" to PresetKey(label = "下一页", send = "Page_Down"),
        "Page_Up" to PresetKey(label = "上一页", send = "Page_Up"),
        "PinCandFunc" to PresetKey(label = "置顶", send = "Control+t"),
        "ResetCandWeight" to PresetKey(label = "忘词", send = "Shift+Delete"),
        "Return" to PresetKey(label = "enter_labels", repeatable = true, send = "Return"),
        "Right" to PresetKey(label = "→", repeatable = true, send = "Right"),
        "Schema_switch" to PresetKey(label = "切换", send = "Control+Shift+0"),
        "Settings" to PresetKey(label = "设定", send = "SETTINGS"),
        "Shift_L" to PresetKey(label = "⇪", send = "Shift_L"),
        "Shift_L2" to PresetKey(label = "L", shiftLock = "click", send = "Shift_L"),
        "Up" to PresetKey(label = "↑", repeatable = true, send = "Up"),
        "ascii_switch" to PresetKey(toggle = "ascii_mode", states = listOf("中", "EN"), send = "SWITCH_CHARSET"),
        "back_to_keyboard" to PresetKey(command = "back_to_keyboard", label = "⌨", send = "FUNCTION"),
        "clipboard_window" to PresetKey(command = "clipboard_window", label = "剪贴", send = "FUNCTION"),
        "copy" to PresetKey(label = "复制", send = "Control+c"),
        "copy_all" to PresetKey(label = "全拷", text = "{Control+a}{Control+c}"),
        "cut" to PresetKey(label = "剪切", send = "Control+x"),
        "cut_all" to PresetKey(label = "全挪", text = "{Control+a}{Control+x}"),
        "liquid_keyboard_emoji" to PresetKey(command = "liquid_keyboard", option = "😀", label = "☻", send = "FUNCTION"),
        "menu_keyboard" to PresetKey(command = "menu_keyboard", label = "菜单", send = "FUNCTION"),
        "paste" to PresetKey(label = "粘贴", send = "Control+v"),
        "punct_switch" to PresetKey(toggle = "ascii_punct", states = listOf("，。", ". ,"), send = "SWITCH_CHARSET"),
        "redo" to PresetKey(label = "重做", send = "Control+y"),
        "select_all" to PresetKey(label = "全选", send = "Control+a"),
        "space" to PresetKey(preview = "{schema}", repeatable = true, send = "space"),
        "undo" to PresetKey(label = "撤销", send = "Control+z"),
    )
}
