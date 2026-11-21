/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import com.jiaoay.jrime.core.CompositionProto

/**
 * The composing-code echo: the preedit with the soft caret stripped and the
 * aux separator rendered as the middle dot (the face shows the code, the
 * engine still gets "/").
 */
internal fun compositionEchoText(preedit: String?): String = preedit.orEmpty().trimEnd('\u2038').replace("/", "·")

/**
 * The aux-code gate, mirrored from cand_selector.lua: 2-4 input letters,
 * the "/" separator, then 0-2 form letters or a tone mark (I/U/N/M) at the
 * tail. Confirmed segments render as CJK text, so a non-letter (or the
 * string head) is the left boundary; a 5+ letter run is never an aux code.
 */
private val AUX_PREEDIT = Regex("(?:^|[^a-z])[a-z]{2,4}/([a-z]{0,2}|[IUNM])$")

/**
 * Whether the composition's active segment is taking aux codes: the preedit
 * tail reads as input letters + "/" + form codes. The DISABLE inline mode
 * appends the soft caret to the preedit, so it is stripped first.
 */
internal fun CompositionProto.isAuxActive(): Boolean {
    val text = preedit?.trimEnd('\u2038') ?: return false
    return text.isNotEmpty() && AUX_PREEDIT.containsMatchIn(text)
}
