/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose.panels

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.jiaoay.jrime.data.clip.ClipboardHelper
import com.jiaoay.jrime.data.clip.FavoriteHelper
import com.jiaoay.jrime.data.prefs.AppPrefs
import com.jiaoay.jrime.ime.clipboard.ClipboardItem
import com.jiaoay.jrime.ime.clipboard.toClipboardItem
import com.jiaoay.jrime.ime.core.TrimeInputMethodService
import com.jiaoay.jrime.ui.main.ClipEditActivity
import com.jiaoay.jrime.util.AppUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Compose-native clipboard panel state (P3): the two live lists plus the user
 * actions. The legacy `ClipboardWindow`/`BoardWindow` host is gone from this
 * path — the surface collects the flows while the panel is composed.
 */
class ClipboardPanelState(
    private val service: TrimeInputMethodService,
    private val onClosePanel: () -> Unit,
    initialTab: Int = 0,
) {
    private val prefs = AppPrefs.defaultInstance().clipboard
    private val returnAfterPaste by prefs.clipboardReturnAfterPaste

    var currentTab by mutableStateOf(initialTab.coerceIn(0, TAB_COUNT - 1))
        private set
    var clippings by mutableStateOf(emptyList<ClipboardItem>())
        private set
    var favorites by mutableStateOf(emptyList<ClipboardItem>())
        private set

    private var collectJob: Job? = null

    fun start() {
        if (collectJob != null) return
        collectJob =
            service.lifecycleScope.launch {
                launch {
                    ClipboardHelper.observeAll().collect { list ->
                        clippings = list.map { it.toClipboardItem() }
                    }
                }
                launch {
                    FavoriteHelper.observeAll().collect { list ->
                        favorites = list.map { it.toClipboardItem() }
                    }
                }
            }
    }

    fun stop() {
        collectJob?.cancel()
        collectJob = null
    }

    fun selectTab(tab: Int) {
        currentTab = tab.coerceIn(0, TAB_COUNT - 1)
    }

    fun paste(item: ClipboardItem) {
        service.commitText(item.text)
        if (returnAfterPaste) onClosePanel()
    }

    fun pin(item: ClipboardItem) {
        service.lifecycleScope.launch { ClipboardHelper.pin(item.id) }
    }

    fun unpin(item: ClipboardItem) {
        service.lifecycleScope.launch { ClipboardHelper.unpin(item.id) }
    }

    fun favorite(item: ClipboardItem) {
        service.lifecycleScope.launch { FavoriteHelper.addNewEntity(item.text) }
    }

    fun deleteClipping(item: ClipboardItem) {
        service.lifecycleScope.launch { ClipboardHelper.delete(item.id) }
    }

    fun deleteFavorite(item: ClipboardItem) {
        service.lifecycleScope.launch { FavoriteHelper.delete(item.id) }
    }

    fun editClipping(item: ClipboardItem) {
        AppUtils.launchClipEdit(service, item.id, ClipEditActivity.FROM_CLIPBOARD)
    }

    fun editFavorite(item: ClipboardItem) {
        AppUtils.launchClipEdit(service, item.id, ClipEditActivity.FROM_COLLECTION)
    }

    fun share(item: ClipboardItem) {
        val target =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, item.text)
            }
        service.startActivity(
            Intent.createChooser(target, null).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
        )
    }

    fun clearCurrentTab() {
        service.lifecycleScope.launch {
            when (currentTab) {
                0 -> ClipboardHelper.deleteAll(ClipboardHelper.haveUnpinned())
                else -> FavoriteHelper.deleteAll()
            }
        }
    }

    companion object {
        private const val TAB_COUNT = 2
    }
}
