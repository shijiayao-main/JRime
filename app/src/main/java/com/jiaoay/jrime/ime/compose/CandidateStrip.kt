/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.jiaoay.jrime.core.CandidateProto

/**
 * The composing code echo: the DISABLE inline mode appends the soft caret to
 * the preedit, so it is stripped for display; the aux separator "/" reads as
 * the middle dot, matching the inline copy. The aux detection still reads the
 * raw preedit -- stripping here never touches the behavior layer.
 */
internal fun compositionEchoText(preedit: String?): String = preedit.orEmpty().trimEnd('\u2038').replace("/", "·")

/**
 * The candidate strip: a horizontally scrolling row of candidates (18sp, the
 * highlighted one bold blue, aux-code comments as small gray suffixes, 1dp
 * hairline dividers). A tap commits through [ImeUiState.selectCandidate] --
 * the same path the action bar's strip always rode on -- and a long-press
 * deletes the suggestion from the user lexicon ([ImeUiState.deleteCandidate]).
 * While composing the
 * strip lives in the keyboard's digit-row slot ([GboardKeyboard]'s
 * [GboardKeyboard.ROW_HEIGHT] top row); the full-data panels keep it in the
 * action bar.
 */
@Suppress("FunctionName")
@Composable
internal fun CandidateStrip(
    state: ImeUiState,
    modifier: Modifier = Modifier,
    emptyHint: Boolean = false,
) {
    LazyRow(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // An empty candidate list mid-composition must not collapse the row
        // (or read as "tap the digits to pick"): a gray nudge keeps the slot.
        if (emptyHint && state.candidates.isEmpty()) {
            item {
                BasicText(
                    text = "继续输入…",
                    style = gboardStyle().candidateCommentStyle,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
        itemsIndexed(state.candidates) { index, candidate ->
            if (index > 0) {
                CandidateDivider()
            }
            CandidateWord(index = index, candidate = candidate, state = state)
        }
    }
}

/**
 * The select-order badge on a candidate: 1-9 mirror the engine's select keys
 * (a long-press on q-p sends the digit, which selects the same candidate);
 * candidates past nine have no select key, so no badge.
 */
internal fun candidateBadgeNumber(index: Int): String? = if (index in 0..8) (index + 1).toString() else null

@OptIn(ExperimentalFoundationApi::class)
@Suppress("FunctionName")
@Composable
internal fun CandidateWord(
    index: Int,
    candidate: CandidateProto,
    state: ImeUiState,
) {
    val style = gboardStyle()
    val haptics = LocalHapticFeedback.current
    val highlighted = index == state.highlightedCandidate
    Row(
        modifier = Modifier
            .combinedClickable(
                onClick = { state.selectCandidate(index) },
                // the fast-input hygiene move: a long-pressed suggestion
                // leaves the user lexicon (haptic first, no dialog -- the
                // Rime stream refreshes the strip on its own)
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.deleteCandidate(index)
                },
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        candidateBadgeNumber(index)?.let { badge ->
            BasicText(
                text = badge,
                style = style.candidateCommentStyle,
                modifier = Modifier.padding(end = 3.dp),
            )
        }
        BasicText(
            text = candidate.text,
            style = if (highlighted) style.candidateHighlightedStyle else style.candidateStyle,
        )
        if (candidate.comment.isNotEmpty()) {
            BasicText(
                text = candidate.comment,
                style = style.candidateCommentStyle,
                modifier = Modifier.padding(start = 2.dp, top = 2.dp),
            )
        }
    }
}

@Suppress("FunctionName")
@Composable
internal fun CandidateDivider() {
    Spacer(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .width(1.dp)
            .height(22.dp)
            .background(LocalImeColors.current.stripDivider),
    )
}
