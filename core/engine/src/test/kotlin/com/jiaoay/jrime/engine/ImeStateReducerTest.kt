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

        "a candidate event replaces the list, highlight and paging" {
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
            state.candidatePage shouldBe 2
            state.candidatePageCount shouldBe 4
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

        "palette mode is stored" {
            ImeStateReducer.onCommand(
                ImeSessionState(),
                ImeCommand.SetPaletteMode(PaletteMode.ForceDark),
            ).paletteMode shouldBe PaletteMode.ForceDark
        }
    })
