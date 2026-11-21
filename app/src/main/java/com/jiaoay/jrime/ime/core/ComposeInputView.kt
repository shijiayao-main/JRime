/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.core

import android.content.SharedPreferences
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.jiaoay.jrime.ime.clipboard.ClipboardWindow
import com.jiaoay.jrime.ime.compose.ClipboardChip
import com.jiaoay.jrime.ime.compose.ImeColors
import com.jiaoay.jrime.ime.compose.ImeCommandExecutor
import com.jiaoay.jrime.ime.compose.ImeSurfaceController
import com.jiaoay.jrime.ime.compose.LiquidPanel
import com.jiaoay.jrime.ime.compose.LocalImeColors
import com.jiaoay.jrime.ime.compose.toEngineAccent
import com.jiaoay.jrime.ime.compose.toEnginePaletteMode
import com.jiaoay.jrime.ime.keyboard.CommonKeyboardActionListener
import com.jiaoay.jrime.ime.keyboard.InputFeedbackManager
import com.jiaoay.jrime.ime.keyboard.KeyAction
import com.jiaoay.jrime.ime.keyboard.KeyboardWindow
import com.jiaoay.jrime.ime.phrase.CustomPhraseStore
import com.jiaoay.jrime.ime.phrase.PhrasePanel
import com.jiaoay.jrime.ime.phrase.PhraseWindow
import com.jiaoay.jrime.ime.symbol.LiquidWindow
import com.jiaoay.jrime.ime.window.BoardWindow
import com.jiaoay.jrime.ime.window.BoardWindowManager
import com.jiaoay.jrime.ui.design.ImeIcon
import com.jiaoay.jrime.ui.ime.ImeSurface
import com.jiaoay.jrime.ui.ime.ImeSurfaceConfig
import com.jiaoay.jrime.ui.ime.ImeSurfaceHost
import com.jiaoay.jrime.ui.ime.PanelSlot
import com.jiaoay.jrime.ui.ime.ToolbarAction
import kotlinx.coroutines.launch
import org.kodein.di.instance
import timber.log.Timber

/**
 * The M1 Compose host: the same [InputView] machinery (DI, broadcaster, board
 * windows) feeds the new shared surface instead of the legacy [ImeRoot] tree.
 *
 * The surface itself lives in `:ui:ime` and knows nothing about Android; this
 * class supplies the platform seam: the command executor (the proven
 * [KeyAction] pipeline), the panel slots (clipboard/liquid still on the legacy
 * state machines during M1), the clipboard chip and the prefs-derived config.
 */
