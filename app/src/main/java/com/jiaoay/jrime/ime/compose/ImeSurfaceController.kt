/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import com.jiaoay.jrime.action.ActionResolver
import com.jiaoay.jrime.action.ImeAction
import com.jiaoay.jrime.core.Candidates
import com.jiaoay.jrime.core.CompositionProto
import com.jiaoay.jrime.core.RimeMessage
import com.jiaoay.jrime.core.SchemaItem
import com.jiaoay.jrime.core.StatusProto
import com.jiaoay.jrime.daemon.RimeSession
import com.jiaoay.jrime.daemon.launchOnReady
import com.jiaoay.jrime.data.prefs.AppPrefs
import com.jiaoay.jrime.engine.CandidateView
import com.jiaoay.jrime.engine.CompositionView
import com.jiaoay.jrime.engine.EngineEvent
import com.jiaoay.jrime.engine.ImeCommand
import com.jiaoay.jrime.engine.ImeSessionState
import com.jiaoay.jrime.engine.ImeStateReducer
import com.jiaoay.jrime.engine.KeyboardId
import com.jiaoay.jrime.engine.PanelId
import com.jiaoay.jrime.ime.broadcast.InputBroadcastReceiver
import com.jiaoay.jrime.ime.broadcast.InputBroadcaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The M1 bridge between the engine's broadcast stream and the shared surface
 * state: the app-side adapter that turns Rime messages into [ImeSessionState] events. The surface itself only sees the
 * immutable state flow.
 *
 * The old [ImeUiState] stays in place until the surface switch is verified;
 * this class mirrors its message handling so the two trees can be compared.
 */
class ImeSurfaceController(
    private val broadcaster: InputBroadcaster,
    private val rime: RimeSession,
    private val scope: CoroutineScope,
    presetKeys: Map<String, com.jiaoay.jrime.data.theme.model.PresetKey>,
) : InputBroadcastReceiver {
    private val resolver = ActionResolver(presetKeys)

    private val _state = MutableStateFlow(ImeSessionState())
    val state: StateFlow<ImeSessionState> = _state.asStateFlow()

    fun attach() {
        broadcaster.addReceiver(this)
        rime.launchOnReady { api ->
            // Seed the option/status truth so the first frame renders the
            // current schema and input mode instead of defaults.
            val status = api.statusCached
            // The schema ships a traditional lexicon plus conversion filters;
            // the product default is simplified, so enable zh_simp when no
            // character-form option is set yet.
            val formOptions = listOf("zh_simp", "zh_trad", "zh_hk", "zh_tw")
            val enabledForms = formOptions.count { api.getRuntimeOption(it) }
            if (enabledForms == 0) {
                api.setRuntimeOption("zh_simp", true)
            }
            val simplified = api.getRuntimeOption("zh_simp")
            val appearance = AppPrefs.defaultInstance().appearance
            val pageSize = appearance.candidatePageSize.getValue()
            val paletteMode = appearance.paletteMode.getValue().toEnginePaletteMode()
            val accent = appearance.accent.getValue().toEngineAccent()
            _state.update {
                ImeStateReducer.onEvent(
                    it.copy(
                        candidatePageSize = pageSize,
                        paletteMode = paletteMode,
                        accent = accent,
                        simplified = simplified,
                    ),
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
    }

    /**
     * Applies a UI-local command to the session state (paging, panel switches,
     * keyboard switches). The same command then flows to the executor for its
     * engine/platform effect.
     */
    fun onCommand(command: ImeCommand) {
        // A key slot may carry a surface-state command (switch keyboard, open
        // panel); reduce those before the executor sees the key.
        val translated =
            if (command is ImeCommand.Key) {
                when (val action = resolver.resolve(command.token)) {
                    is ImeAction.SwitchKeyboard -> ImeCommand.SwitchKeyboard(action.keyboard)
                    is ImeAction.OpenPanel -> ImeCommand.OpenPanel(action.panel)
                    ImeAction.ClosePanel -> ImeCommand.ClosePanel
                    else -> command
                }
            } else {
                command
            }
        _state.update { ImeStateReducer.onCommand(it, translated) }
    }

    fun detach() {
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

    override fun onRimeOptionUpdated(value: RimeMessage.OptionMessage.Data) {
        _state.update { ImeStateReducer.onEvent(it, EngineEvent.Option(value.option, value.value)) }
        if (value.option == "zh_simp") {
            _state.update { it.copy(simplified = value.value) }
        }
    }
}
