/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.core

import android.content.SharedPreferences
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.jiaoay.jrime.core.RimeMessage
import com.jiaoay.jrime.daemon.RimeSession
import com.jiaoay.jrime.data.prefs.AppPrefs
import com.jiaoay.jrime.data.theme.ColorManager
import com.jiaoay.jrime.data.theme.ThemeScope
import com.jiaoay.jrime.engine.ImeCommand
import com.jiaoay.jrime.engine.ImeSessionState
import com.jiaoay.jrime.engine.KeyBinding
import com.jiaoay.jrime.engine.KeyGesture
import com.jiaoay.jrime.engine.KeyboardCatalog
import com.jiaoay.jrime.engine.PanelId
import com.jiaoay.jrime.ime.broadcast.InputBroadcaster
import com.jiaoay.jrime.ime.clipboard.ClipboardPanel
import com.jiaoay.jrime.ime.compose.ClipboardChip
import com.jiaoay.jrime.ime.compose.ImeColors
import com.jiaoay.jrime.ime.compose.ImeCommandExecutor
import com.jiaoay.jrime.ime.compose.ImeSurfaceController
import com.jiaoay.jrime.ime.compose.LiquidPanel
import com.jiaoay.jrime.ime.compose.LocalImeColors
import com.jiaoay.jrime.ime.compose.panels.ClipboardPanelState
import com.jiaoay.jrime.ime.compose.panels.SymbolsPanelState
import com.jiaoay.jrime.ime.compose.toEngineAccent
import com.jiaoay.jrime.ime.compose.toEnginePaletteMode
import com.jiaoay.jrime.ime.keyboard.InputFeedbackManager
import com.jiaoay.jrime.ime.phrase.CustomPhraseStore
import com.jiaoay.jrime.ime.phrase.PhrasePanel
import com.jiaoay.jrime.ui.design.ImeIcon
import com.jiaoay.jrime.ui.ime.ImeSurface
import com.jiaoay.jrime.ui.ime.ImeSurfaceConfig
import com.jiaoay.jrime.ui.ime.ImeSurfaceHost
import com.jiaoay.jrime.ui.ime.PanelSlot
import com.jiaoay.jrime.ui.ime.ToolbarAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The Compose input view: the only input surface. It owns the Rime message
 * pump, the shared-surface controller/executor and the panel slot wiring; the
 * legacy `InputView`/`BoardWindow` tree is gone (P3b).
 */
