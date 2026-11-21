/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiaoay.jrime.data.clip.ClipboardHelper
import com.jiaoay.jrime.data.prefs.AppPrefs
import com.jiaoay.jrime.ime.keyboard.InputFeedbackManager
import kotlinx.coroutines.delay

/** How many characters of the clip the chip previews before the ellipsis. */
internal const val CLIPBOARD_CHIP_PREVIEW_CHARS = 24

/** The chip's width ceiling: room for a real preview, never the whole bar. */
private val ClipboardChipMaxWidth = 200.dp

/**
 * The chip's one-line preview: past [limit] characters it truncates with an
 * ellipsis, otherwise verbatim. Display only -- a tap pastes the full text.
 */
internal fun clipboardChipPreview(
    text: String,
    limit: Int = CLIPBOARD_CHIP_PREVIEW_CHARS,
): String = if (text.length > limit) text.take(limit) + "…" else text

/**
 * The chip's visibility rule: the `clipboard_suggestion` switch gates it, a
 * clip must exist, sensitive clips stay out (the conservative privacy
 * default -- the panel keeps its own `clipboard_mask_sensitive` masking),
 * and a nonzero timeout expires the clip once [elapsedMillis] passes the
 * window; timeout 0 never expires (the preference pair had no other reader,
 * so no legacy semantics to honor).
 */
internal fun clipboardChipVisible(
    suggestionEnabled: Boolean,
    hasEntity: Boolean,
    sensitive: Boolean,
    timeoutSeconds: Int,
    elapsedMillis: Long,
): Boolean = suggestionEnabled &&
    hasEntity &&
    !sensitive &&
    (timeoutSeconds == 0 || elapsedMillis <= timeoutSeconds * 1000L)

/**
 * The clipboard quick-paste chip: the idle action bar's left cluster shows
 * the latest clip as a preview chip -- one tap commits the full text inline
 * (the same dispatch path as the keys), then the chip hides (consumed on
 * paste; it comes back with the next clip).
 *
 * The data is pushed by [ClipboardHelper] -- the event-driven pattern the
 * Android 10+ background-clip limits dictate; the IME never reads
 * primaryClip on demand, and a fresh process offers nothing until the next
 * copy lands. The suggestion/timeout preferences ride one shared listener
 * (the [GboardKeyboard] template). The chip hides itself by composing to
 * nothing, so the action bar simply always includes it; while a full-data
 * panel owns the idle row it yields ([panelOpen]) -- the panel keeps its
 * own clipboard entrance.
 */
@Suppress("FunctionName")
@Composable
internal fun ClipboardChip(
    onCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    panelOpen: Boolean = false,
) {
    val clipboardPrefs = remember { AppPrefs.defaultInstance().clipboard }
    var suggestionEnabled by remember { mutableStateOf(clipboardPrefs.clipboardSuggestion.getValue()) }
    var timeoutSeconds by remember { mutableIntStateOf(clipboardPrefs.clipboardSuggestionTimeout.getValue()) }
    DisposableEffect(clipboardPrefs) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                when (key) {
                    AppPrefs.Clipboard.CLIPBOARD_SUGGESTION ->
                        suggestionEnabled = clipboardPrefs.clipboardSuggestion.getValue()

                    AppPrefs.Clipboard.CLIPBOARD_SUGGESTION_TIMEOUT ->
                        timeoutSeconds = clipboardPrefs.clipboardSuggestionTimeout.getValue()
                }
            }
        clipboardPrefs.clipboardSuggestion.sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            clipboardPrefs.clipboardSuggestion.sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    var entity by remember { mutableStateOf(ClipboardHelper.lastEntity) }
    DisposableEffect(Unit) {
        val listener = ClipboardHelper.OnClipboardUpdateListener { entity = it }
        ClipboardHelper.addOnUpdateListener(listener)
        onDispose { ClipboardHelper.removeOnUpdateListener(listener) }
    }
    // Consume-on-paste: a fresh clip re-arms the chip; the hide lives for
    // the idle session -- a re-idled bar offers a still-fresh clip again.
    var consumed by remember { mutableStateOf(false) }
    LaunchedEffect(entity?.timestamp) { consumed = false }
    // The expiry clock: with a nonzero timeout, one delayed tick past the
    // window refreshes "now" and the pure rule hides the chip; timeout 0
    // schedules nothing.
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(entity?.timestamp, timeoutSeconds) {
        nowMillis = System.currentTimeMillis()
        val timestamp = entity?.timestamp
        if (timestamp != null && timeoutSeconds > 0) {
            val remaining = timestamp + timeoutSeconds * 1000L - System.currentTimeMillis()
            if (remaining > 0) delay(remaining)
            nowMillis = System.currentTimeMillis()
        }
    }
    val visible =
        !panelOpen &&
            !consumed &&
            clipboardChipVisible(
                suggestionEnabled = suggestionEnabled,
                hasEntity = entity != null,
                sensitive = entity?.sensitive ?: false,
                timeoutSeconds = timeoutSeconds,
                elapsedMillis = entity?.let { nowMillis - it.timestamp } ?: 0L,
            )
    if (!visible) return
    val colors = LocalImeColors.current
    Box(
        modifier = modifier
            .padding(horizontal = 3.dp)
            .widthIn(min = 36.dp, max = ClipboardChipMaxWidth)
            .height(32.dp)
            .background(colors.functionKey, RoundedCornerShape(6.dp))
            .clickable {
                val clip = entity ?: return@clickable
                // The action bar's feedback contract minus the keycode speech:
                // the inline commit path already speaks the pasted text
                // (textCommitSpeak) -- keyPressSpeak(0) would say "unknown".
                InputFeedbackManager.run { keyPressSound() }
                consumed = true
                onCommit(clip.text)
            },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = clipboardChipPreview(entity?.text.orEmpty()),
            style = TextStyle(fontSize = 14.sp, color = colors.keySymbol),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}
