/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.core

/**
 * Formats the field-facing inline preedit out of the raw engine composition.
 *
 * The engine only hands out compositions: which shape the app's field shows
 * (the inline preedit mode, the display separator) is the IME side's call,
 * so implementations live above the core layer.
 */
fun interface RimeInlinePreeditFormatter {
    fun formatInlinePreedit(
        composition: CompositionProto,
        isNullInputType: Boolean,
    ): String
}
