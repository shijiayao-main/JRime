/*
 * SPDX-FileCopyrightText: 2015 - 2024 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.main.settings

import com.jiaoay.jrime.data.prefs.AppPrefs
import com.jiaoay.jrime.data.prefs.PreferenceDelegateFragment

class GeneralSettingsFragment : PreferenceDelegateFragment(AppPrefs.defaultInstance().general)
