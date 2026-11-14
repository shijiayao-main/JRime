/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.theme

import com.jiaoay.jrime.data.theme.ColorScheme

/**
 * Gboard-style color schemes, hardcoded: the color keys are the canonical
 * `ColorKey` spellings (lowercased enum names). Values start from Google's
 * Material surface palette and are calibrated against real Gboard
 * screenshots during acceptance (task 1.5); treat every constant here as a
 * tuning knob, not a finished decision.
 *
 * The light scheme carries the `dark_scheme` link the resolver follows when
 * the system switches to night mode, so the pair travels as one.
 */
object GboardColors {
    val Light: ColorScheme = mapOf(
        // candidate bar
        "back_color" to "#FFFFFF",
        "candidate_background" to "#FFFFFF",
        "candidate_text_color" to "#202124",
        "comment_text_color" to "#5F6368",
        "hilited_candidate_back_color" to "#E8F0FE",
        "hilited_candidate_text_color" to "#1A73E8",
        "hilited_comment_text_color" to "#1A73E8",
        "candidate_separator_color" to "#DADCE0",
        // composition / preedit text
        "text_color" to "#202124",
        "hilited_text_color" to "#1A73E8",
        "hilited_back_color" to "#E8F0FE",
        // keyboard
        "keyboard_back_color" to "#F1F3F4",
        "key_back_color" to "#FFFFFF",
        "key_text_color" to "#202124",
        "key_symbol_color" to "#5F6368",
        "hilited_key_back_color" to "#DADCE0",
        "hilited_key_text_color" to "#202124",
        "hilited_key_symbol_color" to "#5F6368",
        // function keys (off) and active toggles (on)
        "off_key_back_color" to "#E8EAED",
        "off_key_text_color" to "#3C4043",
        "off_key_symbol_color" to "#5F6368",
        "hilited_off_key_back_color" to "#DADCE0",
        "hilited_off_key_text_color" to "#3C4043",
        "hilited_off_key_symbol_color" to "#5F6368",
        "on_key_back_color" to "#1A73E8",
        "on_key_text_color" to "#FFFFFF",
        "on_key_symbol_color" to "#FFFFFF",
        "hilited_on_key_back_color" to "#3B78E7",
        "hilited_on_key_text_color" to "#FFFFFF",
        "hilited_on_key_symbol_color" to "#FFFFFF",
        // misc surfaces
        "label_color" to "#5F6368",
        "hilited_label_color" to "#1A73E8",
        "long_text_color" to "#202124",
        "long_text_back_color" to "#FFFFFF",
        "liquid_keyboard_background" to "#F1F3F4",
        "hilited_popup_back_color" to "#E8EAED",
        "hilited_popup_text_color" to "#202124",
        "border_color" to "#00000000",
        "candidate_border_color" to "#00000000",
        "key_border_color" to "#00000000",
        "off_key_border_color" to "#00000000",
        "on_key_border_color" to "#00000000",
        "popup_back_color" to "#FFFFFF",
        "popup_text_color" to "#202124",
        // theme-defined key color names the layouts reference: the Gboard
        // enter key is an accent-filled pill.
        "enter_bg_color" to "#1A73E8",
        "enter_text_color" to "#FFFFFF",
        "enter_pressed_bg_color" to "#3B78E7",
        "enter_pressed_text_color" to "#FFFFFF",
        "one_color" to "#1A73E8",
        // the resolver's day -> night chain
        "dark_scheme" to "gboard_dark",
    )

    val Dark: ColorScheme = mapOf(
        // candidate bar
        "back_color" to "#202124",
        "candidate_background" to "#202124",
        "candidate_text_color" to "#E8EAED",
        "comment_text_color" to "#9AA0A6",
        "hilited_candidate_back_color" to "#303134",
        "hilited_candidate_text_color" to "#8AB4F8",
        "hilited_comment_text_color" to "#8AB4F8",
        "candidate_separator_color" to "#3C4043",
        // composition / preedit text
        "text_color" to "#E8EAED",
        "hilited_text_color" to "#8AB4F8",
        "hilited_back_color" to "#303134",
        // keyboard
        "keyboard_back_color" to "#202124",
        "key_back_color" to "#3C4043",
        "key_text_color" to "#E8EAED",
        "key_symbol_color" to "#9AA0A6",
        "hilited_key_back_color" to "#5F6368",
        "hilited_key_text_color" to "#E8EAED",
        "hilited_key_symbol_color" to "#9AA0A6",
        // function keys (off) and active toggles (on)
        "off_key_back_color" to "#303134",
        "off_key_text_color" to "#E8EAED",
        "off_key_symbol_color" to "#9AA0A6",
        "hilited_off_key_back_color" to "#5F6368",
        "hilited_off_key_text_color" to "#E8EAED",
        "hilited_off_key_symbol_color" to "#9AA0A6",
        "on_key_back_color" to "#8AB4F8",
        "on_key_text_color" to "#202124",
        "on_key_symbol_color" to "#202124",
        "hilited_on_key_back_color" to "#AECBFA",
        "hilited_on_key_text_color" to "#202124",
        "hilited_on_key_symbol_color" to "#202124",
        // misc surfaces
        "label_color" to "#9AA0A6",
        "hilited_label_color" to "#8AB4F8",
        "long_text_color" to "#E8EAED",
        "long_text_back_color" to "#3C4043",
        "liquid_keyboard_background" to "#202124",
        "hilited_popup_back_color" to "#5F6368",
        "hilited_popup_text_color" to "#E8EAED",
        "border_color" to "#00000000",
        "candidate_border_color" to "#00000000",
        "key_border_color" to "#00000000",
        "off_key_border_color" to "#00000000",
        "on_key_border_color" to "#00000000",
        "popup_back_color" to "#3C4043",
        "popup_text_color" to "#E8EAED",
        // theme-defined key color names the layouts reference, dark values
        "enter_bg_color" to "#8AB4F8",
        "enter_text_color" to "#202124",
        "enter_pressed_bg_color" to "#AECBFA",
        "enter_pressed_text_color" to "#202124",
        "one_color" to "#8AB4F8",
        // keep the chain closed: night falls back to itself
        "dark_scheme" to "gboard_dark",
    )

    /** Keys that carry no color and must not leak into fallback tables. */
    private val CHAIN_KEYS = setOf("dark_scheme", "light_scheme")

    val fallback: Map<String, String> = Light.filterKeys { it !in CHAIN_KEYS }
}
