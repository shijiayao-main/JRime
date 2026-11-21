/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.ime

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jiaoay.jrime.engine.ImeCommand
import com.jiaoay.jrime.engine.ImeMetrics
import com.jiaoay.jrime.engine.KeyBinding
import com.jiaoay.jrime.engine.KeyGesture
import com.jiaoay.jrime.engine.PanelId

/**
 * The platform seam of the IME surface: everything the shared UI needs to ask
 * the app to do. Implemented by the Android host (and later by the desktop
 * host); the shared surface never touches the engine or the platform directly.
 */
interface ImeSurfaceHost {
    /** Executes a UI intent. */
    fun dispatch(command: ImeCommand)

    /** Reports the visible band's top offset in window pixels (IME insets). */
    fun reportBandTop(topPx: Int)

    /** Key feedback (sound/haptic/TTS); the surface calls it at finger-down. */
    fun onKeyFeedback(binding: KeyBinding, gesture: KeyGesture)

    /** The composing-code text the host wants shown in the top bar (may be empty). */
    fun compositionEcho(): String
}

/**
 * App-provided panel content. The host supplies the composable; the shared
 * surface owns the host frame, animation and height, so adding a panel never
 * changes the surface code.
 */
data class PanelSlot(
    val id: PanelId,
    val title: String,
    val icon: String,
    val content: @Composable (Modifier) -> Unit,
)

/** One top-bar button. */
data class ToolbarAction(
    val id: String,
    val label: String,
    val command: ImeCommand,
    val binding: KeyBinding? = null,
    val highlighted: Boolean = false,
    val visible: Boolean = true,
)

/** Everything the surface needs that is not in the session state. */
data class ImeSurfaceConfig(
    val rowHeightDp: Int = ImeMetrics.ROW_HEIGHT_DEFAULT_DP,
    val popupOnKeyPress: Boolean = true,
    val candidateTextSizeSp: Float = 18f,
    val showCandidateComment: Boolean = true,
    val enterLabel: String = "",
    val toolbarActions: List<ToolbarAction> = emptyList(),
    val panelSlots: List<PanelSlot> = emptyList(),
    /** App-provided quick-paste chip rendered in the idle top bar. */
    val clipboardChip: (@Composable () -> Unit)? = null,
)

/** The Gboard-structured keyboard's fixed row count (digit + 2 letters + shift + chrome). */
const val GBOARD_CONTENT_ROWS: Int = 5
