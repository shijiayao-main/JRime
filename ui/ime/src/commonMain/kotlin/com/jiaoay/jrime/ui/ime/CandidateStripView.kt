/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.ime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.jiaoay.jrime.engine.CandidateActionKind
import com.jiaoay.jrime.engine.CandidateView
import com.jiaoay.jrime.engine.ImeCommand
import com.jiaoay.jrime.engine.ImeMetrics
import com.jiaoay.jrime.engine.ImeSessionState
import com.jiaoay.jrime.ui.design.ImeDimens
import com.jiaoay.jrime.ui.design.ImeShapes
import com.jiaoay.jrime.ui.design.ImeTypography
import com.jiaoay.jrime.ui.design.LocalImePalette

/** Shown in the empty slot while composing; the module carries no resources. */
private const val EMPTY_HINT: String = "…"

/**
 * The paged candidate strip.
 *
 * The engine pushes the full candidate list (bulk); paging is the surface's
 * job: the strip renders one page of `state.candidatePageSize` candidates and
 * offers previous/next controls plus a page indicator when there is more than
 * one page. Selection always uses the candidate's global index, so a page
 * boundary can never change what a tap commits.
 *
 * A long press opens the candidate menu (pin / delete / copy / query code)
 * instead of silently deleting the word.
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
    var menuCandidate by remember { mutableStateOf<CandidateView?>(null) }
    val pageSize = state.candidatePageSize.coerceIn(
        ImeMetrics.CANDIDATE_PAGE_SIZE_MIN,
        ImeMetrics.CANDIDATE_PAGE_SIZE_MAX,
    )
    val pageCount = state.candidatePageCount.coerceAtLeast(1)
    val page = state.candidatePage.coerceIn(0, pageCount - 1)
    val visible = state.candidates.drop(page * pageSize).take(pageSize)

    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .height(ImeDimens.CandidateHeight),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (visible.isEmpty()) {
            if (emptyHint) {
                BasicText(
                    text = state.schemaLabel.ifEmpty { EMPTY_HINT },
                    style = ImeTypography.candidateCommentStyle(palette.hintText),
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = ImeDimens.CandidateGap),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (page > 0) {
                    PageArrow(
                        label = "‹",
                        onClick = { host.dispatch(ImeCommand.ChangeCandidatePage(forward = false)) },
                    )
                }
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(ImeDimens.CandidateGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    itemsIndexed(
                        items = visible,
                        key = { _, candidate -> candidate.index },
                    ) { _, candidate ->
                        CandidateChip(
                            candidate = candidate,
                            highlighted = candidate.index == state.highlightedIndex,
                            config = config,
                            onSelect = { host.dispatch(ImeCommand.SelectCandidate(candidate.index)) },
                            onLongPress = { menuCandidate = candidate },
                        )
                    }
                }
                if (page < pageCount - 1) {
                    PageArrow(
                        label = "›",
                        onClick = { host.dispatch(ImeCommand.ChangeCandidatePage(forward = true)) },
                    )
                }
                if (pageCount > 1) {
                    BasicText(
                        text = "${page + 1}/$pageCount",
                        style = ImeTypography.candidateCommentStyle(palette.candidateComment),
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = ImeDimens.CandidateGap / 2f),
                    )
                }
            }
        }
    }

    val target = menuCandidate
    if (target != null) {
        Popup(
            alignment = Alignment.TopCenter,
            onDismissRequest = { menuCandidate = null },
            properties = PopupProperties(focusable = true),
        ) {
            CandidateMenu(
                candidate = target,
                onAction = { kind ->
                    host.dispatch(
                        ImeCommand.CandidateAction(
                            index = target.index,
                            action = kind,
                            text = target.text,
                        ),
                    )
                    menuCandidate = null
                },
            )
        }
    }
}

/** The candidate long-press menu over a freshly pressed candidate. */
@Suppress("FunctionName")
@Composable
private fun CandidateMenu(
    candidate: CandidateView,
    onAction: (CandidateActionKind) -> Unit,
) {
    val palette = LocalImePalette.current
    Column(
        modifier =
        Modifier
            .widthIn(min = ImeDimens.MinTouchTarget * 2)
            .clip(ImeShapes.Panel)
            .background(palette.popupBackground)
            .padding(vertical = ImeDimens.KeyGap),
    ) {
        MenuItem("置顶", palette.popupText) { onAction(CandidateActionKind.Pin) }
        MenuItem("删词", palette.danger) { onAction(CandidateActionKind.Delete) }
        MenuItem("复制", palette.popupText) { onAction(CandidateActionKind.Copy) }
        if (candidate.comment.isNotEmpty()) {
            MenuItem("查编码", palette.popupText) { onAction(CandidateActionKind.QueryCode) }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun MenuItem(
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    BasicText(
        text = label,
        style = ImeTypography.keySymbolStyle(color),
        maxLines = 1,
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = ImeDimens.PanelPadding, vertical = ImeDimens.KeyGap),
    )
}

/** A previous/next page control. */
@Suppress("FunctionName")
@Composable
private fun PageArrow(
    label: String,
    onClick: () -> Unit,
) {
    val palette = LocalImePalette.current
    Box(
        modifier =
        Modifier
            .height(ImeDimens.CandidateHeight)
            .widthIn(min = ImeDimens.CandidateMinWidth)
            .clip(ImeShapes.Chip)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = ImeTypography.candidateStyle(palette.candidateHighlighted),
            maxLines = 1,
        )
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
    candidate: CandidateView,
    highlighted: Boolean,
    config: ImeSurfaceConfig,
    onSelect: () -> Unit,
    onLongPress: () -> Unit,
) {
    val palette = LocalImePalette.current
    val badge = candidateBadgeNumber(candidate.index, candidate.label)
    Box(
        modifier =
        Modifier
            .height(ImeDimens.CandidateHeight)
            .widthIn(min = ImeDimens.CandidateMinWidth)
            .clip(ImeShapes.Chip)
            .background(if (highlighted) palette.candidateHighlightBg else Color.Transparent)
            .combinedClickable(
                onClick = onSelect,
                onLongClick = onLongPress,
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
                    modifier =
                    Modifier
                        .align(Alignment.Top)
                        .padding(end = ImeDimens.CandidateGap / 3),
                )
            }
            BasicText(
                text = candidate.text,
                style =
                ImeTypography.candidateStyle(
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
