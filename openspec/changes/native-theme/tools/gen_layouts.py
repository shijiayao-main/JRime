#!/usr/bin/env python3
"""One-shot scaffolding generator for native-theme task 1.2.

Transcribes the kept closure of flyhe.trime.yaml -- the seven keyboards and
the 71 preset keys from the 4.1 analysis (openspec/changes/startup-fastpath/
theme-trim-list.md) -- into the NativeTheme Kotlin DSL. Output is committed
as the new source of truth; this script is transitional provenance and dies
with the YAML engine (task 1.4).

Run from the repository root:
    python3 -I openspec/changes/native-theme/tools/gen_layouts.py
"""

import re
import sys

import yaml

REPO = "."
THEME = f"{REPO}/app/src/main/assets/shared/flyhe.trime.yaml"
OUT = f"{REPO}/app/src/main/java/com/jiaoay/jrime/theme/KeyLayouts.kt"

KEPT_KEYBOARDS = [
    "default",
    "flyhe36",
    "number",
    "editor",
    "basic_symbol",
    "math_symbol",
    "t9_symbol",
]

# The 71-key closure from theme-trim-list.md section 4.1: A (64 keyboard
# closure) + B (7 tool_bar/liquid references). The aux-code key replaces the
# schema-switch slot on the default keyboard's bottom row (design section 2.3).
AUX_KEY_REPLACES = "Schema_switch"

# Behavior slots in TextKey order; the DSL takes each by keyword.
SLOTS = [
    "click",
    "double_click",
    "lazy_double_click",
    "long_click",
    "swipe_up",
    "swipe_down",
    "swipe_left",
    "swipe_right",
    "composing",
    "has_menu",
    "paging",
    "combo",
    "ascii",
]

DSL_SLOT = {
    "double_click": "doubleClick",
    "lazy_double_click": "lazyDoubleClick",
    "long_click": "longClick",
    "swipe_up": "swipeUp",
    "swipe_down": "swipeDown",
    "swipe_left": "swipeLeft",
    "swipe_right": "swipeRight",
}

# TextKey visual fields the DSL mirrors (plus offsets/colors used by the
# function-row keys); everything else stays at model defaults.
VISUAL = [
    "label",
    "label_symbol",
    "hint",
    "width",
    "height",
    "key_text_size",
    "symbol_text_size",
    "key_text_offset_y",
    "key_symbol_offset_x",
    "key_back_color",
    "key_text_color",
    "key_symbol_color",
    "hilited_key_back_color",
    "hilited_key_text_color",
]

FLOAT_VISUAL = {"width", "height", "key_text_size", "symbol_text_size", "key_text_offset_y", "key_symbol_offset_x"}

DSL_VISUAL = {
    "label_symbol": "labelSymbol",
    "key_text_size": "keyTextSize",
    "symbol_text_size": "symbolTextSize",
    "key_text_offset_y": "keyTextOffsetY",
    "key_symbol_offset_x": "keySymbolOffsetX",
    "key_back_color": "keyBackColor",
    "key_text_color": "keyTextColor",
    "key_symbol_color": "keySymbolColor",
    "hilited_key_back_color": "hilitedKeyBackColor",
    "hilited_key_text_color": "hilitedKeyTextColor",
}

# Keyboard-level fields the DSL mirrors.
BOARD_FIELDS = [
    "author",
    "width",
    "height",
    "keyboard_height",
    "keyboard_height_land",
    "auto_height_index",
    "horizontal_gap",
    "vertical_gap",
    "round_corner",
    "key_border",
    "columns",
    "ascii_mode",
    "reset_ascii_mode",
    "label_transform",
    "lock",
    "ascii_keyboard",
    "landscape_keyboard",
    "landscape_split_percent",
    "key_text_size",
    "symbol_text_size",
    "key_text_offset_x",
    "key_text_offset_y",
    "key_symbol_offset_x",
    "key_symbol_offset_y",
    "key_hint_offset_x",
    "key_hint_offset_y",
    "key_press_offset_x",
    "key_press_offset_y",
]
DSL_BOARD = {
    "keyboard_height": "keyboardHeight",
    "keyboard_height_land": "keyboardHeightLand",
    "auto_height_index": "autoHeightIndex",
    "horizontal_gap": "horizontalGap",
    "vertical_gap": "verticalGap",
    "round_corner": "roundCorner",
    "key_border": "keyBorder",
    "ascii_mode": "asciiMode",
    "reset_ascii_mode": "resetAsciiMode",
    "label_transform": "labelTransform",
    "ascii_keyboard": "asciiKeyboard",
    "landscape_keyboard": "landscapeKeyboard",
    "landscape_split_percent": "landscapeSplitPercent",
    "key_text_size": "keyTextSize",
    "symbol_text_size": "symbolTextSize",
    "key_text_offset_x": "keyTextOffsetX",
    "key_text_offset_y": "keyTextOffsetY",
    "key_symbol_offset_x": "keySymbolOffsetX",
    "key_symbol_offset_y": "keySymbolOffsetY",
    "key_hint_offset_x": "keyHintOffsetX",
    "key_hint_offset_y": "keyHintOffsetY",
    "key_press_offset_x": "keyPressOffsetX",
    "key_press_offset_y": "keyPressOffsetY",
}
BOARD_FLOATS = {"width", "height", "round_corner", "key_text_size", "symbol_text_size",
                "key_text_offset_x", "key_text_offset_y", "key_symbol_offset_x",
                "key_symbol_offset_y", "key_hint_offset_x", "key_hint_offset_y",
                "key_press_offset_x", "key_press_offset_y"}

