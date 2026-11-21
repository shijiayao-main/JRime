/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.jiaoay.jrime.R
import com.jiaoay.jrime.ui.common.PaddingPreferenceFragment
import com.jiaoay.jrime.util.Const
import com.jiaoay.jrime.util.addCategory
import com.jiaoay.jrime.util.addPreference
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.License
import kotlinx.coroutines.launch

class LicenseFragment : PaddingPreferenceFragment() {
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        lifecycleScope.launch {
            val context = preferenceManager.context
            val jsonString = resources.openRawResource(R.raw.aboutlibraries)
                .bufferedReader()
                .use { it.readText() }
            val libraries =
                Libs
                    .Builder()
                    .withJson(jsonString)
                    .build()
                    .libraries
            val native = libraries.filter { it.tag == "native" }.sortedBy { it.uniqueId.lowercase() }
            val others = libraries.filter { it.tag != "native" }.sortedBy { it.uniqueId.lowercase() }
            preferenceScreen = preferenceManager.createPreferenceScreen(requireContext()).apply {
                addPreference(
                    getString(R.string.licenses_intro_title),
                    getString(R.string.licenses_intro_summary),
                )
                addPreference(
                    getString(R.string.upstream_credits_title),
                    getString(R.string.upstream_credits_summary),
                ) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Const.RIME_URL)))
                }
                addCategory(R.string.licenses_section_native) {
                    native.forEach { library -> addLibraryPreference(context, library) }
                }
                addCategory(R.string.licenses_section_libraries) {
                    others.forEach { library -> addLibraryPreference(context, library) }
                }
            }
        }
    }

    private fun PreferenceGroup.addLibraryPreference(
        context: android.content.Context,
        library: com.mikepenz.aboutlibraries.entity.Library,
    ) {
        addPreference(
            Preference(context).apply {
                isIconSpaceReserved = false
                title = "${library.uniqueId}:${library.artifactVersion}"
                summary = library.licenses.joinToString { l -> l.spdxId ?: l.name }
                setOnPreferenceClickListener { _ ->
                    showLicenseDialog(library.uniqueId, library.licenses)
                }
            },
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.disableTopOptionsMenu()
    }

    private fun showLicenseDialog(
        uniqueId: String,
        licenses: Set<License>,
    ): Boolean {
        when (licenses.size) {
            0 -> {}

            1 -> showLicenseContent(licenses.first())

            else -> {
                val licenseArray = licenses.toTypedArray()
                val licenseNames = licenseArray.map { it.spdxId ?: it.name }.toTypedArray()
                AlertDialog
                    .Builder(requireContext())
                    .setTitle(uniqueId)
                    .setItems(licenseNames) { _, idx ->
                        showLicenseContent(licenseArray[idx])
                    }.setPositiveButton(android.R.string.cancel, null)
                    .show()
            }
        }
        return true
    }

    private fun showLicenseContent(license: License) {
        if (license.url?.isNotBlank() == true) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(license.url)))
        }
    }
}
