/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.core

import com.jiaoay.jrime.core.CompositionProto
import com.jiaoay.jrime.core.RimeInlinePreeditFormatter
import com.jiaoay.jrime.data.prefs.AppPrefs

/**
 * The app's inline preedit spelling: picks the composition field per the
 * user's inline preedit mode (null-input editors never show one) and
 * re-spells the aux separator for the field-facing copy.
 */
class InlinePreeditFormatter : RimeInlinePreeditFormatter {
    override fun formatInlinePreedit(
        composition: CompositionProto,
        isNullInputType: Boolean,
    ): String {
        val mode = if (isNullInputType) {
            InlinePreeditMode.DISABLE
        } else {
            AppPrefs.defaultInstance().general.inlinePreeditMode.getValue()
        }
        return when (mode) {
            InlinePreeditMode.DISABLE -> ""

            // The aux separator stays "/" in the engine (the cand_selector
            // contract) and in the composition broadcast (the aux-state
            // detector matches it); only the field-facing copy is re-spelled.
            InlinePreeditMode.COMPOSING_TEXT -> composition.preedit?.replace(AUX_SEPARATOR, AUX_SEPARATOR_DISPLAY) ?: ""

            InlinePreeditMode.COMMIT_TEXT_PREVIEW -> composition.commitTextPreview ?: ""
        }
    }

    companion object {
        /** The aux-code separator the engine parses (the cand_selector contract). */
        private const val AUX_SEPARATOR = "/"

        /** What the app's field shows in its place -- a raw slash reads as a typo. */
        private const val AUX_SEPARATOR_DISPLAY = "·"
    }
}
