/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.design

import androidx.compose.foundation.shape.RoundedCornerShape

/** The single source of IME shapes. */
object ImeShapes {
    val Key = RoundedCornerShape(ImeDimens.KeyCorner)
    val Chip = RoundedCornerShape(ImeDimens.ChipCorner)
    val Panel = RoundedCornerShape(ImeDimens.PanelCorner)
    val Popup = RoundedCornerShape(ImeDimens.KeyCorner)
}
