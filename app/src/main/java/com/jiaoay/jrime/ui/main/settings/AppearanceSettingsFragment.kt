/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.main.settings

import com.jiaoay.jrime.data.prefs.AppPrefs
import com.jiaoay.jrime.data.prefs.PreferenceDelegateFragment

/**
 * The appearance page: palette mode, accent preset, key hints and the two
 * candidate typography knobs. The first slice of the settings-v2 IA — the
 * Compose rewrite replaces the Preference machinery later.
 */
class AppearanceSettingsFragment : PreferenceDelegateFragment(AppPrefs.defaultInstance().appearance)
