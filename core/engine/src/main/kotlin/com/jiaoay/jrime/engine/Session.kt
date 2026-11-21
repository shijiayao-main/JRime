/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.engine

import com.jiaoay.jrime.data.theme.model.KeyActionToken as LegacyKeyToken

/** Which palette the surface should use. */
enum class PaletteMode {
    FollowSystem,
    ForceLight,
    ForceDark,
}

/** Numeric metrics the platform converts to dp/sp; single source of bounds. */
object ImeMetrics {
    const val ROW_HEIGHT_MIN_DP: Int = 36
    const val ROW_HEIGHT_MAX_DP: Int = 80
    const val ROW_HEIGHT_DEFAULT_DP: Int = 50
    const val CONTENT_ROWS: Int = 6
    const val CANDIDATE_PAGE_SIZE: Int = 7
    const val CANDIDATE_PAGE_SIZE_MIN: Int = 5
    const val CANDIDATE_PAGE_SIZE_MAX: Int = 9
    const val CANDIDATE_TEXT_SIZE_DEFAULT: Int = 18
    const val CANDIDATE_TEXT_SIZE_MIN: Int = 15
    const val CANDIDATE_TEXT_SIZE_MAX: Int = 22
}

/** One candidate as the surface needs it. */
data class CandidateView(
    val index: Int,
    val text: String,
    val comment: String = "",
    val label: String = "",
)

/** The composition/preedit snapshot. */
data class CompositionView(
    val preedit: String,
    val cursor: Int = preedit.length,
    val selStart: Int = preedit.length,
    val selEnd: Int = preedit.length,
    val commitPreview: String = "",
)

/** The immutable snapshot the whole surface renders from. */
data class ImeSessionState(
    val composition: CompositionView? = null,
    val candidates: List<CandidateView> = emptyList(),
    val highlightedIndex: Int = -1,
    val candidatePage: Int = 0,
    val candidatePageCount: Int = 1,
    val candidatePageSize: Int = ImeMetrics.CANDIDATE_PAGE_SIZE,
    val composing: Boolean = false,
    val asciiMode: Boolean = false,
    val asciiPunctuation: Boolean = false,
    val fullShape: Boolean = false,
    val simplified: Boolean = true,
    val schemaLabel: String = "",
    val activePanel: PanelId? = null,
    val keyboardId: KeyboardId = KeyboardId.Qwerty26,
    val rowHeightDp: Int = 50,
    val paletteMode: PaletteMode = PaletteMode.FollowSystem,
)

/** Engine pushes. The app adapter translates Rime messages into these. */
sealed interface EngineEvent {
    data class Composition(val view: CompositionView?) : EngineEvent

    data class Candidates(
        val items: List<CandidateView>,
        val highlighted: Int = -1,
        val page: Int = 0,
        val pageCount: Int = 1,
    ) : EngineEvent

    data class Status(
        val asciiMode: Boolean,
        val asciiPunctuation: Boolean,
        val fullShape: Boolean,
        val simplified: Boolean,
        val schemaId: String = "",
        val schemaLabel: String = "",
    ) : EngineEvent

    /** The engine's composing/menu/paging flags (the keyboard appearance stream). */
    data class Appearance(
        val composing: Boolean,
        val hasMenu: Boolean = false,
        val paging: Boolean = false,
    ) : EngineEvent

    data class Commit(val text: String) : EngineEvent

    data class Option(val option: String, val value: Boolean) : EngineEvent
}

/** The candidate long-press menu's actions. */
enum class CandidateActionKind {
    Pin,
    Delete,
    Copy,
    QueryCode,
}

/** UI intents. Only [ImeCommand]s flow from the surface to the rest of the app. */
sealed interface ImeCommand {
    data class Key(val token: LegacyKeyToken, val gesture: KeyGesture = KeyGesture.Tap) : ImeCommand

    data class SelectCandidate(val index: Int) : ImeCommand

    data class LongPressCandidate(val index: Int) : ImeCommand

    data class CommitText(val text: String) : ImeCommand

    data object Backspace : ImeCommand