# PresetKey fields, in constructor order; Kotlin param names are camelCase.
PRESET_FIELDS = [
    ("command", "command"),
    ("option", "option"),
    ("select", "select"),
    ("toggle", "toggle"),
    ("label", "label"),
    ("preview", "preview"),
    ("shift_lock", "shiftLock"),
    ("commit", "commit"),
    ("text", "text"),
    ("sticky", "sticky"),
    ("repeatable", "repeatable"),
    ("slide_cursor", "slideCursor"),
    ("slide_delete", "slideDelete"),
    ("functional", "functional"),
    ("send", "send"),
]


def kstr(value):
    """Kotlin string literal."""
    text = str(value).replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n")
    return f'"{text}"'


def key_expr(entry):
    """One DSL key() call for a YAML key entry."""
    if not isinstance(entry, dict):
        sys.exit(f"non-mapping key entry: {entry!r}")
    if all(
        not entry.get(slot)
        for slot in SLOTS
    ):
        # spacer: width-only filler
        width = entry.get("width", 0)
        return f"spacer({width}f)" if width else "spacer(0f)"
    parts = []
    for slot in SLOTS:
        value = entry.get(slot)
        if value in (None, "", [], {}):
            continue
        name = DSL_SLOT.get(slot, slot)
        if name == "click":
            parts.append(f'click = {kstr(value)}')
        else:
            parts.append(f"{name} = {kstr(value)}")
    for field in VISUAL:
        value = entry.get(field)
        if value in (None, "", 0, 0.0, -1, -1.0):
            continue
        name = DSL_VISUAL.get(field, field)
        if field in FLOAT_VISUAL:
            parts.append(f"{name} = {value}f")
        else:
            parts.append(f"{name} = {kstr(value)}")
    popup = entry.get("popup")
    if popup:
        items = ", ".join(kstr(item) for item in popup)
        parts.append(f"popup = listOf({items})")
    body = ", ".join(parts)
    return f"key({body})" if body else "spacer(0f)"


def keyboard_expr(name, board):
    lines = [f'keyboard("{name}") {{']
    for field in BOARD_FIELDS:
        value = board.get(field)
        if value is None or value == "" or value == "NONE":
            continue
        dsl_name = DSL_BOARD.get(field, field)
        if field == "label_transform":
            lines.append(f"    {dsl_name} = TextKeyboard.LabelTransform.{value}")
        elif field in BOARD_FLOATS:
            lines.append(f"    {dsl_name} = {value}f")
        elif isinstance(value, bool):
            lines.append(f"    {dsl_name} = {'true' if value else 'false'}")
        elif isinstance(value, (int, float)):
            lines.append(f"    {dsl_name} = {value}")
        else:
            lines.append(f"    {dsl_name} = {kstr(value)}")
    for entry in board.get("keys", []):
        if AUX_KEY_REPLACES and isinstance(entry, dict) and entry.get("click") == AUX_KEY_REPLACES:
            lines.append("    // 辅助码键：单方案化后方案切换已无意义，键位让给一键辅助码（design §2.3）")
            parts = ['click = "/"', 'label = "/"', 'hint = "辅助"']
            width = entry.get("width")
            if width:
                parts.append(f"width = {width}f")
            if entry.get("key_text_offset_y") is not None:
                parts.append(f"keyTextOffsetY = {entry.get('key_text_offset_y')}f")
            for field, dsl in (
                ("key_back_color", "keyBackColor"),
                ("key_text_color", "keyTextColor"),
                ("hilited_key_back_color", "hilitedKeyBackColor"),
                ("hilited_key_text_color", "hilitedKeyTextColor"),
            ):
                if entry.get(field):
                    parts.append(f"{dsl} = {kstr(entry[field])}")
            if entry.get("long_click"):
                parts.append(f'longClick = {kstr(entry["long_click"])}')
            if entry.get("swipe_down"):
                parts.append(f'swipeDown = {kstr(entry["swipe_down"])}')
            lines.append(f"    key({', '.join(parts)})")
            continue
        expr = key_expr(entry)
        for line in expr.splitlines():
            lines.append(f"    {line}")
    lines.append("}")
    return "\n".join(lines)


