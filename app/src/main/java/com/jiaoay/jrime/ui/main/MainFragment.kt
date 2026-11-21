/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.main

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceGroup
import com.jiaoay.jrime.R
import com.jiaoay.jrime.ui.common.PaddingPreferenceFragment
import com.jiaoay.jrime.util.addCategory
import com.jiaoay.jrime.util.addPreference
import com.jiaoay.jrime.util.navigateWithAnim

class MainFragment : PaddingPreferenceFragment() {
    private val viewModel: MainViewModel by activityViewModels()

    override fun onStart() {
        super.onStart()
        viewModel.enableTopOptionsMenu()
    }

    override fun onStop() {
        viewModel.disableTopOptionsMenu()
        super.onStop()
    }

    private fun PreferenceGroup.addDestinationPreference(
        @StringRes title: Int,
        @DrawableRes icon: Int,
        route: NavigationRoute,
    ) {
        addPreference(title, icon = icon) {
            findNavController().navigateWithAnim(route)
        }
    }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext()).apply {
            addDestinationPreference(
                R.string.user_dictionary,
                R.drawable.ic_baseline_book_24,
                NavigationRoute.UserDict,
            )
            addDestinationPreference(
                R.string.profile,
                R.drawable.ic_baseline_snippet_folder_24,
                NavigationRoute.Profile,
            )
            addCategory("") {
                isIconSpaceReserved = false
                addDestinationPreference(
                    R.string.settings,
                    R.drawable.ic_baseline_tune_24,
                    NavigationRoute.Settings,
                )
            }
        }
    }
}
