/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.main.settings

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.jiaoay.jrime.R
import com.jiaoay.jrime.data.prefs.AppPrefs
import com.jiaoay.jrime.data.theme.ImeFonts
import com.jiaoay.jrime.ui.settings.SettingsScreen
import com.jiaoay.jrime.ui.settings.SettingsStrings
import com.jiaoay.jrime.util.toast
import kotlinx.coroutines.launch

/**
 * The settings hub host: a Fragment only because the app's navigation is
 * fragment-based; the screen itself is the Compose `SettingsScreen` fed by
 * [AppSettingsStore] + [SettingsSpecsFactory].
 */
class SettingsFragment : Fragment() {
    private var store: AppSettingsStore? = null

    private val importFont =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) importFont(uri)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val appStore = AppSettingsStore(PreferenceManager.getDefaultSharedPreferences(requireContext()))
        store = appStore
        val baseGroups = SettingsSpecsFactory.forContext(requireContext()).build()
        val inputTitle = baseGroups.firstOrNull { it.id == "input" }?.title.orEmpty()
        val ruleRows = InputRuleSpecs.build(labeler = { getString(it) }, groupTitle = inputTitle)
        val groups =
            baseGroups.map { group ->
                if (group.id == "input") {
                    group.copy(specs = ruleRows + group.specs)
                } else {
                    group
                }
            }
        val strings =
            SettingsStrings(
                title = getString(R.string.settings),
                searchPlaceholder = getString(R.string.search_settings),
                empty = getString(R.string.no_matching_settings),
                cancel = getString(R.string.cancel),
                confirm = getString(R.string.ok),
            )
        return ComposeView(requireContext()).apply {
            setContent {
                // The hub wears the app's brand blue (the keyboard's accent
                // default) instead of Material's baseline purple.
                val dark = isSystemInDarkTheme()
                MaterialTheme(
                    colorScheme =
                    if (dark) {
                        darkColorScheme(primary = Color(0xFF8AB4F8))
                    } else {
                        lightColorScheme(primary = Color(0xFF1A73E8))
                    },
                ) {
                    SettingsScreen(
                        groups = groups,
                        store = appStore,
                        strings = strings,
                        onAction = ::handleAction,
                        options = RimeRuntimeOptionSource(),
                    )
                }
            }
        }
    }

    private fun handleAction(id: String) {
        when (id) {
            SettingsSpecsFactory.ACTION_IMPORT_FONT ->
                importFont.launch(arrayOf("font/*", "application/octet-stream"))

            SettingsSpecsFactory.ACTION_PICK_SOUND -> {
                val ctx = context ?: return
                lifecycleScope.launch {
                    SoundEffectPickerDialog.build(lifecycleScope, ctx).show()
                }
            }
        }
    }

    private fun importFont(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            ImeFonts
                .import(requireContext(), uri)
                .onSuccess {
                    requireContext().toast(R.string.import_font_done)
                }.onFailure {
                    requireContext().toast(getString(R.string.import_font_failed, it.message))
                }
        }
    }

    override fun onStop() {
        AppPrefs.defaultInstance().syncToDeviceEncryptedStorage()
        super.onStop()
    }

    override fun onDestroyView() {
        store?.dispose()
        store = null
        super.onDestroyView()
    }
}
