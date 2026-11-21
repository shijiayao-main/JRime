/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.engine

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ImeStateReducerTest :
    StringSpec({
        "a composition event starts composing and clears at null" {
            val state = ImeStateReducer.onEvent(
                ImeSessionState(),
                EngineEvent.Composition(CompositionView("nihao")),
            )
            state.composing shouldBe true
            state.composition?.preedit shouldBe "nihao"

            val cleared = ImeStateReducer.onEvent(state, EngineEvent.Composition(null))
            cleared.composing shouldBe false
            cleared.composition shouldBe null
        }

        "a bulk candidate list is paged by the state's page size" {
            val items = (0 until 16).map { CandidateView(it, "c$it") }
            val state = ImeStateReducer.onEvent(
                ImeSessionState(candidatePageSize = 7),
                EngineEvent.Candidates(items),
            )
            state.candidates.size shouldBe 16
            state.candidatePage shouldBe 0
            state.candidatePageCount shouldBe 3
        }

        "page flipping clamps at both ends" {
            val items = (0 until 16).map { CandidateView(it, "c$it") }
            var state = ImeStateReducer.onEvent(ImeSessionState(), EngineEvent.Candidates(items))
            state = ImeStateReducer.onCommand(state, ImeCommand.ChangeCandidatePage(forward = false))
            state.candidatePage shouldBe 0
            state = ImeStateReducer.onCommand(state, ImeCommand.ChangeCandidatePage(forward = true))
            state.candidatePage shouldBe 1
            state = ImeStateReducer.onCommand(state, ImeCommand.ChangeCandidatePage(forward = true))
            state = ImeStateReducer.onCommand(state, ImeCommand.ChangeCandidatePage(forward = true))
            state.candidatePage shouldBe 2
        }

        "changing the page size recomputes the page count and keeps the page in range" {
            val items = (0 until 16).map { CandidateView(it, "c$it") }
            var state = ImeStateReducer.onEvent(ImeSessionState(), EngineEvent.Candidates(items))
            state = ImeStateReducer.onCommand(state, ImeCommand.SetCandidatePageSize(9))
            state.candidatePageSize shouldBe 9
            state.candidatePageCount shouldBe 2
            state = ImeStateReducer.onCommand(state, ImeCommand.SetCandidatePageSize(5))
            state.candidatePageSize shouldBe 5
            state.candidatePageCount shouldBe 4
            (state.candidatePage <= state.candidatePageCount - 1) shouldBe true
        }

        "a candidate event resets to the first page" {
            val candidates =
                listOf(
                    CandidateView(0, "你"),
                    CandidateView(1, "尼"),
                )
            val state = ImeStateReducer.onEvent(
                ImeSessionState(),
                EngineEvent.Candidates(candidates, highlighted = 1, page = 2, pageCount = 4),
            )
            state.candidates shouldBe candidates
            state.highlightedIndex shouldBe 1
            state.candidatePage shouldBe 0
            state.candidatePageCount shouldBe 1
        }

        "a status event maps the engine flags" {
            val state = ImeStateReducer.onEvent(
                ImeSessionState(),
                EngineEvent.Status(
                    asciiMode = true,
                    asciiPunctuation = true,
                    fullShape = true,
                    simplified = false,
                    schemaId = "flypy_xhfast",
                    schemaLabel = "飞鹤快拼",
                ),
            )
            state.asciiMode shouldBe true
            state.asciiPunctuation shouldBe true
            state.fullShape shouldBe true
            state.simplified shouldBe false
            state.schemaLabel shouldBe "飞鹤快拼"
        }

        "option events update the matching flags" {
            var state = ImeSessionState()
            state = ImeStateReducer.onEvent(state, EngineEvent.Option("ascii_punct", true))
            state.asciiPunctuation shouldBe true
            state = ImeStateReducer.onEvent(state, EngineEvent.Option("simplification", false))
            state.simplified shouldBe false
            val before = state
            state = ImeStateReducer.onEvent(state, EngineEvent.Option("unknown", true))
            state shouldBe before
        }

        "opening and closing a panel" {
            val opened = ImeStateReducer.onCommand(
                ImeSessionState(),
                ImeCommand.OpenPanel(PanelId.Clipboard),
            )
            opened.activePanel shouldBe PanelId.Clipboard
            val closed = ImeStateReducer.onCommand(opened, ImeCommand.ClosePanel)
            closed.activePanel shouldBe null
        }

        "switching the keyboard leaves the panel" {
            val state = ImeSessionState(activePanel = PanelId.Symbols)
            val switched = ImeStateReducer.onCommand(
                state,
                ImeCommand.SwitchKeyboard(KeyboardId.Flyhe36),
            )
            switched.keyboardId shouldBe KeyboardId.Flyhe36
            switched.activePanel shouldBe null
        }

        "row height is clamped to the supported range" {
            ImeStateReducer.onCommand(ImeSessionState(), ImeCommand.SetRowHeight(10)).rowHeightDp shouldBe
                ImeMetrics.ROW_HEIGHT_MIN_DP
            ImeStateReducer.onCommand(ImeSessionState(), ImeCommand.SetRowHeight(200)).rowHeightDp shouldBe
                ImeMetrics.ROW_HEIGHT_MAX_DP
            ImeStateReducer.onCommand(ImeSessionState(), ImeCommand.SetRowHeight(56)).rowHeightDp shouldBe 56
        }

        "the accent preset is stored" {
            ImeStateReducer.onCommand(
                ImeSessionState(),
                ImeCommand.SetAccent(AccentPreset.Purple),
            ).accent shouldBe AccentPreset.Purple
        }

        "palette mode is stored" {
            ImeStateReducer.onCommand(
                ImeSessionState(),
                ImeCommand.SetPaletteMode(PaletteMode.ForceDark),
            ).paletteMode shouldBe PaletteMode.ForceDark
        }
    })
