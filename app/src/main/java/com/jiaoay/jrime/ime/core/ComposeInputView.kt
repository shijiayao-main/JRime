/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.core

import android.view.View
import android.view.ViewGroup
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
import com.jiaoay.jrime.data.theme.ThemeScope
import com.jiaoay.jrime.ime.broadcast.InputBroadcaster
import com.jiaoay.jrime.ime.compose.ImeRoot
import com.jiaoay.jrime.ime.compose.ImeUiState
import com.jiaoay.jrime.ime.keyboard.CommonKeyboardActionListener
import com.jiaoay.jrime.ime.keyboard.KeyAction
import com.jiaoay.jrime.ime.keyboard.KeyboardWindow
import kotlinx.coroutines.launch
import org.kodein.di.DIAware
import org.kodein.di.instance
import timber.log.Timber

/**
 * The stage-1 Compose input view: the full [InputView] machinery -- DI graph,
 * broadcaster wiring, the board windows as state machines -- keeps running,
 * but the legacy tree is hidden and a single [ComposeView] renders
 * [ImeRoot] instead. Window views keep attaching into the hidden
 * `input_window` frame, so switching and state broadcasts flow unchanged;
 * only the pixels come from Compose.
 *
 * The layouts render from NativeTheme's hardcoded keyboards (verified
 * field-for-field against the YAML model in task 1.2), so the swap needs no
 * change to the legacy theme plumbing yet -- the source flip is task 1.4.
 */
class ComposeInputView(
    service: TrimeInputMethodService,
    rime: RimeSession,
    scope: ThemeScope,
) : InputView(service, rime, scope) {
    private val broadcaster: InputBroadcaster by instance()
    private val actionListener: CommonKeyboardActionListener by instance()
    private val keyboardWindow: KeyboardWindow by instance()

    private val imeState: ImeUiState
    private val compose: ComposeView

    init {
        Timber.d("ComposeInputView init")
        imeState = ImeUiState(broadcaster, rime, service.lifecycleScope).also { it.attach() }
        // The band reflows when the smartbar shows or hides; the service's
        // inset computation rides the band top, so re-apply insets per move.
        imeState.onBandMoved = { post { requestApplyInsets() } }
        post {
            Timber.d("ComposeInputView sized: self=${width}x$height compose=${compose.width}x${compose.height}")
        }
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
                    ImeRoot(
                        state = imeState,
                        keyboardWindow = keyboardWindow,
                        onDispatch = ::dispatch,
                    )
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

    @Deprecated("Deprecated in Java")
    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        super.onLayout(changed, left, top, right, bottom)
        Timber.d("ComposeInputView layout: self=$left,$top,$right,$bottom compose=${compose.left},${compose.top},${compose.right},${compose.bottom}")
    }

    /**
     * A scheme flip (day/night) restyles the legacy tree in place; here it
     * bumps the palette revision so the next composition reads the new table.
     */
    override fun refreshColors() {
        super.refreshColors()
        imeState.bumpColors()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Timber.d("ComposeInputView attached; compose attached=${compose.isAttachedToWindow}")
    }

    @Deprecated("Deprecated in Java")
    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Timber.d("ComposeInputView measured: ${MeasureSpec.getSize(widthMeasureSpec)}x${MeasureSpec.getSize(heightMeasureSpec)} -> ${measuredWidth}x$measuredHeight")
    }

    private fun dispatch(action: KeyAction) {
        // The press feedback fires in the gesture handler at finger-down; a
        // second onPress here would double-fire every keyboard tap.
        actionListener.listener.onAction(action)
    }

    /**
     * The Compose band's top edge in IME-window px (MIN until first layout):
     * the real height the service should report to apps. The legacy measured
     * height over-reports (the old YAML renderer's geometry), which left a
     * transparent gap the apps showed above the band.
     */
    val bandTopY: Int
        get() = imeState.bandTopY

    override fun onDetachedFromWindow() {
        imeState.detach()
        super.onDetachedFromWindow()
    }
}

/**
 * A dedicated mini lifecycle for the IME's composition. ComposeView requires a
 * saved-state-registry owner whose lifecycle is still in its initialization
 * stage when the registry controller attaches, which a live service can
 * never offer; this owner starts fresh, restores from nothing, and rides to
 * resumed once, staying alive across window re-attacks (the composition
 * persists between input fields, which the input-latency budget wants).
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
