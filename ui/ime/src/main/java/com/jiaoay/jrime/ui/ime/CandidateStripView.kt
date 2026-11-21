/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.ime

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.jiaoay.jrime.engine.CandidateView
import com.jiaoay.jrime.engine.ImeCommand
import com.jiaoay.jrime.engine.ImeSessionState
import com.jiaoay.jrime.ui.design.ImeDimens
import com.jiaoay.jrime.ui.design.ImeShapes
import com.jiaoay.jrime.ui.design.ImeTypography
import com.jiaoay.jrime.ui.design.LocalImePalette

/** Shown in the empty slot while composing; the module carries no resources. */
private const val EMPTY_HINT: String = "…"

/**
 * The horizontal candidate strip: one chip per candidate, highlighted with the
 * palette's highlight background, the first nine carrying their select-order
 * badge (the engine selects the same candidate when a digit is long-pressed on
 * the letter keys, so candidates past nine have no badge).
 *
 * A tap selects through [ImeCommand.SelectCandidate]; a long press rides
 * [ImeCommand.LongPressCandidate] (delete from the user lexicon). The strip is
 * platform-neutral: every metric comes from the design tokens, so it can be
 * hosted by the top bar or by the keyboard's digit-row slot alike.
 */
@Suppress("FunctionName")
@Composable
fun CandidateStripView(
    state: ImeSessionState,
    config: ImeSurfaceConfig,
    host: ImeSurfaceHost,
    modifier: Modifier = Modifier,
    emptyHint: Boolean = false,
) {
    val palette = LocalImePalette.current
    // The strip keeps its band height even when empty, so the keyboard's digit
    // row (and the composing top bar) never collapse while candidates load.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ImeDimens.CandidateHeight),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (state.candidates.isEmpty()) {
            if (emptyHint) {
                BasicText(
                    text = state.schemaLabel.ifEmpty { EMPTY_HINT },
                    style = ImeTypography.candidateCommentStyle(palette.hintText),
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = ImeDimens.CandidateGap),
                )
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ImeDimens.CandidateGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                itemsIndexed(
                    items = state.candidates,
                    key = { _, candidate -> candidate.index },
                ) { position, candidate ->
                    CandidateChip(
                        position = position,
                        candidate = candidate,
                        highlighted = candidate.index == state.highlightedIndex,
                        config = config,
                        host = host,
                    )
                }
            }
        }
    }
}

/**
 * One candidate chip: the text (user-size from the config) plus the comment
 * when enabled, with the select-order badge at the leading edge above the
 * baseline, Gboard style.
 */
@Suppress("FunctionName")
@Composable
private fun CandidateChip(
    position: Int,
    candidate: CandidateView,
    highlighted: Boolean,
    config: ImeSurfaceConfig,
    host: ImeSurfaceHost,
) {
    val palette = LocalImePalette.current
    val badge = candidateBadgeNumber(position, candidate.label)
    Box(
        modifier = Modifier
            .height(ImeDimens.CandidateHeight)
            .widthIn(min = ImeDimens.CandidateMinWidth)
            .clip(ImeShapes.Chip)
            .background(if (highlighted) palette.candidateHighlightBg else Color.Transparent)
            .combinedClickable(
                onClick = { host.dispatch(ImeCommand.SelectCandidate(candidate.index)) },
                onLongClick = { host.dispatch(ImeCommand.LongPressCandidate(candidate.index)) },
            )
            .padding(horizontal = ImeDimens.CandidateGap),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (badge != null) {
                BasicText(
                    text = badge,
                    style = TextStyle(fontSize = ImeTypography.Badge, color = palette.candidateComment),
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.Top)
                        .padding(end = ImeDimens.CandidateGap / 3),
                )
            }
            BasicText(
                text = candidate.text,
                style = ImeTypography.candidateStyle(
                    color = if (highlighted) palette.candidateHighlighted else palette.candidateText,
                    size = config.candidateTextSizeSp.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (config.showCandidateComment && candidate.comment.isNotEmpty()) {
                BasicText(
                    text = candidate.comment,
                    style = ImeTypography.candidateCommentStyle(palette.candidateComment),
                    maxLines = 1,
                    modifier = Modifier.padding(start = ImeDimens.CandidateGap / 3),
                )
            }
        }
    }
}

/**
 * The select-order badge on a candidate: 1-9 mirror the engine's select keys,
 * and a candidate-provided label wins over the generated digit; candidates
 * past nine have no select key and therefore no badge.
 */
internal fun candidateBadgeNumber(position: Int, label: String = ""): String? {
    if (position !in 0..8) return null
    return label.ifEmpty { (position + 1).toString() }
}