class ComposeInputView(
    service: TrimeInputMethodService,
    rime: RimeSession,
    scope: ThemeScope,
) : InputView(service, rime, scope) {
    private val broadcaster: InputBroadcaster by instance()
    private val actionListener: CommonKeyboardActionListener by instance()
    private val keyboardWindow: KeyboardWindow by instance()
    private val windowManager: BoardWindowManager by instance()
    private val liquidWindow: LiquidWindow by instance()
    private val phraseWindow: PhraseWindow by lazy { PhraseWindow(di) }

    private val catalog = KeyboardCatalog.shipped()
    private val controller = ImeSurfaceController(broadcaster, rime, service.lifecycleScope, keyboardWindow)
    private val executor =
        ImeCommandExecutor(
            service = service,
            rime = rime,
            actionListener = actionListener,
            windowManager = windowManager,
            keyboardWindow = keyboardWindow,
            liquidWindow = liquidWindow,
            phraseWindow = phraseWindow,
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

    private val host =
        object : ImeSurfaceHost {
            override fun dispatch(command: ImeCommand) {
                // UI-local state first (paging/panels swap instantly), then the
                // engine/platform effect.
                controller.onCommand(command)
                executor.execute(command)
            }

            override fun reportBandTop(topPx: Int) {
                if (topPx >= 0 && topPx != bandTop) {
                    bandTop = topPx
                    // The band top is the truth the service's inset
                    // computation rides on; re-apply insets per move.
                    post { requestApplyInsets() }
                }
            }

            override fun onKeyFeedback(
                binding: KeyBinding,
                gesture: KeyGesture,
            ) {
                val action = KeyAction(binding.token, catalog.presetKeys)
                InputFeedbackManager.keyPressSound(action.code)
                InputFeedbackManager.keyPressSpeak(action.code)
                when (gesture) {
                    KeyGesture.Tap -> InputFeedbackManager.keyPressVibrate(this@ComposeInputView)

                    // Haptic marks the long-press/cursor-slide mode entry.
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

                AppPrefs.General.ASCII_SWITCH_MODE ->
                    asciiSwitchMode = generalPrefs.asciiSwitchMode.getValue()

                AppPrefs.Appearance.CANDIDATE_PAGE_SIZE ->
                    controller.onCommand(
                        com.jiaoay.jrime.engine.ImeCommand.SetCandidatePageSize(
                            appearancePrefs.candidatePageSize.getValue(),
                        ),
                    )

                AppPrefs.Appearance.PALETTE_MODE ->
                    controller.onCommand(
                        com.jiaoay.jrime.engine.ImeCommand.SetPaletteMode(
                            appearancePrefs.paletteMode.getValue().toEnginePaletteMode(),
                        ),
                    )

                AppPrefs.Appearance.ACCENT ->
                    controller.onCommand(
                        com.jiaoay.jrime.engine.ImeCommand.SetAccent(
                            appearancePrefs.accent.getValue().toEngineAccent(),
                        ),
                    )
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
                // ComposeView requires lifecycle and saved-state owners in the
                // view tree; LifecycleInputMethodService provides the former,
                // and this stateless composition gets a never-restored registry.
                val owner = ComposeLifecycleOwner()
                owner.performRestore()
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    val state by controller.state.collectAsState()
                    val window by controller.window.collectAsState()
                    // Reading the revision re-creates the band after a scheme
                    // flip, which re-reads the system night mode inside.
                    key(colorsRevision) {
                        ImeSurface(
                            state = state,
                            config = surfaceConfig(state, window),
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

    private fun surfaceConfig(
        state: ImeSessionState,
        window: BoardWindow?,
    ): ImeSurfaceConfig = ImeSurfaceConfig(
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
        panelSlots = panelSlots(window),
        clipboardChip = {
            legacyImeColors {
                ClipboardChip(
                    onDispatch = { action -> executor.executeAction(action) },
                    panelOpen = window != null,
                )
            }
        },
    )

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
        // Engine-option toggles (the old surface only reached these through
        // /so or a hardware shortcut).
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

    private fun panelSlots(window: BoardWindow?): List<PanelSlot> = listOf(
        PanelSlot(
            id = PanelId.Clipboard,
            title = "剪贴板",
            icon = ImeIcon.Clipboard,
        ) { modifier ->
            (window as? ClipboardWindow)?.let {
                legacyImeColors { ClipboardPanel(it, modifier) }
            }
        },
        PanelSlot(
            id = PanelId.Phrase,
            title = "常用语",
            icon = ImeIcon.Phrase,
        ) { modifier ->
            PhrasePanel(modifier = modifier) { text ->
                controller.onCommand(ImeCommand.ClosePanel)
                executor.execute(ImeCommand.CommitText(text))
            }
        },
        PanelSlot(
            id = PanelId.Symbols,
            title = "符号",
            icon = ImeIcon.Emoji,
        ) { modifier ->
            (window as? LiquidWindow)?.let {
                legacyImeColors { LiquidPanel(it, modifier) }
            }
        },
    )

    /**
     * The M1 panel surfaces still read the legacy [LocalImeColors]; this
     * provider keeps them rendering from the same palette the old tree used.
     */
    @androidx.compose.runtime.Composable
    private fun legacyImeColors(content: @androidx.compose.runtime.Composable () -> Unit) {
        val colors = ColorManager.currentScope()?.let { ImeColors(it.colors) }
        if (colors != null) {
            androidx.compose.runtime.CompositionLocalProvider(LocalImeColors provides colors, content = content)
        }
    }

    /**
     * A scheme flip (day/night) restyles the legacy tree in place; here it
     * re-creates the shared band so the palette is re-read.
     */
    override fun refreshColors() {
        super.refreshColors()
        colorsRevision++
    }

    /** The visible band's top edge in IME-window px (see [ImeSurfaceHost]). */
    val bandTopY: Int
        get() = bandTop

    override fun onDetachedFromWindow() {
        keyboardPrefs.rowHeight.sharedPreferences.unregisterOnSharedPreferenceChangeListener(prefListener)
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
