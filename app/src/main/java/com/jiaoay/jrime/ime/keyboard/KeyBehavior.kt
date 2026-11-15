// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.jiaoay.jrime.ime.keyboard

/** 按键行为枚举  */
enum class KeyBehavior {
    // 状态槽按特异性排序：aux（辅助码输入态）最特异，先于composing解析
    // 长按按键展开列表时，正上方为长按对应按键，排序如上，不展示combo及之前的按键，展示extra
    AUX,
    COMPOSING,
    HAS_MENU,
    PAGING,
    COMBO,
    ASCII,
    CLICK,
    DOUBLE_CLICK,
    LAZY_DOUBLE_CLICK,
    SWIPE_UP,
    LONG_CLICK,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
}