    data object ToggleAscii : ImeCommand

    data class SetOption(val option: String, val value: Boolean) : ImeCommand

    data class OpenPanel(val id: PanelId) : ImeCommand

    data object ClosePanel : ImeCommand

    data class SwitchKeyboard(val id: KeyboardId) : ImeCommand

    data class MoveCursor(val chars: Int = 0, val lines: Int = 0) : ImeCommand

    data class ChangeCandidatePage(val forward: Boolean) : ImeCommand

    data class SetCandidatePageSize(val size: Int) : ImeCommand

    data class CandidateAction(val index: Int, val action: CandidateActionKind, val text: String = "") : ImeCommand

    data class SetRowHeight(val dp: Int) : ImeCommand

    data class SetPaletteMode(val mode: PaletteMode) : ImeCommand

    data object HideKeyboard : ImeCommand
}

/**
 * The pure state reducer of the surface: engine events and UI-local commands
 * in, a new immutable snapshot out. Everything that touches the engine or the
 * platform lives behind the command dispatcher, not here.
 */
object ImeStateReducer {
    fun onEvent(
        state: ImeSessionState,
        event: EngineEvent,
    ): ImeSessionState = when (event) {
        is EngineEvent.Composition ->
            state.copy(
                composition = event.view,
                composing = event.view != null,
            )

        is EngineEvent.Candidates -> {
            val size = state.candidatePageSize.coerceAtLeast(1)
            val pageCount = ((event.items.size + size - 1) / size).coerceAtLeast(1)
            state.copy(
                candidates = event.items,
                highlightedIndex = event.highlighted,
                candidatePage = 0,
                candidatePageCount = pageCount,
            )
        }

        is EngineEvent.Status ->
            state.copy(
                asciiMode = event.asciiMode,
                asciiPunctuation = event.asciiPunctuation,
                fullShape = event.fullShape,
                simplified = event.simplified,
                schemaLabel = event.schemaLabel.ifEmpty { state.schemaLabel },
            )

        is EngineEvent.Appearance -> state.copy(composing = event.composing)

        is EngineEvent.Commit -> state

        is EngineEvent.Option ->
            when (event.option) {
                "ascii_mode" -> state.copy(asciiMode = event.value)
                "ascii_punct" -> state.copy(asciiPunctuation = event.value)
                "full_shape" -> state.copy(fullShape = event.value)
                "simplification" -> state.copy(simplified = event.value)
                else -> state
            }
    }

    fun onCommand(
        state: ImeSessionState,
        command: ImeCommand,
    ): ImeSessionState = when (command) {
        is ImeCommand.OpenPanel -> state.copy(activePanel = command.id)

        is ImeCommand.ClosePanel -> state.copy(activePanel = null)

        is ImeCommand.SwitchKeyboard ->
            state.copy(
                keyboardId = command.id,
                // A keyboard switch always leaves the panel.
                activePanel = null,
            )

        is ImeCommand.SetRowHeight ->
            state.copy(
                rowHeightDp = command.dp.coerceIn(ImeMetrics.ROW_HEIGHT_MIN_DP, ImeMetrics.ROW_HEIGHT_MAX_DP),
            )

        is ImeCommand.SetPaletteMode -> state.copy(paletteMode = command.mode)

        is ImeCommand.ChangeCandidatePage -> {
            val target =
                (state.candidatePage + if (command.forward) 1 else -1)
                    .coerceIn(0, (state.candidatePageCount - 1).coerceAtLeast(0))
            state.copy(candidatePage = target)
        }

        is ImeCommand.SetCandidatePageSize -> {
            val size = command.size.coerceIn(
                ImeMetrics.CANDIDATE_PAGE_SIZE_MIN,
                ImeMetrics.CANDIDATE_PAGE_SIZE_MAX,
            )
            val pageCount = ((state.candidates.size + size - 1) / size).coerceAtLeast(1)
            state.copy(
                candidatePageSize = size,
                candidatePageCount = pageCount,
                candidatePage = state.candidatePage.coerceAtMost(pageCount - 1),
            )
        }

        else -> state
    }
}
