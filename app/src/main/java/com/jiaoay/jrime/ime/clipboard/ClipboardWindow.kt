/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.clipboard

import android.content.Intent
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.jiaoay.jrime.data.clip.ClipboardHelper
import com.jiaoay.jrime.data.clip.FavoriteHelper
import com.jiaoay.jrime.data.prefs.AppPrefs
import com.jiaoay.jrime.ime.core.TrimeInputMethodService
import com.jiaoay.jrime.ime.keyboard.KeyboardWindow
import com.jiaoay.jrime.ime.window.BoardWindow
import com.jiaoay.jrime.ime.window.BoardWindowManager
import com.jiaoay.jrime.ui.main.ClipEditActivity
import com.jiaoay.jrime.util.AppUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance

/**
 * The clipboard panel's state machine: the two live lists (history and
 * favorites) as Compose state plus one method per user action. Rendering
 * lives in the Compose [ClipboardPanel]; this window only carries data and
 * behavior, the same split [com.jiaoay.jrime.ime.symbol.LiquidWindow] rides.
 */
class ClipboardWindow(
    di: DI,
    initialTab: Int = 0,
) : BoardWindow.BarBoardWindow(di) {
    override val showTitle = false

    private val service: TrimeInputMethodService by instance()
    private val windowManager: BoardWindowManager by instance()

    private val prefs = AppPrefs.defaultInstance().clipboard
    private val clipboardReturnAfterPaste by prefs.clipboardReturnAfterPaste

    /** The two tabs: 0 = clipboard history, 1 = favorites. */
    var currentTab by mutableStateOf(initialTab.coerceIn(0, TAB_COUNT - 1))
        private set
    var clippings by mutableStateOf(emptyList<ClipboardItem>())
        private set
    var favorites by mutableStateOf(emptyList<ClipboardItem>())
        private set

    private var collectJob: Job? = null

    fun selectTab(tab: Int) {
        currentTab = tab.coerceIn(0, TAB_COUNT - 1)
    }

    /** Pastes the text and returns to the keyboard when the preference asks. */
    fun paste(item: ClipboardItem) {
        service.commitText(item.text)
        if (clipboardReturnAfterPaste) {
            windowManager.attachWindow(KeyboardWindow)
        }
    }

    fun pin(item: ClipboardItem) {
        service.lifecycleScope.launch { ClipboardHelper.pin(item.id) }
    }

    fun unpin(item: ClipboardItem) {
        service.lifecycleScope.launch { ClipboardHelper.unpin(item.id) }
    }

    /** Pins a history item into the favorites table. */
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
        service.startActivity(Intent.createChooser(target, null).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    /** Clears the current tab; pinned history entries stay. */
    fun clearCurrentTab() {
        service.lifecycleScope.launch {
            when (currentTab) {
                0 -> ClipboardHelper.deleteAll(ClipboardHelper.haveUnpinned())
                else -> FavoriteHelper.deleteAll()
            }
        }
    }

    override fun onCreateView(): View = View(context)

    override fun onAttached() {
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

    override fun onDetached() {
        collectJob?.cancel()
        collectJob = null
    }

    companion object {
        private const val TAB_COUNT = 2
    }
}