class ComposeInputView(
    private val service: TrimeInputMethodService,
    private val rime: RimeSession,
    private val scope: ThemeScope,
) : ConstraintLayout(service) {
    private val broadcaster = InputBroadcaster()
    private val catalog = KeyboardCatalog.shipped()
    private val controller = ImeSurfaceController(broadcaster, rime, service.lifecycleScope, catalog.presetKeys)
    private val executor =
        ImeCommandExecutor(
            service = service,
            rime = rime,
            presetKeys = catalog.presetKeys,
            shiftTogglesLanguage = { asciiSwitchMode != AppPrefs.AsciiSwitchMode.BUTTON_ONLY },
        )

    private val generalPrefs = AppPrefs.defaultInstance().general
    private val keyboardPrefs = AppPrefs.defaultInstance().keyboard
    private val appearancePrefs = AppPrefs.defaultInstance().appearance
    private var rowHeightDp by mutableIntStateOf(keyboardPrefs.rowHeight.getValue())
    private var popupOnKeyPress by mutableStateOf(keyboardPrefs.popupOnKeyPress.getValue())
    private var spaceLongPressVoice by mutableStateOf(keyboardPrefs.spaceLongPressVoice.getValue())
    private var candidateTextSize by mutableIntStateOf(appearancePrefs.candidateTextSize.getValue())
    private var showKeyHints by mutableStateOf(appearancePrefs.showKeyHints.getValue())
    private var showCandidateComment by mutableStateOf(appearancePrefs.showCandidateComment.getValue())
    private var asciiSwitchMode by mutableStateOf(generalPrefs.asciiSwitchMode.getValue())
    private var colorsRevision by mutableIntStateOf(0)
    private var bandTop = Int.MIN_VALUE
    private var messageJob: Job? = null

    /** Physical-keyboard visibility gate owned by [InputDeviceManager]. */
    var handleMessages: Boolean = false
        set(value) {
            field = value
            if (value && messageJob == null) {
                messageJob =
                    service.lifecycleScope.launch {
                        rime.run { messageFlow }.collect(::handleRimeMessage)
                    }
            } else if (!value) {
                messageJob?.cancel()
                messageJob = null
            }
        }

    private val host =
        object : ImeSurfaceHost {
            override fun dispatch(command: ImeCommand) {
                controller.onCommand(command)
                executor.execute(command)
            }

            override fun reportBandTop(topPx: Int) {
                if (topPx >= 0 && topPx != bandTop) {
                    bandTop = topPx
                    post { requestApplyInsets() }
                }
            }

            override fun onKeyFeedback(
                binding: KeyBinding,
                gesture: KeyGesture,
            ) {
                InputFeedbackManager.keyPressSound()
                InputFeedbackManager.keyPressSpeak(0)
                when (gesture) {
                    KeyGesture.Tap -> InputFeedbackManager.keyPressVibrate(this@ComposeInputView)

                    KeyGesture.LongPress,
                    is KeyGesture.SlideCursor,
                    -> InputFeedbackManager.keyPressVibrate(this@ComposeInputView, longPress = true)

                    else -> Unit
                }
            }

            override fun compositionEcho(): String = controller.state.value.composition?.preedit.orEmpty()
        }

    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                AppPrefs.Keyboard.KEYBOARD_HEIGHT -> rowHeightDp = keyboardPrefs.rowHeight.getValue()

                AppPrefs.Keyboard.POPUP_ON_KEY_PRESS -> popupOnKeyPress = keyboardPrefs.popupOnKeyPress.getValue()

                AppPrefs.Keyboard.SPACE_LONG_PRESS_VOICE ->
                    spaceLongPressVoice = keyboardPrefs.spaceLongPressVoice.getValue()

                AppPrefs.Appearance.CANDIDATE_TEXT_SIZE ->
                    candidateTextSize = appearancePrefs.candidateTextSize.getValue()

                AppPrefs.Appearance.SHOW_KEY_HINTS -> showKeyHints = appearancePrefs.showKeyHints.getValue()

                AppPrefs.Appearance.SHOW_CANDIDATE_COMMENT ->
                    showCandidateComment = appearancePrefs.showCandidateComment.getValue()

                AppPrefs.General.ASCII_SWITCH_MODE -> asciiSwitchMode = generalPrefs.asciiSwitchMode.getValue()

                AppPrefs.Appearance.CANDIDATE_PAGE_SIZE ->
                    controller.onCommand(
                        ImeCommand.SetCandidatePageSize(appearancePrefs.candidatePageSize.getValue()),
                    )

                AppPrefs.Appearance.PALETTE_MODE ->
                    controller.onCommand(
                        ImeCommand.SetPaletteMode(appearancePrefs.paletteMode.getValue().toEnginePaletteMode()),
                    )

                AppPrefs.Appearance.ACCENT ->
                    controller.onCommand(ImeCommand.SetAccent(appearancePrefs.accent.getValue().toEngineAccent()))
            }
        }

    private val compose: ComposeView

    init {
        Timber.d("ComposeInputView init (shared surface)")
        controller.attach()
        CustomPhraseStore.bind(service.lifecycleScope)
        keyboardPrefs.rowHeight.sharedPreferences.registerOnSharedPreferenceChangeListener(prefListener)
        compose =
            ComposeView(service).apply {
                val owner = ComposeLifecycleOwner()
                owner.performRestore()
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    val state by controller.state.collectAsState()
                    key(colorsRevision) {
                        ImeSurface(
                            state = state,
                            config = surfaceConfig(state),
                            catalog = catalog,
                            host = host,
                        )
                    }
                }
            }
        val params =
            ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
            )
        params.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        params.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
        params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        addView(compose, params)
    }

    // -- surface config ----------------------------------------------------
    private fun surfaceConfig(state: ImeSessionState): ImeSurfaceConfig = ImeSurfaceConfig(
        rowHeightDp = rowHeightDp,
        popupOnKeyPress = popupOnKeyPress,
        candidateTextSizeSp = candidateTextSize.toFloat(),
        showKeyHints = showKeyHints,
        showCandidateComment = showCandidateComment,
        showLanguageToggle = asciiSwitchMode != AppPrefs.AsciiSwitchMode.SHIFT_ONLY,
        shiftTogglesLanguage = asciiSwitchMode != AppPrefs.AsciiSwitchMode.BUTTON_ONLY,
        spaceLongPressVoice = spaceLongPressVoice,
        enterLabel = "↵",
        toolbarActions = toolbarActions(state),
        panelSlots = panelSlots(),
        clipboardChip = {
            legacyImeColors {
                ClipboardChip(
                    onCommit = { text -> executor.execute(ImeCommand.CommitText(text)) },
                    panelOpen = state.activePanel != null,
                )
            }
        },
    )

    private fun closePanel() {
        controller.onCommand(ImeCommand.ClosePanel)
    }

    private fun toolbarActions(state: ImeSessionState): List<ToolbarAction> = listOf(
        toolbar("settings", "Settings", "Settings", ImeIcon.Settings),
        toolbar(
            id = "punct",
            label = if (state.asciiPunctuation) ". ," else "，。",
            token = "punct_switch",
        ),
        toolbar("number", "123", "Keyboard_number", ImeIcon.NumberPad),
        toolbar("clipboard", "剪贴板", "clipboard_window", ImeIcon.Clipboard),
        toolbar("emoji", "表情", "liquid_keyboard_emoji", ImeIcon.Emoji),
        ToolbarAction(
            id = "simplified",
            label = if (state.simplified) "简" else "繁",
            command = ImeCommand.SetSimplified(!state.simplified),
            highlighted = !state.simplified,
        ),
        ToolbarAction(
            id = "fullshape",
            label = if (state.fullShape) "全" else "半",
            command = ImeCommand.SetOption("full_shape", !state.fullShape),
            highlighted = state.fullShape,
        ),
        ToolbarAction(
            id = "phrase",
            label = "常用语",
            icon = ImeIcon.Phrase,
            command = ImeCommand.OpenPanel(PanelId.Phrase),
        ),
    )

    private fun toolbar(
        id: String,
        label: String,
        token: String,
        icon: ImeIcon? = null,
    ): ToolbarAction {
        val keyToken = com.jiaoay.jrime.data.theme.model.KeyActionToken.Plain(token)
        return ToolbarAction(
            id = id,
            label = label,
            icon = icon,
            command = ImeCommand.Key(keyToken),
            binding = KeyBinding.resolve(keyToken, catalog.presetKeys),
        )
    }

    private fun panelSlots(): List<PanelSlot> = listOf(
        PanelSlot(PanelId.Clipboard, "剪贴板", ImeIcon.Clipboard) { modifier ->
            val state = remember { ClipboardPanelState(service, onClosePanel = ::closePanel) }
            DisposableEffect(state) {
                state.start()
                onDispose { state.stop() }
            }
            legacyImeColors { ClipboardPanel(state, modifier) }
        },
        PanelSlot(PanelId.Phrase, "常用语", ImeIcon.Phrase) { modifier ->
            PhrasePanel(modifier = modifier) { text ->
                controller.onCommand(ImeCommand.ClosePanel)
                executor.execute(ImeCommand.CommitText(text))
            }
        },
        PanelSlot(PanelId.Symbols, "符号", ImeIcon.Emoji) { modifier ->
            symbolsSlot(modifier, initialTab = 0)
        },
        PanelSlot(PanelId.Emoji, "表情", ImeIcon.Emoji) { modifier ->
            symbolsSlot(modifier, initialTab = null)
        },
    )

    @Composable
    private fun symbolsSlot(
        modifier: androidx.compose.ui.Modifier,
        initialTab: Int?,
    ) {
        val state =
            remember {
                SymbolsPanelState(
                    service = service,
                    rime = rime,
                    scope = scope,
                    onClosePanel = ::closePanel,
                )
            }
        LaunchedEffect(Unit) {
            if (state.currentTabIndex >= 0) return@LaunchedEffect
            val index =
                initialTab ?: state.tabs.indexOfFirst {
                    it.label == "😀" || it.type.name.equals("EMOJI", ignoreCase = true)
                }
            state.setDataByIndex(index.coerceAtLeast(0))
        }
        legacyImeColors { LiquidPanel(state, modifier) }
    }

    /**
     * The panel surfaces still read the legacy [LocalImeColors]; this provider
     * keeps them rendering from the same palette until P4 removes the palette.
     */
    @Composable
    private fun legacyImeColors(content: @Composable () -> Unit) {
        val colors = ColorManager.currentScope()?.let { ImeColors(it.colors) }
        if (colors != null) {
            CompositionLocalProvider(LocalImeColors provides colors, content = content)
        }
    }

    // -- rime message pump -------------------------------------------------
    private fun handleRimeMessage(message: RimeMessage<*>) {
        when (message) {
            is RimeMessage.SchemaMessage -> {
                broadcaster.onRimeSchemaUpdated(message.data)
                controller.onCommand(ImeCommand.ClosePanel)
            }

            is RimeMessage.OptionMessage -> {
                broadcaster.onRimeOptionUpdated(message.data)
                if (message.data.option == "_liquid_keyboard") {
                    controller.onCommand(ImeCommand.OpenPanel(PanelId.Symbols))
                }
            }

            is RimeMessage.CompositionMessage -> broadcaster.onCompositionUpdate(message.data)

            is RimeMessage.BulkCandidatesMessage -> broadcaster.onCandidateListUpdate(message.data)

            else -> Unit
        }
        broadcastKeyAppearanceUpdate()
    }

    private fun broadcastKeyAppearanceUpdate() {
        val composing = rime.run { statusCached.isComposing }
        val hasMenu = rime.run { hasMenu }
        val paging = rime.run { paging }
        broadcaster.onKeyAppearanceUpdate(composing, hasMenu, paging)
    }

    // -- service lifecycle -------------------------------------------------
    fun startInput(
        info: EditorInfo,
        restarting: Boolean = false,
    ) {
        broadcaster.onStartInput(info)
        if (!restarting) controller.onCommand(ImeCommand.ClosePanel)
    }

    fun updateSelection(
        start: Int,
        end: Int,
    ) {
        broadcaster.onSelectionUpdate(start, end)
    }

    fun trimMemory() = Unit

    fun refreshColors() {
        colorsRevision++
    }

    /** The visible band's top edge in IME-window px (see [ImeSurfaceHost]). */
    val bandTopY: Int
        get() = bandTop

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        requestApplyInsets()
    }

    override fun onDetachedFromWindow() {
        keyboardPrefs.rowHeight.sharedPreferences.unregisterOnSharedPreferenceChangeListener(prefListener)
        handleMessages = false
        controller.detach()
        super.onDetachedFromWindow()
    }
}

/**
 * A dedicated mini lifecycle for the IME's composition. ComposeView requires a
 * saved-state-registry owner whose lifecycle is still in its initialization
 * stage when the registry controller attaches, which a live service can
 * never offer; this owner starts fresh, restores from nothing, and rides to
 * resumed once, staying alive across window re-attacks.
 */
private class ComposeLifecycleOwner :
    LifecycleOwner,
    SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = registry

    override val savedStateRegistry
        get() = controller.savedStateRegistry

    fun performRestore() {
        controller.performAttach()
        controller.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
}
