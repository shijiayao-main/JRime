/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.main.settings

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.jiaoay.jrime.R
import com.jiaoay.jrime.data.prefs.AppPrefs
import com.jiaoay.jrime.data.prefs.PreferenceDelegateFragment
import com.jiaoay.jrime.data.theme.ImeFonts
import com.jiaoay.jrime.util.toast
import kotlinx.coroutines.launch

class GeneralSettingsFragment : PreferenceDelegateFragment(AppPrefs.defaultInstance().general) {
    private val importFont =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) importFont(uri)
        }

    /**
     * The rare-glyph rescue: fonts the user imports ride behind the system
     * face, so rare CJK ideographs stop rendering as tofu in the candidate
     * strip. The APK ships no font -- the user picks the file.
     */
    override fun onPreferenceUiCreated(screen: PreferenceScreen) {
        val preference =
            Preference(preferenceManager.context).apply {
                key = KEY_IMPORT_FONT
                setTitle(R.string.import_font_title)
                summary = fontSummary()
                setOnPreferenceClickListener {
                    // Font files often carry no registered MIME on devices;
                    // octet-stream keeps the picker from hiding them.
                    importFont.launch(arrayOf("font/*", "application/octet-stream"))
                    true
                }
            }
        screen.addPreference(preference)
    }

    override fun onResume() {
        super.onResume()
        // A fresh import changes the summary; refresh when coming back.
        findPreference<Preference>(KEY_IMPORT_FONT)?.summary = fontSummary()
    }

    private fun importFont(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            ImeFonts
                .import(requireContext(), uri)
                .onSuccess {
                    requireContext().toast(R.string.import_font_done)
                    findPreference<Preference>(KEY_IMPORT_FONT)?.summary = fontSummary()
                }.onFailure {
                    requireContext().toast(getString(R.string.import_font_failed, it.message))
                }
        }
    }

    private fun fontSummary(): String {
        val names = ImeFonts.fontFiles().map { it.name }
        return if (names.isEmpty()) {
            getString(R.string.import_font_summary_empty)
        } else {
            getString(R.string.import_font_summary_named, names.joinToString(", "))
        }
    }

    companion object {
        private const val KEY_IMPORT_FONT = "import_font"
    }
}
