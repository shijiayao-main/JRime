/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jiaoay.jrime.core.CandidateProto
import com.jiaoay.jrime.core.Candidates
import com.jiaoay.jrime.core.CompositionProto
import com.jiaoay.jrime.core.RimeMessage
import com.jiaoay.jrime.daemon.RimeSession
import com.jiaoay.jrime.daemon.launchOnReady
import com.jiaoay.jrime.ime.broadcast.InputBroadcastReceiver
import com.jiaoay.jrime.ime.broadcast.InputBroadcaster
import com.jiaoay.jrime.ime.window.BoardWindow
import kotlinx.coroutines.CoroutineScope

/**
 * The Compose UI's single window into the IME state stream. Registered as one
 * more [InputBroadcastReceiver] on the shared broadcaster -- the same
 * observer pattern the legacy views ride on; the composables read only this.
 *
 * Candidate texts arrive as metadata in the bulk message, so the texts are
 * fetched on the session scope and exposed as plain state.
 */
class ImeUiState(
    private val broadcaster: InputBroadcaster,
    private val rime: RimeSession,
    private val scope: CoroutineScope,
) : InputBroadcastReceiver {
    var composition by mutableStateOf(CompositionProto())
        private set
    var candidates by mutableStateOf<List<CandidateProto>>(emptyList())
        private set
    var candidatesCount by mutableStateOf(0)
        private set
    var highlightedCandidate by mutableStateOf(0)
        private set
    var composing by mutableStateOf(false)
        private set
    var hasMenu by mutableStateOf(false)
        private set
    var paging by mutableStateOf(false)
        private set

    /** The window the board manager last attached; drives the panel switch. */
    var currentWindow by mutableStateOf<BoardWindow?>(null)
        private set

    /**
     * The two toggles the action bar surfaces as dual-state buttons: the CJK/EN
     * switch and the full/half-width punctuation switch. Kept as Compose state
     * so the labels flip the moment librime reports the option change.
     */
    var isAsciiMode by mutableStateOf(false)
        private set
    var isAsciiPunct by mutableStateOf(false)
        private set

    /**
     * Bumped whenever the color scheme flips (day/night): the action bar and
     * key faces read the palette through it, so a bump re-renders the whole
     * band in the new scheme.
     */
    var colorsRevision by mutableStateOf(0)
        private set

    fun bumpColors() {
        colorsRevision++
    }

    /**
     * The band's top edge in IME-window coordinates, reported by the
     * composition on layout -- the truth the service's inset computation
     * rides on (the legacy measurement over-reports the band height).
     */
    var bandTopY by mutableStateOf(Int.MIN_VALUE)
        private set

    fun updateBandTop(y: Int) {
        if (y >= 0 && y != bandTopY) {
            bandTopY = y
            onBandMoved?.invoke()
        }
    }

    /** Fired when the band top moves (smartbar showing/hiding reflows it). */
    var onBandMoved: (() -> Unit)? = null

    fun attach() {
        broadcaster.addReceiver(this)
        // The option stream only carries changes; seed the two toggles with
        // the session's current values so the first frame shows the truth.
        rime.launchOnReady { api ->
            isAsciiMode = api.getRuntimeOption("ascii_mode")
            isAsciiPunct = api.getRuntimeOption("ascii_punct")
        }
    }

    fun detach() {
        broadcaster.removeReceiver(this)
    }

    fun selectCandidate(index: Int) {
        rime.launchOnReady { it.selectCandidate(index, global = true) }
    }

    fun changeCandidatePage(backward: Boolean) {
        rime.launchOnReady { it.changeCandidatePage(backward) }
    }

    override fun onCompositionUpdate(data: CompositionProto) {
        composition = data
    }

    override fun onCandidateListUpdate(data: Candidates.Bulk) {
        candidatesCount = data.candidates.size
        highlightedCandidate = data.highlighted
        candidates = data.candidates.toList()
    }

    override fun onKeyAppearanceUpdate(composing: Boolean, menu: Boolean, paging: Boolean) {
        this.composing = composing
        this.hasMenu = menu
        this.paging = paging
    }

    override fun onWindowAttached(window: BoardWindow) {
        currentWindow = window
    }

    override fun onRimeOptionUpdated(value: RimeMessage.OptionMessage.Data) {
        when (value.option) {
            "ascii_mode" -> isAsciiMode = value.value
            "ascii_punct" -> isAsciiPunct = value.value
        }
    }
}