def preset_expr(name, preset):
    parts = []
    for field, dsl in PRESET_FIELDS:
        value = preset.get(field)
        if value in (None, "", False, []):
            continue
        if isinstance(value, bool):
            if value:
                parts.append(f"{dsl} = true")
        else:
            parts.append(f"{dsl} = {kstr(value)}")
    states = preset.get("states")
    if states:
        items = ", ".join(kstr(state) for state in states)
        parts.append(f"states = listOf({items})")
    body = ", ".join(parts)
    return f'    "{name}" to PresetKey({body}),' if body else f'    "{name}" to PresetKey(),'


def kept_preset_names(theme):
    """The kept 71-key closure: every preset referenced by the kept keyboards'
    13 slots + popups, plus the tool_bar/liquid/liquid_keyboard references."""
    wanted = set()

    def collect(value):
        if isinstance(value, str) and value in theme.get("preset_keys", {}):
            wanted.add(value)
        elif isinstance(value, list):
            for item in value:
                collect(item)
        elif isinstance(value, dict):
            for item in value.values():
                collect(item)

    for name in KEPT_KEYBOARDS:
        board = theme["preset_keyboards"][name]
        for entry in board.get("keys", []):
            collect(entry)
    for section in ("tool_bar", "liquid_keyboard"):
        collect(theme.get(section, {}))
    # top-level theme key actions (return_keyboard etc.) ride in preset_keys too
    return wanted


def main():
    with open(THEME, encoding="utf-8") as handle:
        theme = yaml.safe_load(handle)

    presets = kept_preset_names(theme)
    boards = []
    for name in KEPT_KEYBOARDS:
        if name not in theme.get("preset_keyboards", {}):
            sys.exit(f"missing keyboard: {name}")
        boards.append(keyboard_expr(name, theme["preset_keyboards"][name]))

    preset_lines = [
        preset_expr(name, theme["preset_keys"][name])
        for name in sorted(presets)
    ]

    out = []
    out.append("/*")
    out.append(" * SPDX-FileCopyrightText: 2015 - 2026 Rime community")
    out.append(" * SPDX-License-Identifier: GPL-3.0-or-later")
    out.append(" */")
    out.append("")
    out.append("package com.jiaoay.jrime.theme")
    out.append("")
    out.append("import com.jiaoay.jrime.data.theme.model.PresetKey")
    out.append("import com.jiaoay.jrime.data.theme.model.TextKeyboard")
    out.append("")
    out.append("/**")
    out.append(" * The hardcoded keyboard layouts and key actions.")
    out.append(" *")
    out.append(" * Generated once from the flyhe.trime.yaml closure by")
    out.append(" * openspec/changes/native-theme/tools/gen_layouts.py (task 1.2), reviewed")
    out.append(" * against the 4.1 closure analysis, and frozen: this file is now the")
    out.append(" * single source of truth and is edited by hand from here on.")
    out.append(" */")
    out.append("object KeyLayouts {")
    out.append("    val keyboards: Map<String, TextKeyboard> = mapOf(")
    for name, board in zip(KEPT_KEYBOARDS, boards):
        indented = board.replace(chr(10), chr(10) + "        ")
        out.append(f'        "{name}" to {indented},')
    out.append("    ).mapValues { (_, builder) ->")
    out.append("        builder.build(TextKeyboard.DEFAULTS)")
    out.append("    }")
    out.append("")
    out.append("    val presetKeys: Map<String, PresetKey> = mapOf(")
    out.extend(f"        {line}" for line in preset_lines)
    out.append("    )")
    out.append("}")
    out.append("")

    with open(OUT, "w", encoding="utf-8") as handle:
        handle.write("\n".join(out))
    print(f"wrote {OUT}: {len(KEPT_KEYBOARDS)} keyboards, {len(presets)} preset keys")


if __name__ == "__main__":
    main()
