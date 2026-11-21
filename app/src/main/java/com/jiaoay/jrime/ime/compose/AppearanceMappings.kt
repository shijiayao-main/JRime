/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import com.jiaoay.jrime.data.prefs.AppPrefs
import com.jiaoay.jrime.engine.AccentPreset
import com.jiaoay.jrime.engine.PaletteMode

/** Maps the persisted appearance enums to the surface's engine enums. */
internal fun AppPrefs.PaletteModePref.toEnginePaletteMode(): PaletteMode = when (this) {
    AppPrefs.PaletteModePref.FOLLOW_SYSTEM -> PaletteMode.FollowSystem
    AppPrefs.PaletteModePref.LIGHT -> PaletteMode.ForceLight
    AppPrefs.PaletteModePref.DARK -> PaletteMode.ForceDark
}

internal fun AppPrefs.AccentPref.toEngineAccent(): AccentPreset = when (this) {
    AppPrefs.AccentPref.BLUE -> AccentPreset.Blue
    AppPrefs.AccentPref.GREEN -> AccentPreset.Green
    AppPrefs.AccentPref.PURPLE -> AccentPreset.Purple
}
