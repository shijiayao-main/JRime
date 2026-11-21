/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import com.jiaoay.jrime.core.Candidates
import com.jiaoay.jrime.core.CompositionProto
import com.jiaoay.jrime.core.SchemaItem
import com.jiaoay.jrime.core.StatusProto
import com.jiaoay.jrime.core.RimeMessage
import com.jiaoay.jrime.daemon.RimeSession
import com.jiaoay.jrime.daemon.launchOnReady
import com.jiaoay.jrime.engine.CandidateView
import com.jiaoay.jrime.engine.CompositionView
import com.jiaoay.jrime.engine.EngineEvent
import com.jiaoay.jrime.engine.ImeSessionState
import com.jiaoay.jrime.engine.ImeStateReducer
import com.jiaoay.jrime.engine.KeyboardId
import com.jiaoay.jrime.engine.PanelId
import com.jiaoay.jrime.ime.broadcast.InputBroadcastReceiver
import com.jiaoay.jrime.ime.broadcast.InputBroadcaster
import com.jiaoay.jrime.ime.clipboard.ClipboardWindow
import com.jiaoay.jrime.ime.keyboard.KeyboardWindow
import com.jiaoay.jrime.ime.symbol.LiquidWindow
import com.jiaoay.jrime.ime.window.BoardWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The M1 bridge between the engine's broadcast stream and the shared surface
 * state: the app-side adapter that turns Rime messages and board-window
 * attaches into [ImeSessionState] events. The surface itself only sees the
 * immutable state flow.
 *
 * The old [ImeUiState] stays in place until the surface switch is verified;
 * this class mirrors its message handling so the two trees can be compared.
 */
class ImeSurfaceController(
    private val broadcaster: InputBroadcaster,
    private val rime: RimeSession,
    private val scope: CoroutineScope,
    private val keyboardWindow: KeyboardWindow,
) : InputBroadcastReceiver {
    private val _state = MutableStateFlow(ImeSessionState())
    val state: StateFlow<ImeSessionState> = _state.asStateFlow()

    /** The board window last attached; the app builds panel slots from it. */
    private val _window = MutableStateFlow<BoardWindow?>(null)
    val window: StateFlow<BoardWindow?> = _window.asStateFlow()

    private var keyboardJob: Job? = null

    fun attach() {
        broadcaster.addReceiver(this)
        rime.launchOnReady { api ->
            // Seed the option/status truth so the first frame renders the
            // current schema and input mode instead of defaults.
            val status = api.statusCached
            _state.update {
                ImeStateReducer.onEvent(
                    it,
                    EngineEvent.Status(
                        asciiMode = status.isAsciiMode,
                        asciiPunctuation = status.isAsciiPunct,
                        fullShape = status.isFullShape,
                        simplified = status.isSimplified,
                        schemaId = status.schemaId,
                        schemaLabel = status.schemaName,
                    ),
                )
            }
        }
        keyboardJob =
            scope.launch {
                keyboardWindow.currentKeyboardName.collect { name ->
                    if (name.isNotEmpty()) {
                        _state.update { it.copy(keyboardId = KeyboardId(name)) }
                    }
                }
            }
    }

    fun detach() {
        keyboardJob?.cancel()
        broadcaster.removeReceiver(this)
    }

    override fun onCompositionUpdate(data: CompositionProto) {
        val preedit = data.preedit.orEmpty()
        val view =
            if (preedit.isEmpty()) {
                null
            } else {
                CompositionView(
                    preedit = preedit,
                    cursor = data.cursorPos,
                    selStart = data.selStart,
                    selEnd = data.selEnd,
                    commitPreview = data.commitTextPreview.orEmpty(),
                )
            }
        _state.update { ImeStateReducer.onEvent(it, EngineEvent.Composition(view)) }
    }

    override fun onCandidateListUpdate(data: Candidates.Bulk) {
        val items =
            data.candidates.mapIndexed { index, candidate ->
                CandidateView(
                    index = index,
                    text = candidate.text,
                    comment = candidate.comment,
                    label = candidate.label,
                )
            }
        _state.update {
            ImeStateReducer.onEvent(
                it,
                EngineEvent.Candidates(
                    items = items,
                    highlighted = data.highlighted,
                    page = 0,
                    pageCount = 1,
                ),
            )
        }
    }

    override fun onKeyAppearanceUpdate(
        composing: Boolean,
        menu: Boolean,
        paging: Boolean,
    ) {
        _state.update { ImeStateReducer.onEvent(it, EngineEvent.Appearance(composing, menu, paging)) }
    }

    override fun onRimeSchemaUpdated(schema: SchemaItem) {
        _state.update { it.copy(schemaLabel = schema.name.ifEmpty { schema.id }) }
    }

    override fun onInputStatusUpdate(value: StatusProto) {
        _state.update {
            ImeStateReducer.onEvent(
                it,
                EngineEvent.Status(
                    asciiMode = value.isAsciiMode,
                    asciiPunctuation = value.isAsciiPunct,
                    fullShape = value.isFullShape,
                    simplified = value.isSimplified,
                    schemaId = value.schemaId,
                    schemaLabel = value.schemaName,
                ),
            )
        }
    }

    override fun onWindowAttached(window: BoardWindow) {
        _window.value = window
        val panel =
            when (window) {
                is KeyboardWindow -> null
                is ClipboardWindow -> PanelId.Clipboard
                is LiquidWindow -> PanelId.Symbols
                else -> null
            }
        _state.update { it.copy(activePanel = panel) }
    }

    override fun onRimeOptionUpdated(value: RimeMessage.OptionMessage.Data) {
        _state.update { ImeStateReducer.onEvent(it, EngineEvent.Option(value.option, value.value)) }
    }
}
